package com.workforce.attendance.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.AttendanceCheckInDTO;
import com.workforce.attendance.dto.AttendanceCheckOutDTO;
import com.workforce.attendance.dto.AttendanceResponseDTO;

import com.workforce.attendance.entity.AttendanceEvent;
import com.workforce.attendance.entity.AttendanceEventType;
import com.workforce.attendance.entity.AttendanceSession;
import com.workforce.attendance.entity.AttendanceStatus;
import com.workforce.attendance.entity.AnomalySeverity;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.EmployeeRoster;

import com.workforce.attendance.exception.ResourceNotFoundException;

import com.workforce.attendance.repository.AttendanceEventRepository;
import com.workforce.attendance.repository.AttendanceSessionRepository;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.EmployeeRosterRepository;

import com.workforce.attendance.service.AttendanceService;
import com.workforce.attendance.service.GeofenceService;
import com.workforce.attendance.util.ShiftTimeCalculator;
import com.workforce.attendance.service.AnomalyFlagService;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeRosterRepository employeeRosterRepository;
    private final GeofenceService geofenceService;
    private final AnomalyFlagService anomalyFlagService;

    private static final ZoneId APP_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final double MAX_LOCATION_ACCURACY_METERS = 50.0;
    private static final double UNUSUAL_DISTANCE_METERS = 10000.0;
    private static final long LOCATION_TIME_WINDOW_SECONDS = 300;

    public AttendanceServiceImpl(
            AttendanceSessionRepository attendanceSessionRepository,
            AttendanceEventRepository attendanceEventRepository,
            EmployeeRepository employeeRepository,
            EmployeeRosterRepository employeeRosterRepository,
            GeofenceService geofenceService,
            AnomalyFlagService anomalyFlagService) {

        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceEventRepository = attendanceEventRepository;
        this.employeeRepository = employeeRepository;
        this.employeeRosterRepository = employeeRosterRepository;
        this.geofenceService = geofenceService;
        this.anomalyFlagService = anomalyFlagService;
    }

    @Override
    @Transactional
    public AttendanceResponseDTO checkIn(AttendanceCheckInDTO dto) {

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + dto.getEmployeeId()));

        if (!employee.isActive()) {
            throw new IllegalArgumentException(
                    "Inactive employees cannot check in");
        }

        validateAttendanceLocation(
                employee,
                AttendanceEventType.CHECK_IN,
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getLocationAccuracyMeters());

        Double accuracy = dto.getLocationAccuracyMeters();

        boolean alreadyCheckedIn =
                attendanceSessionRepository.existsByEmployeeAndStatus(
                        employee, AttendanceStatus.CHECKED_IN);

        boolean currentlyOnBreak =
                attendanceSessionRepository.existsByEmployeeAndStatus(
                        employee, AttendanceStatus.ON_BREAK);

        if (alreadyCheckedIn || currentlyOnBreak) {

            anomalyFlagService.recordLocationAnomaly(
                    employee,
                    AttendanceEventType.CHECK_IN,
                    dto.getLatitude(),
                    dto.getLongitude(),
                    accuracy,
                    "DUPLICATE_CHECK_IN",
                    AnomalySeverity.MEDIUM);

            throw new IllegalArgumentException(
                    "Employee is already checked in");
        }

        LocalDate today = LocalDate.now(APP_ZONE);

        Optional<EmployeeRoster> todayRoster =
                employeeRosterRepository.findByEmployeeAndRosterDate(
                        employee, today);

        AttendanceSession session = new AttendanceSession();
        session.setEmployee(employee);
        session.setRoster(todayRoster.orElse(null));
        session.setCheckInTime(Instant.now());
        session.setStatus(AttendanceStatus.CHECKED_IN);

        AttendanceSession savedSession =
                attendanceSessionRepository.save(session);

        AttendanceEvent event = createAttendanceEvent(
                savedSession,
                AttendanceEventType.CHECK_IN,
                dto.getLatitude(),
                dto.getLongitude(),
                accuracy);

        AttendanceEvent savedEvent = attendanceEventRepository.save(event);

        detectUnusualLocationChange(employee, savedEvent);

        return convertToDTO(savedSession);
    }

    @Override
    @Transactional
    public AttendanceResponseDTO checkOut(AttendanceCheckOutDTO dto) {

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + dto.getEmployeeId()));

        if (!employee.isActive()) {
            throw new IllegalArgumentException(
                    "Inactive employees cannot check out");
        }

        Optional<AttendanceSession> activeSession =
                attendanceSessionRepository
                        .findByEmployeeOrderByCheckInTimeDesc(employee)
                        .stream()
                        .filter(s -> s.getStatus() == AttendanceStatus.CHECKED_IN)
                        .findFirst();

        if (activeSession.isEmpty()) {

            anomalyFlagService.recordLocationAnomaly(
                    employee,
                    AttendanceEventType.CHECK_OUT,
                    dto.getLatitude(),
                    dto.getLongitude(),
                    dto.getLocationAccuracyMeters(),
                    "CHECKOUT_WITHOUT_CHECKIN",
                    AnomalySeverity.MEDIUM);

            throw new IllegalArgumentException(
                    "Employee has no active attendance session");
        }

        AttendanceSession session = activeSession.get();

        validateAttendanceLocation(
                employee,
                AttendanceEventType.CHECK_OUT,
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getLocationAccuracyMeters());

        Double accuracy = dto.getLocationAccuracyMeters();

        /*
         * D1.4: Validate checkout time against the scheduled shift start.
         *
         * The roster date is used to calculate the shift start.
         * This also works for overnight shifts because their start
         * time belongs to the original roster date.
         *
         * Validation happens before modifying or saving the session.
         */
        Instant checkoutTime = Instant.now();

        if (session.getRoster() != null
                && session.getRoster().getShift() != null) {

            Instant scheduledStart = ShiftTimeCalculator
                    .getShiftStartDateTime(
                            session.getRoster().getRosterDate(),
                            session.getRoster().getShift())
                    .atZone(APP_ZONE)
                    .toInstant();

            if (checkoutTime.isBefore(scheduledStart)) {
                throw new IllegalArgumentException(
                        "Checkout cannot occur before the scheduled shift starts");
            }
        }

        session.setCheckOutTime(checkoutTime);
        session.setStatus(AttendanceStatus.CHECKED_OUT);

        AttendanceSession savedSession =
                attendanceSessionRepository.save(session);

        AttendanceEvent event = createAttendanceEvent(
                savedSession,
                AttendanceEventType.CHECK_OUT,
                dto.getLatitude(),
                dto.getLongitude(),
                accuracy);

        AttendanceEvent savedEvent = attendanceEventRepository.save(event);

        detectUnusualLocationChange(employee, savedEvent);

        return convertToDTO(savedSession);
    }

    /*
     * Validates GPS for both employee types.
     *
     * OFFICE:
     * Requires an assigned office and valid office geofence.
     *
     * FIELD_WORK:
     * Does not require an assigned office or office geofence.
     * GPS coordinates and accuracy are still mandatory.
     */
    private void validateAttendanceLocation(
            Employee employee,
            AttendanceEventType eventType,
            Double latitude,
            Double longitude,
            Double accuracy) {

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException(
                    "Latitude and longitude are required");
        }

        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude)
                || longitude < -180 || longitude > 180) {

            throw new IllegalArgumentException(
                    "Invalid GPS coordinates");
        }

        if (accuracy != null && (!Double.isFinite(accuracy) || accuracy < 0)) {
            throw new IllegalArgumentException(
                    "Invalid GPS accuracy value");
        }

        if (accuracy != null && accuracy > MAX_LOCATION_ACCURACY_METERS) {

            anomalyFlagService.recordLocationAnomaly(
                    employee,
                    eventType,
                    latitude,
                    longitude,
                    accuracy,
                    "POOR_LOCATION_ACCURACY",
                    AnomalySeverity.MEDIUM);

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
            // Field employees can record attendance from their current
            // work location. Their GPS coordinates are still recorded.
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
                latitude,
                longitude,
                employee.getAssignedOffice());

        if (!withinOffice) {

            anomalyFlagService.recordLocationAnomaly(
                    employee,
                    eventType,
                    latitude,
                    longitude,
                    accuracy,
                    "OUTSIDE_GEOFENCE",
                    AnomalySeverity.HIGH);

            throw new IllegalArgumentException(
                    "Attendance denied: You are outside the permitted office radius");
        }
    }

    private AttendanceEvent createAttendanceEvent(
            AttendanceSession session,
            AttendanceEventType eventType,
            Double latitude,
            Double longitude,
            Double accuracy) {

        AttendanceEvent event = new AttendanceEvent();

        event.setAttendanceSession(session);
        event.setEventType(eventType);
        event.setServerReceivedAt(Instant.now());
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        event.setLocationAccuracyMeters(accuracy);

        return event;
    }

    /*
     * Flags unusual movement only when both GPS readings are reliable,
     * distance exceeds 10 km, and the readings are within five minutes.
     * This is a review indicator, not proof of fraud.
     */
    private void detectUnusualLocationChange(
            Employee employee,
            AttendanceEvent currentEvent) {

        if (!isReliableLocation(currentEvent)) {
            return;
        }

        Optional<AttendanceEvent> previousEvent =
                attendanceEventRepository
                        .findFirstByAttendanceSession_Employee_IdAndIdNotAndLatitudeIsNotNullAndLongitudeIsNotNullAndLocationAccuracyMetersLessThanEqualOrderByServerReceivedAtDesc(
                                employee.getId(),
                                currentEvent.getId(),
                                MAX_LOCATION_ACCURACY_METERS);

        if (previousEvent.isEmpty()) {
            return;
        }

        AttendanceEvent previous = previousEvent.get();

        if (!isReliableLocation(previous)) {
            return;
        }

        long elapsedSeconds = Duration.between(
                previous.getServerReceivedAt(),
                currentEvent.getServerReceivedAt()).getSeconds();

        if (elapsedSeconds <= 0
                || elapsedSeconds > LOCATION_TIME_WINDOW_SECONDS) {
            return;
        }

        double distanceMeters = calculateDistanceMeters(
                previous.getLatitude(),
                previous.getLongitude(),
                currentEvent.getLatitude(),
                currentEvent.getLongitude());

        if (distanceMeters > UNUSUAL_DISTANCE_METERS) {

            anomalyFlagService.recordLocationAnomaly(
                    employee,
                    currentEvent.getEventType(),
                    currentEvent.getLatitude(),
                    currentEvent.getLongitude(),
                    currentEvent.getLocationAccuracyMeters(),
                    "UNUSUAL_LOCATION_CHANGE",
                    AnomalySeverity.MEDIUM);
        }
    }

    private boolean isReliableLocation(AttendanceEvent event) {

        Double accuracy = event.getLocationAccuracyMeters();

        return event.getLatitude() != null
                && event.getLongitude() != null
                && accuracy != null
                && Double.isFinite(accuracy)
                && accuracy >= 0
                && accuracy <= MAX_LOCATION_ACCURACY_METERS;
    }

    private double calculateDistanceMeters(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2) {

        final double earthRadiusMeters = 6371000.0;

        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);

        double deltaLatitude = Math.toRadians(latitude2 - latitude1);
        double deltaLongitude = Math.toRadians(longitude2 - longitude1);

        double a = Math.sin(deltaLatitude / 2)
                * Math.sin(deltaLatitude / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLongitude / 2)
                * Math.sin(deltaLongitude / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));

        return earthRadiusMeters * c;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getAttendanceHistory(Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + employeeId));

        return attendanceSessionRepository
                .findByEmployeeOrderByCheckInTimeDesc(employee)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    private AttendanceResponseDTO convertToDTO(
            AttendanceSession session) {

        AttendanceResponseDTO response = new AttendanceResponseDTO(
                session.getId(),
                session.getEmployee().getId(),
                session.getCheckInTime(),
                session.getCheckOutTime(),
                session.getStatus()
        );

        // Calculate shift-based attendance only when
        // the attendance session has an assigned roster and shift.

        if (session.getRoster() != null
                && session.getRoster().getShift() != null) {

            var roster = session.getRoster();

            var shift = roster.getShift();

            long lateMinutes =
                    ShiftTimeCalculator.calculateLateArrivalMinutes(
                            roster.getRosterDate(),
                            shift,
                            session.getCheckInTime(),
                            APP_ZONE
                    );

            response.setLateArrivalMinutes(lateMinutes);

            // Calculate early departure only after checkout.
            // Skip invalid legacy checkout timestamps so that
            // attendance history retrieval does not fail.

            if (session.getCheckOutTime() != null) {

                Instant scheduledStart = ShiftTimeCalculator
                        .getShiftStartDateTime(
                                roster.getRosterDate(),
                                shift)
                        .atZone(APP_ZONE)
                        .toInstant();

                if (!session.getCheckOutTime().isBefore(scheduledStart)) {

                    long earlyMinutes =
                            ShiftTimeCalculator.calculateEarlyDepartureMinutes(
                                    roster.getRosterDate(),
                                    shift,
                                    session.getCheckOutTime(),
                                    APP_ZONE
                            );

                    response.setEarlyDepartureMinutes(earlyMinutes);
                }
            }
        }

        return response;
    }
}