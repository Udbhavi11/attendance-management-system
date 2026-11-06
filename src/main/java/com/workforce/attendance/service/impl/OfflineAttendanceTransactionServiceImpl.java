package com.workforce.attendance.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.OfflineAttendanceDTO;
import com.workforce.attendance.entity.*;
import com.workforce.attendance.exception.IdempotencyConflictException;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.*;
import com.workforce.attendance.service.GeofenceService;
import com.workforce.attendance.service.OfflineAttendanceTransactionService;

@Service
public class OfflineAttendanceTransactionServiceImpl
        implements OfflineAttendanceTransactionService {

    private static final Duration MAX_OFFLINE_DELAY =
            Duration.ofMinutes(5);

    private final EmployeeRepository employeeRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final BreakPolicyRepository breakPolicyRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final GeofenceService geofenceService;

    public OfflineAttendanceTransactionServiceImpl(
            EmployeeRepository employeeRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            AttendanceEventRepository attendanceEventRepository,
            BreakRecordRepository breakRecordRepository,
            BreakPolicyRepository breakPolicyRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            GeofenceService geofenceService) {

        this.employeeRepository = employeeRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceEventRepository = attendanceEventRepository;
        this.breakRecordRepository = breakRecordRepository;
        this.breakPolicyRepository = breakPolicyRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.geofenceService = geofenceService;
    }

    // 1. CHECK-IN

    @Override
    @Transactional
    public OfflineAttendanceDTO processCheckIn(
            OfflineAttendanceDTO dto,
            String idempotencyKey) {

        return processAttendance(
                dto, idempotencyKey, AttendanceEventType.CHECK_IN);
    }

    // 2. CHECK-OUT

    @Override
    @Transactional
    public OfflineAttendanceDTO processCheckOut(
            OfflineAttendanceDTO dto,
            String idempotencyKey) {

        return processAttendance(
                dto, idempotencyKey, AttendanceEventType.CHECK_OUT);
    }

    // 3. BREAK START

    @Override
    @Transactional
    public OfflineAttendanceDTO processBreakStart(
            OfflineAttendanceDTO dto,
            String idempotencyKey) {

        return processAttendance(
                dto, idempotencyKey, AttendanceEventType.BREAK_START);
    }

    // 4. BREAK END

    @Override
    @Transactional
    public OfflineAttendanceDTO processBreakEnd(
            OfflineAttendanceDTO dto,
            String idempotencyKey) {

        return processAttendance(
                dto, idempotencyKey, AttendanceEventType.BREAK_END);
    }

    // COMMON TRANSACTION PROCESSING

    private OfflineAttendanceDTO processAttendance(
            OfflineAttendanceDTO dto,
            String idempotencyKey,
            AttendanceEventType expectedEventType) {

        validateRequest(dto, idempotencyKey, expectedEventType);

        // Lock employee row to prevent concurrent attendance changes.
        Employee employee = employeeRepository
                .findByIdForUpdate(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + dto.getEmployeeId()));

        String requestHash = generateRequestHash(dto);

        // Check whether this request was already processed.
        Optional<IdempotencyRecord> existingRecord =
                idempotencyRecordRepository.findByIdempotencyKey(
                        idempotencyKey);

        if (existingRecord.isPresent()) {

            IdempotencyRecord previous = existingRecord.get();

            if (!previous.getEmployee().getId().equals(employee.getId())
                    || !previous.getRequestHash().equals(requestHash)) {

                throw new IdempotencyConflictException(
                        "Idempotency key was already used with different request data");
            }

            return convertToDTO(previous);
        }

        Instant serverReceivedAt = Instant.now();

        IdempotencyRecord record =
                createIdempotencyRecord(
                        employee, idempotencyKey,
                        requestHash, serverReceivedAt);

        AttendanceEvent event =
                createAttendanceEvent(dto, serverReceivedAt);

        // Check whether the request arrived within the allowed window.
        if (!isWithinOfflineWindow(dto, serverReceivedAt)) {

            event = attendanceEventRepository.save(event);

            record.setAttendanceEvent(event);
            record.setStatus(SubmissionStatus.REQUIRES_REVIEW);

            idempotencyRecordRepository.save(record);

            return convertToDTO(record);
        }

        try {

            if (!employee.isActive()) {
                throw new IllegalArgumentException(
                        "Inactive employees cannot submit attendance events");
            }

            validateLocation(employee, dto);

            AttendanceSession session = processEvent(
                    employee, dto, serverReceivedAt, expectedEventType);

            event.setAttendanceSession(session);

            event = attendanceEventRepository.save(event);

            record.setAttendanceEvent(event);
            record.setStatus(SubmissionStatus.ACCEPTED);

        } catch (IllegalArgumentException ex) {

            // Preserve rejected event without creating a successful
            // attendance operation.

            event = attendanceEventRepository.save(event);

            record.setAttendanceEvent(event);
            record.setStatus(SubmissionStatus.REJECTED);

            System.out.println(
                    "OFFLINE ATTENDANCE REJECTED: " + ex.getMessage());
        }

        idempotencyRecordRepository.save(record);

        return convertToDTO(record);
    }

    // REQUEST VALIDATION

    private void validateRequest(
            OfflineAttendanceDTO dto,
            String idempotencyKey,
            AttendanceEventType expectedEventType) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {

            throw new IllegalArgumentException(
                    "A valid idempotency key is required");
        }

        if (dto == null
                || dto.getEmployeeId() == null
                || dto.getEventType() == null
                || dto.getClientReportedAt() == null) {

            throw new IllegalArgumentException(
                    "Employee ID, event type and client-reported time are required");
        }

        if (dto.getEventType() != expectedEventType) {

            throw new IllegalArgumentException(
                    "Event type does not match the requested operation");
        }
    }

    // CREATE IDEMPOTENCY RECORD

    private IdempotencyRecord createIdempotencyRecord(
            Employee employee,
            String idempotencyKey,
            String requestHash,
            Instant serverReceivedAt) {

        IdempotencyRecord record = new IdempotencyRecord();

        record.setIdempotencyKey(idempotencyKey);
        record.setEmployee(employee);
        record.setCreatedAt(serverReceivedAt);
        record.setRequestHash(requestHash);
        record.setStatus(SubmissionStatus.PENDING);

        return idempotencyRecordRepository.save(record);
    }

    // CREATE ATTENDANCE EVENT

    private AttendanceEvent createAttendanceEvent(
            OfflineAttendanceDTO dto,
            Instant serverReceivedAt) {

        AttendanceEvent event = new AttendanceEvent();

        event.setEventType(dto.getEventType());
        event.setServerReceivedAt(serverReceivedAt);
        event.setClientReportedAt(dto.getClientReportedAt());
        event.setLatitude(dto.getLatitude());
        event.setLongitude(dto.getLongitude());
        event.setLocationAccuracyMeters(
                dto.getLocationAccuracyMeters());

        return event;
    }

    // OFFLINE TIME VALIDATION

    private boolean isWithinOfflineWindow(
            OfflineAttendanceDTO dto,
            Instant serverReceivedAt) {

        Duration delay = Duration.between(
                dto.getClientReportedAt(),
                serverReceivedAt);

        return !delay.isNegative()
                && delay.compareTo(MAX_OFFLINE_DELAY) <= 0;
    }

    // LOCATION VALIDATION

    private void validateLocation(
            Employee employee,
            OfflineAttendanceDTO dto) {

        if (dto.getLatitude() == null
                || dto.getLongitude() == null) {

            throw new IllegalArgumentException(
                    "Latitude and longitude are required");
        }

        geofenceService.validateLocationAccuracy(
                dto.getLocationAccuracyMeters());

        if (employee.getAssignedOffice() == null) {

            throw new IllegalArgumentException(
                    "Employee has no assigned office");
        }

        boolean withinOffice = geofenceService.isWithinOffice(
                dto.getLatitude(),
                dto.getLongitude(),
                employee.getAssignedOffice());

        if (!withinOffice) {

            throw new IllegalArgumentException(
                    "Employee is outside the permitted office radius");
        }
    }

    // EVENT DISPATCHER

    private AttendanceSession processEvent(
            Employee employee,
            OfflineAttendanceDTO dto,
            Instant serverTime,
            AttendanceEventType eventType) {

        switch (eventType) {

            case CHECK_IN:
                return processCheckInEvent(employee, serverTime);

            case CHECK_OUT:
                return processCheckOutEvent(employee, serverTime);

            case BREAK_START:
                return processBreakStartEvent(employee, serverTime);

            case BREAK_END:
                return processBreakEndEvent(employee, serverTime);

            default:
                throw new IllegalArgumentException(
                        "Unsupported attendance event type");
        }
    }

    // CHECK-IN BUSINESS LOGIC

    private AttendanceSession processCheckInEvent(
            Employee employee,
            Instant serverTime) {

        if (attendanceSessionRepository
                .existsByEmployeeAndStatus(
                        employee, AttendanceStatus.CHECKED_IN)
                || attendanceSessionRepository
                .existsByEmployeeAndStatus(
                        employee, AttendanceStatus.ON_BREAK)) {

            throw new IllegalArgumentException(
                    "Employee already has an active attendance session");
        }

        AttendanceSession session = new AttendanceSession();

        session.setEmployee(employee);
        session.setCheckInTime(serverTime);
        session.setStatus(AttendanceStatus.CHECKED_IN);

        return attendanceSessionRepository.save(session);
    }

    // CHECK-OUT BUSINESS LOGIC

    private AttendanceSession processCheckOutEvent(
            Employee employee,
            Instant serverTime) {

        AttendanceSession session =
                attendanceSessionRepository
                        .findByEmployeeAndStatus(
                                employee, AttendanceStatus.CHECKED_IN)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No active attendance session found"));

        session.setCheckOutTime(serverTime);
        session.setStatus(AttendanceStatus.CHECKED_OUT);

        return attendanceSessionRepository.save(session);
    }

    // BREAK START BUSINESS LOGIC

    private AttendanceSession processBreakStartEvent(
            Employee employee,
            Instant serverTime) {

        AttendanceSession session =
                attendanceSessionRepository
                        .findByEmployeeAndStatus(
                                employee, AttendanceStatus.CHECKED_IN)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Employee must be checked in before starting a break"));

        if (breakRecordRepository
                .existsByAttendanceSessionAndEndTimeIsNull(session)) {

            throw new IllegalArgumentException(
                    "Employee already has an active break");
        }

        BreakPolicy policy = getActivePolicy();

        List<BreakRecord> previousBreaks =
                breakRecordRepository
                        .findByAttendanceSessionOrderByStartTimeAsc(session);

        if (previousBreaks.size() >= policy.getMaxBreaksPerShift()) {

            throw new IllegalArgumentException(
                    "Maximum number of breaks exceeded");
        }

        long previousBreakSeconds =
                calculateBreakSeconds(previousBreaks);

        if (previousBreakSeconds
                >= policy.getMaxTotalBreakMinutes() * 60L) {

            throw new IllegalArgumentException(
                    "Maximum total break duration exceeded");
        }

        BreakRecord newBreak = new BreakRecord();

        newBreak.setAttendanceSession(session);
        newBreak.setStartTime(serverTime);

        breakRecordRepository.save(newBreak);

        session.setStatus(AttendanceStatus.ON_BREAK);

        return attendanceSessionRepository.save(session);
    }

    // BREAK END BUSINESS LOGIC

    private AttendanceSession processBreakEndEvent(
            Employee employee,
            Instant serverTime) {

        AttendanceSession session =
                attendanceSessionRepository
                        .findByEmployeeAndStatus(
                                employee, AttendanceStatus.ON_BREAK)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Employee is not currently on a break"));

        BreakRecord activeBreak =
                breakRecordRepository
                        .findByAttendanceSessionAndEndTimeIsNull(session)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No active break record found"));

        BreakPolicy policy = getActivePolicy();

        Duration currentBreakDuration = Duration.between(
                activeBreak.getStartTime(), serverTime);

        if (currentBreakDuration.isNegative()) {

            throw new IllegalArgumentException(
                    "Invalid break end time");
        }

        if (currentBreakDuration.compareTo(
                Duration.ofMinutes(
                        policy.getMaxDurationPerBreakMinutes())) > 0) {

            throw new IllegalArgumentException(
                    "Maximum duration for this break exceeded");
        }

        List<BreakRecord> allBreaks =
                breakRecordRepository
                        .findByAttendanceSessionOrderByStartTimeAsc(session);

        long completedBreakSeconds =
                calculateBreakSeconds(allBreaks);

        long currentBreakSeconds = currentBreakDuration.getSeconds();

        if ((completedBreakSeconds + currentBreakSeconds)
                > policy.getMaxTotalBreakMinutes() * 60L) {

            throw new IllegalArgumentException(
                    "Maximum total break duration exceeded");
        }

        activeBreak.setEndTime(serverTime);

        breakRecordRepository.save(activeBreak);

        session.setStatus(AttendanceStatus.CHECKED_IN);

        return attendanceSessionRepository.save(session);
    }

    // GET ACTIVE BREAK POLICY

    private BreakPolicy getActivePolicy() {

        return breakPolicyRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active break policy configured"));
    }

    // CALCULATE COMPLETED BREAK DURATION

    private long calculateBreakSeconds(List<BreakRecord> breaks) {

        long totalSeconds = 0;

        for (BreakRecord breakRecord : breaks) {

            if (breakRecord.getEndTime() != null) {

                totalSeconds += Duration.between(
                        breakRecord.getStartTime(),
                        breakRecord.getEndTime()).getSeconds();
            }
        }

        return totalSeconds;
    }

    // GENERATE REQUEST HASH

    private String generateRequestHash(OfflineAttendanceDTO dto) {

        try {

            String requestData =
                    dto.getEmployeeId() + "|" +
                    dto.getEventType() + "|" +
                    dto.getClientReportedAt() + "|" +
                    dto.getLatitude() + "|" +
                    dto.getLongitude() + "|" +
                    dto.getLocationAccuracyMeters();

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    requestData.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "Unable to generate request hash", ex);
        }
    }

    // CONVERT TO RESPONSE DTO

    private OfflineAttendanceDTO convertToDTO(
            IdempotencyRecord record) {

        OfflineAttendanceDTO response = new OfflineAttendanceDTO();

        response.setEmployeeId(record.getEmployee().getId());
        response.setStatus(record.getStatus());

        AttendanceEvent event = record.getAttendanceEvent();

        if (event != null) {

            response.setEventId(event.getId());
            response.setEventType(event.getEventType());
            response.setClientReportedAt(event.getClientReportedAt());
            response.setServerReceivedAt(event.getServerReceivedAt());
            response.setLatitude(event.getLatitude());
            response.setLongitude(event.getLongitude());

            response.setLocationAccuracyMeters(
                    event.getLocationAccuracyMeters());
        }

        switch (record.getStatus()) {

            case ACCEPTED:
                response.setMessage("Attendance event accepted");
                break;

            case REJECTED:
                response.setMessage(
                        "Attendance event rejected. Check server logs for the reason.");
                break;

            case REQUIRES_REVIEW:
                response.setMessage(
                        "Attendance event requires review because it arrived outside the permitted time window.");
                break;

            case PENDING:
                response.setMessage("Attendance event is pending");
                break;
        }

        return response;
    }
}