
package com.workforce.attendance.service.impl;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.BreakDTO;
import com.workforce.attendance.entity.AttendanceEvent;
import com.workforce.attendance.entity.AttendanceEventType;
import com.workforce.attendance.entity.AttendanceSession;
import com.workforce.attendance.entity.AttendanceStatus;
import com.workforce.attendance.entity.AnomalyFlag;
import com.workforce.attendance.entity.AnomalyReviewStatus;
import com.workforce.attendance.entity.AnomalySeverity;
import com.workforce.attendance.entity.BreakPolicy;
import com.workforce.attendance.entity.BreakRecord;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.AnomalyFlagRepository;
import com.workforce.attendance.repository.AttendanceEventRepository;
import com.workforce.attendance.repository.AttendanceSessionRepository;
import com.workforce.attendance.repository.BreakPolicyRepository;
import com.workforce.attendance.repository.BreakRecordRepository;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.service.AnomalyFlagService;
import com.workforce.attendance.service.BreakService;

@Service
public class BreakServiceImpl implements BreakService {

    private final BreakRecordRepository breakRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final BreakPolicyRepository breakPolicyRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final AnomalyFlagService anomalyFlagService;

    public BreakServiceImpl(
            BreakRecordRepository breakRecordRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            EmployeeRepository employeeRepository,
            AttendanceEventRepository attendanceEventRepository,
            BreakPolicyRepository breakPolicyRepository,
            AnomalyFlagRepository anomalyFlagRepository,
            AnomalyFlagService anomalyFlagService) {

        this.breakRecordRepository = breakRecordRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceEventRepository = attendanceEventRepository;
        this.breakPolicyRepository = breakPolicyRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.anomalyFlagService = anomalyFlagService;
    }

    @Override
    @Transactional
    public BreakDTO startBreak(Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + employeeId));

        if (!employee.isActive()) {
            throw new IllegalArgumentException("Employee is inactive");
        }

        Optional<AttendanceSession> checkedInSession =
                attendanceSessionRepository.findByEmployeeAndStatus(
                        employee, AttendanceStatus.CHECKED_IN);

        if (checkedInSession.isEmpty()) {

            // Check whether the employee is already on a break.
            Optional<AttendanceSession> breakSession =
                    attendanceSessionRepository.findByEmployeeAndStatus(
                            employee, AttendanceStatus.ON_BREAK);

            if (breakSession.isPresent()) {

                recordBreakSequenceAnomaly(
                        employee,
                        AttendanceEventType.BREAK_START,
                        "DUPLICATE_BREAK_START");

                throw new IllegalArgumentException(
                        "Employee is already on a break");
            }

            // Preserve the invalid break-start attempt for manager review.
            recordBreakSequenceAnomaly(
                    employee,
                    AttendanceEventType.BREAK_START,
                    "BREAK_START_WITHOUT_CHECK_IN");

            throw new IllegalArgumentException(
                    "Employee is not currently checked in");
        }

        AttendanceSession session = checkedInSession.get();

        if (breakRecordRepository.existsByAttendanceSessionAndEndTimeIsNull(session)) {
            throw new IllegalArgumentException("Employee is already on a break");
        }

        BreakRecord breakRecord = new BreakRecord();
        breakRecord.setAttendanceSession(session);
        breakRecord.setStartTime(Instant.now());

        session.setStatus(AttendanceStatus.ON_BREAK);

        breakRecord = breakRecordRepository.save(breakRecord);
        attendanceSessionRepository.save(session);

        recordBreakEvent(session, AttendanceEventType.BREAK_START);

        return convertToDTO(breakRecord);
    }

    @Override
    @Transactional
    public BreakDTO endBreak(Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with ID: " + employeeId));

        if (!employee.isActive()) {
            throw new IllegalArgumentException("Employee is inactive");
        }

        Optional<AttendanceSession> breakSession =
                attendanceSessionRepository.findByEmployeeAndStatus(
                        employee, AttendanceStatus.ON_BREAK);

        if (breakSession.isEmpty()) {

            // An employee cannot end a break without an active break session.
            recordBreakSequenceAnomaly(
                    employee,
                    AttendanceEventType.BREAK_END,
                    "BREAK_END_WITHOUT_START");

            throw new IllegalArgumentException(
                    "Employee is not currently on a break");
        }

        AttendanceSession session = breakSession.get();

        Optional<BreakRecord> activeBreak =
                breakRecordRepository.findByAttendanceSessionAndEndTimeIsNull(session);

        if (activeBreak.isEmpty()) {

            recordBreakSequenceAnomaly(
                    employee,
                    AttendanceEventType.BREAK_END,
                    "BREAK_END_WITHOUT_START");

            throw new IllegalArgumentException(
                    "No active break record found");
        }

        BreakRecord breakRecord = activeBreak.get();

        breakRecord.setEndTime(Instant.now());

        session.setStatus(AttendanceStatus.CHECKED_IN);

        breakRecord = breakRecordRepository.save(breakRecord);
        attendanceSessionRepository.save(session);

        recordBreakEvent(session, AttendanceEventType.BREAK_END);

        return convertToDTO(breakRecord);
    }

    /*
     * Records an invalid break sequence independently of the current
     * transaction, so the anomaly is not lost when the operation is rejected.
     */
    private void recordBreakSequenceAnomaly(
            Employee employee,
            AttendanceEventType eventType,
            String ruleIdentifier) {

        anomalyFlagService.recordLocationAnomaly(
                employee,
                eventType,
                null,
                null,
                null,
                ruleIdentifier,
                AnomalySeverity.MEDIUM);
    }

    @Override
    @Transactional(readOnly = true)
    public String getTotalBreakDuration(Long attendanceSessionId) {

        AttendanceSession session = attendanceSessionRepository
                .findById(attendanceSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attendance session not found with ID: " + attendanceSessionId));

        List<BreakRecord> breaks = breakRecordRepository
                .findByAttendanceSessionOrderByStartTimeAsc(session);

        long totalSeconds = 0;

        for (BreakRecord breakRecord : breaks) {

            if (breakRecord.getEndTime() != null) {

                totalSeconds += Duration.between(
                        breakRecord.getStartTime(),
                        breakRecord.getEndTime()
                ).getSeconds();
            }
        }

        return formatDuration(totalSeconds);
    }

    @Override
    @Transactional(readOnly = true)
    public String getNetWorkingDuration(Long attendanceSessionId) {

        AttendanceSession session = attendanceSessionRepository
                .findById(attendanceSessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attendance session not found with ID: " + attendanceSessionId));

        if (session.getCheckOutTime() == null) {
            throw new IllegalArgumentException(
                    "Employee has not checked out yet");
        }

        long totalSeconds = Duration.between(
                session.getCheckInTime(),
                session.getCheckOutTime()
        ).getSeconds();

        long breakSeconds = calculateTotalBreakSeconds(session);

        long netWorkingSeconds = totalSeconds - breakSeconds;

        if (netWorkingSeconds < 0) {
            throw new IllegalStateException(
                    "Break duration cannot exceed attendance duration");
        }

        return formatDuration(netWorkingSeconds);
    }

    // Automatically checks unfinished breaks at the configured interval.
    @Scheduled(fixedRateString = "${attendance.break-detection.interval-ms:60000}")
    @Transactional
    @Override
    public void detectForgottenBreaks() {

        BreakPolicy policy = breakPolicyRepository.findFirstByActiveTrue()
                .orElse(null);

        if (policy == null) {
            return;
        }

        Instant currentTime = Instant.now();

        List<BreakRecord> unfinishedBreaks =
                breakRecordRepository.findByEndTimeIsNull();

        for (BreakRecord breakRecord : unfinishedBreaks) {

            long elapsedMinutes = Duration.between(
                    breakRecord.getStartTime(),
                    currentTime
            ).toMinutes();

            if (elapsedMinutes >= policy.getMaxDurationPerBreakMinutes()) {

                String ruleIdentifier = "FORGOTTEN_BREAK";

                boolean alreadyFlagged = anomalyFlagRepository
                        .existsByBreakRecordAndRuleIdentifier(
                                breakRecord,
                                ruleIdentifier);

                if (!alreadyFlagged) {

                    AnomalyFlag anomalyFlag = new AnomalyFlag();

                    anomalyFlag.setEmployee(
                            breakRecord.getAttendanceSession().getEmployee());

                    anomalyFlag.setBreakRecord(breakRecord);

                    anomalyFlag.setRuleIdentifier(ruleIdentifier);

                    anomalyFlag.setDetectedAt(currentTime);

                    long allowedMinutes = policy.getMaxDurationPerBreakMinutes();

                    if (elapsedMinutes >= allowedMinutes * 2) {
                        anomalyFlag.setSeverity(AnomalySeverity.HIGH);
                    } else if (elapsedMinutes >= allowedMinutes * 1.5) {
                        anomalyFlag.setSeverity(AnomalySeverity.MEDIUM);
                    } else {
                        anomalyFlag.setSeverity(AnomalySeverity.LOW);
                    }

                    anomalyFlag.setReviewStatus(AnomalyReviewStatus.PENDING);

                    anomalyFlagRepository.save(anomalyFlag);
                }
            }
        }
    }

    private long calculateTotalBreakSeconds(AttendanceSession session) {

        List<BreakRecord> breaks = breakRecordRepository
                .findByAttendanceSessionOrderByStartTimeAsc(session);

        long totalSeconds = 0;

        for (BreakRecord breakRecord : breaks) {

            if (breakRecord.getEndTime() != null) {

                totalSeconds += Duration.between(
                        breakRecord.getStartTime(),
                        breakRecord.getEndTime()
                ).getSeconds();
            }
        }

        return totalSeconds;
    }

    private String formatDuration(long seconds) {

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        return hours + " hours " + minutes + " minutes";
    }

    private void recordBreakEvent(
            AttendanceSession session,
            AttendanceEventType eventType) {

        AttendanceEvent event = new AttendanceEvent();

        event.setAttendanceSession(session);
        event.setEventType(eventType);
        event.setServerReceivedAt(Instant.now());

        attendanceEventRepository.save(event);
    }

    private BreakDTO convertToDTO(BreakRecord breakRecord) {

        Long durationInSeconds = null;

        if (breakRecord.getEndTime() != null) {

            durationInSeconds = Duration.between(
                    breakRecord.getStartTime(),
                    breakRecord.getEndTime()
            ).getSeconds();
        }

        return new BreakDTO(
                breakRecord.getId(),
                breakRecord.getAttendanceSession().getId(),
                breakRecord.getStartTime(),
                breakRecord.getEndTime(),
                durationInSeconds,
                breakRecord.getAttendanceSession().getStatus()
        );
    }
}
