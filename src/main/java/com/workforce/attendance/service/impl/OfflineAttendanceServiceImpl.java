
package com.workforce.attendance.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.OfflineAttendanceDTO;
import com.workforce.attendance.entity.*;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.*;
import com.workforce.attendance.service.GeofenceService;
import com.workforce.attendance.service.OfflineAttendanceService;

@Service
public class OfflineAttendanceServiceImpl
        implements OfflineAttendanceService {

    private static final Duration MAX_OFFLINE_DELAY =
            Duration.ofMinutes(5);

    private static final double MAX_LOCATION_ACCURACY_METERS = 50.0;

    private final EmployeeRepository employeeRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final BreakPolicyRepository breakPolicyRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final GeofenceService geofenceService;

    public OfflineAttendanceServiceImpl(
            EmployeeRepository employeeRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            AttendanceEventRepository attendanceEventRepository,
            BreakRecordRepository breakRecordRepository,
            BreakPolicyRepository breakPolicyRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            AnomalyFlagRepository anomalyFlagRepository,
            GeofenceService geofenceService) {

        this.employeeRepository = employeeRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceEventRepository = attendanceEventRepository;
        this.breakRecordRepository = breakRecordRepository;
        this.breakPolicyRepository = breakPolicyRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.geofenceService = geofenceService;
    }

    @Override
    @Transactional
    public OfflineAttendanceDTO processOfflineAttendance(
            OfflineAttendanceDTO dto,
            String idempotencyKey) {

        // 1. Validate the idempotency key.

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {

            throw new IllegalArgumentException(
                    "A valid idempotency key is required");
        }

        // 2. Validate required request data.

        if (dto == null
                || dto.getEmployeeId() == null
                || dto.getEventType() == null
                || dto.getClientReportedAt() == null) {

            throw new IllegalArgumentException(
                    "Employee ID, event type and client-reported time are required");
        }

        // 3. Find and lock the employee.

        Employee employee = employeeRepository
                .findByIdForUpdate(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + dto.getEmployeeId()));

        // 4. Generate request hash.

        String requestHash = generateRequestHash(dto);

        // 5. Check for an already submitted request.

        var existingRecord =
                idempotencyRecordRepository.findByIdempotencyKey(
                        idempotencyKey);

        if (existingRecord.isPresent()) {

            IdempotencyRecord previous = existingRecord.get();

            if (!previous.getEmployee().getId().equals(employee.getId())
                    || !previous.getRequestHash().equals(requestHash)) {

                throw new IllegalArgumentException(
                        "Idempotency key was already used with different request data");
            }

            // Return the original result without processing the event again.

            return convertToDTO(previous);
        }

        // 6. Capture authoritative server receipt time.

        Instant serverReceivedAt = Instant.now();

        // 7. Create idempotency record.

        IdempotencyRecord record = new IdempotencyRecord();

        record.setIdempotencyKey(idempotencyKey);
        record.setEmployee(employee);
        record.setCreatedAt(serverReceivedAt);
        record.setRequestHash(requestHash);
        record.setStatus(SubmissionStatus.PENDING);

        record = idempotencyRecordRepository.save(record);

        // 8. Prepare attendance event.

        AttendanceEvent event = new AttendanceEvent();

        event.setEventType(dto.getEventType());
        event.setServerReceivedAt(serverReceivedAt);
        event.setClientReportedAt(dto.getClientReportedAt());
        event.setLatitude(dto.getLatitude());
        event.setLongitude(dto.getLongitude());
        event.setLocationAccuracyMeters(
                dto.getLocationAccuracyMeters());

        // 9. Reject inactive employees before checking offline delay.
        // This prevents inactive employees from getting REQUIRES_REVIEW
        // instead of REJECTED for delayed or future-dated submissions.

        if (!employee.isActive()) {

            event = attendanceEventRepository.save(event);

            record.setAttendanceEvent(event);
            record.setStatus(SubmissionStatus.REJECTED);

            idempotencyRecordRepository.save(record);

            OfflineAttendanceDTO response = convertToDTO(record);

            response.setMessage(
                    "Inactive employees cannot submit attendance events");

            return response;
        }

        // 10. Check the offline delay policy.

        Duration delay = Duration.between(
                dto.getClientReportedAt(),
                serverReceivedAt);

        if (delay.isNegative()
                || delay.compareTo(MAX_OFFLINE_DELAY) > 0) {

            event = attendanceEventRepository.save(event);

            record.setAttendanceEvent(event);
            record.setStatus(SubmissionStatus.REQUIRES_REVIEW);

            String ruleIdentifier;
            AnomalySeverity severity;

            if (delay.isNegative()) {
                ruleIdentifier = "FUTURE_CLIENT_TIMESTAMP";
                severity = AnomalySeverity.HIGH;
            } else {
                ruleIdentifier = "DELAYED_OFFLINE_SUBMISSION";
                severity = AnomalySeverity.MEDIUM;
            }

            AnomalyFlag anomalyFlag = new AnomalyFlag();

            anomalyFlag.setEmployee(employee);
            anomalyFlag.setAttendanceEvent(event);
            anomalyFlag.setRuleIdentifier(ruleIdentifier);
            anomalyFlag.setDetectedAt(serverReceivedAt);
            anomalyFlag.setSeverity(severity);
            anomalyFlag.setReviewStatus(AnomalyReviewStatus.PENDING);

            anomalyFlagRepository.save(anomalyFlag);

            idempotencyRecordRepository.save(record);

            return convertToDTO(record);
        }

        // 11. Validate and process attendance event.

        String rejectionReason = null;

        try {

            validateLocation(employee, dto);

            AttendanceSession session = processEvent(
                    employee,
                    dto,
                    serverReceivedAt);

            event.setAttendanceSession(session);

            event = attendanceEventRepository.save(event);

            record.setAttendanceEvent(event);
            record.setStatus(SubmissionStatus.ACCEPTED);

        } catch (IllegalArgumentException ex) {

            rejectionReason = ex.getMessage();

            System.out.println(
                    "OFFLINE ATTENDANCE REJECTED: " + rejectionReason);

            // Preserve rejected events for traceability.

            event = attendanceEventRepository.save(event);

            record.setAttendanceEvent(event);
            record.setStatus(SubmissionStatus.REJECTED);

            // Record relevant rejected attempts as anomalies.
            // The anomaly references the rejected attendance event.

            recordRejectedAnomaly(
                    employee,
                    event,
                    dto,
                    rejectionReason,
                    serverReceivedAt);
        }

        // Preserve the original idempotency behaviour.

        idempotencyRecordRepository.save(record);

        OfflineAttendanceDTO response = convertToDTO(record);

        if (record.getStatus() == SubmissionStatus.REJECTED
                && rejectionReason != null) {

            response.setMessage(rejectionReason);
        }

        return response;
    }

    /*
     * Creates anomaly flags for relevant rejected attendance attempts.
     *
     * Ordinary validation failures and break-policy violations
     * are not automatically classified as suspicious anomalies.
     */
    private void recordRejectedAnomaly(
            Employee employee,
            AttendanceEvent event,
            OfflineAttendanceDTO dto,
            String rejectionReason,
            Instant detectedAt) {

        if (rejectionReason == null) {
            return;
        }

        String ruleIdentifier = null;
        AnomalySeverity severity = AnomalySeverity.MEDIUM;

        // Poor GPS accuracy.

        if (rejectionReason.contains("GPS accuracy is too low")) {

            ruleIdentifier = "POOR_LOCATION_ACCURACY";
        }

        // Employee is outside the permitted office geofence.

        else if (rejectionReason.contains(
                "outside the permitted office radius")) {

            ruleIdentifier = "OUTSIDE_GEOFENCE";
            severity = AnomalySeverity.HIGH;
        }

        // Duplicate check-in attempt.

        else if (rejectionReason.contains(
                "already has an active attendance session")
                && dto.getEventType() == AttendanceEventType.CHECK_IN) {

            ruleIdentifier = "DUPLICATE_CHECK_IN";
        }

        // Checkout without an active attendance session.

        else if (rejectionReason.contains(
                "No active attendance session found")) {

            ruleIdentifier = "CHECKOUT_WITHOUT_CHECKIN";
        }

        // Invalid attendance event sequence involving breaks.

        else if (rejectionReason.contains(
                "must be checked in before starting a break")
                || rejectionReason.contains(
                        "not currently on a break")
                || rejectionReason.contains(
                        "No active break record found")) {

            ruleIdentifier = "INVALID_ATTENDANCE_SEQUENCE";
        }

        // Do not create anomaly flags for unrelated validation failures
        // or ordinary break-policy violations.

        if (ruleIdentifier == null) {
            return;
        }

        AnomalyFlag anomalyFlag = new AnomalyFlag();

        anomalyFlag.setEmployee(employee);
        anomalyFlag.setAttendanceEvent(event);
        anomalyFlag.setRuleIdentifier(ruleIdentifier);
        anomalyFlag.setDetectedAt(detectedAt);
        anomalyFlag.setSeverity(severity);
        anomalyFlag.setReviewStatus(AnomalyReviewStatus.PENDING);

        anomalyFlagRepository.save(anomalyFlag);
    }

    /*
     * Validates GPS for both employee assignment types.
     *
     * OFFICE:
     * Requires an assigned office and valid office geofence.
     *
     * FIELD_WORK:
     * Does not require an assigned office or office geofence.
     * GPS coordinates and accuracy are still mandatory.
     */
    private void validateLocation(
            Employee employee,
            OfflineAttendanceDTO dto) {

        if (dto.getLatitude() == null
                || dto.getLongitude() == null) {

            throw new IllegalArgumentException(
                    "Latitude and longitude are required");
        }

        double latitude = dto.getLatitude();
        double longitude = dto.getLongitude();

        if (!Double.isFinite(latitude)
                || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude)
                || longitude < -180 || longitude > 180) {

            throw new IllegalArgumentException(
                    "Invalid GPS coordinates");
        }

        Double accuracy = dto.getLocationAccuracyMeters();

        if (accuracy != null
                && (!Double.isFinite(accuracy) || accuracy < 0)) {

            throw new IllegalArgumentException(
                    "Invalid GPS accuracy value");
        }

        if (accuracy != null
                && accuracy > MAX_LOCATION_ACCURACY_METERS) {

            throw new IllegalArgumentException(
                    "GPS accuracy is too low. Please try again in a location "
                    + "with a clearer GPS signal.");
        }

        geofenceService.validateLocationAccuracy(accuracy);

        String assignmentType = employee.getAssignmentType();

        if (assignmentType == null || assignmentType.isBlank()) {
            assignmentType = "OFFICE";
        }

        if ("FIELD_WORK".equalsIgnoreCase(assignmentType)) {

            // Field employees can submit attendance from their
            // current work location. GPS is still validated and recorded.

            return;
        }

        if (!"OFFICE".equalsIgnoreCase(assignmentType)) {

            throw new IllegalArgumentException(
                    "Invalid employee assignment type");
        }

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

    private AttendanceSession processEvent(
            Employee employee,
            OfflineAttendanceDTO dto,
            Instant serverTime) {

        AttendanceEventType eventType = dto.getEventType();

        switch (eventType) {

            case CHECK_IN:

                if (attendanceSessionRepository
                        .existsByEmployeeAndStatus(
                                employee,
                                AttendanceStatus.CHECKED_IN)
                        || attendanceSessionRepository
                        .existsByEmployeeAndStatus(
                                employee,
                                AttendanceStatus.ON_BREAK)) {

                    throw new IllegalArgumentException(
                            "Employee already has an active attendance session");
                }

                AttendanceSession newSession = new AttendanceSession();

                newSession.setEmployee(employee);
                newSession.setCheckInTime(serverTime);
                newSession.setStatus(AttendanceStatus.CHECKED_IN);

                return attendanceSessionRepository.save(newSession);

            case CHECK_OUT:

                AttendanceSession checkoutSession =
                        attendanceSessionRepository
                                .findByEmployeeAndStatus(
                                        employee,
                                        AttendanceStatus.CHECKED_IN)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "No active attendance session found"));

                checkoutSession.setCheckOutTime(serverTime);
                checkoutSession.setStatus(AttendanceStatus.CHECKED_OUT);

                return attendanceSessionRepository.save(checkoutSession);

            case BREAK_START:

                AttendanceSession breakStartSession =
                        attendanceSessionRepository
                                .findByEmployeeAndStatus(
                                        employee,
                                        AttendanceStatus.CHECKED_IN)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Employee must be checked in before starting a break"));

                if (breakRecordRepository
                        .existsByAttendanceSessionAndEndTimeIsNull(
                                breakStartSession)) {

                    throw new IllegalArgumentException(
                            "Employee already has an active break");
                }

                BreakPolicy startPolicy = getActivePolicy();

                List<BreakRecord> previousBreaks =
                        breakRecordRepository
                                .findByAttendanceSessionOrderByStartTimeAsc(
                                        breakStartSession);

                if (previousBreaks.size()
                        >= startPolicy.getMaxBreaksPerShift()) {

                    throw new IllegalArgumentException(
                            "Maximum number of breaks exceeded");
                }

                long previousBreakSeconds =
                        calculateBreakSeconds(previousBreaks);

                if (previousBreakSeconds
                        >= startPolicy.getMaxTotalBreakMinutes() * 60L) {

                    throw new IllegalArgumentException(
                            "Maximum total break duration exceeded");
                }

                BreakRecord newBreak = new BreakRecord();

                newBreak.setAttendanceSession(breakStartSession);
                newBreak.setStartTime(serverTime);

                breakRecordRepository.save(newBreak);

                breakStartSession.setStatus(AttendanceStatus.ON_BREAK);

                return attendanceSessionRepository.save(
                        breakStartSession);

            case BREAK_END:

                AttendanceSession breakEndSession =
                        attendanceSessionRepository
                                .findByEmployeeAndStatus(
                                        employee,
                                        AttendanceStatus.ON_BREAK)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Employee is not currently on a break"));

                BreakRecord activeBreak =
                        breakRecordRepository
                                .findByAttendanceSessionAndEndTimeIsNull(
                                        breakEndSession)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "No active break record found"));

                BreakPolicy endPolicy = getActivePolicy();

                Duration currentBreakDuration = Duration.between(
                        activeBreak.getStartTime(),
                        serverTime);

                if (currentBreakDuration.isNegative()) {

                    throw new IllegalArgumentException(
                            "Invalid break end time");
                }

                if (currentBreakDuration.compareTo(
                        Duration.ofMinutes(
                                endPolicy.getMaxDurationPerBreakMinutes())) > 0) {

                    throw new IllegalArgumentException(
                            "Maximum duration for this break exceeded");
                }

                List<BreakRecord> allBreaks =
                        breakRecordRepository
                                .findByAttendanceSessionOrderByStartTimeAsc(
                                        breakEndSession);

                long completedBreakSeconds =
                        calculateBreakSeconds(allBreaks);

                long currentBreakSeconds =
                        currentBreakDuration.getSeconds();

                if ((completedBreakSeconds + currentBreakSeconds)
                        > endPolicy.getMaxTotalBreakMinutes() * 60L) {

                    throw new IllegalArgumentException(
                            "Maximum total break duration exceeded");
                }

                activeBreak.setEndTime(serverTime);

                breakRecordRepository.save(activeBreak);

                breakEndSession.setStatus(AttendanceStatus.CHECKED_IN);

                return attendanceSessionRepository.save(
                        breakEndSession);

            default:

                throw new IllegalArgumentException(
                        "Unsupported attendance event type");
        }
    }

    private BreakPolicy getActivePolicy() {

        return breakPolicyRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active break policy configured"));
    }

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
