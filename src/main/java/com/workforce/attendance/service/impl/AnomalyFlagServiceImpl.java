
package com.workforce.attendance.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.workforce.attendance.dto.AnomalyFlagDTO;
import com.workforce.attendance.entity.AnomalyFlag;
import com.workforce.attendance.entity.AnomalyReviewStatus;
import com.workforce.attendance.entity.AnomalySeverity;
import com.workforce.attendance.entity.AttendanceEvent;
import com.workforce.attendance.entity.AttendanceEventType;
import com.workforce.attendance.entity.AuditRecord;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.AnomalyFlagRepository;
import com.workforce.attendance.repository.AttendanceEventRepository;
import com.workforce.attendance.repository.AuditRecordRepository;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.AnomalyFlagService;

@Service
@Transactional(readOnly = true)
public class AnomalyFlagServiceImpl implements AnomalyFlagService {

    private final AnomalyFlagRepository anomalyFlagRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditRecordRepository auditRecordRepository;

    public AnomalyFlagServiceImpl(
            AnomalyFlagRepository anomalyFlagRepository,
            EmployeeRepository employeeRepository,
            AttendanceEventRepository attendanceEventRepository,
            UserAccountRepository userAccountRepository,
            AuditRecordRepository auditRecordRepository) {

        this.anomalyFlagRepository = anomalyFlagRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceEventRepository = attendanceEventRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditRecordRepository = auditRecordRepository;
    }

    @Override
    public List<AnomalyFlagDTO> getAllAnomalies() {

        return anomalyFlagRepository.findAllByOrderByDetectedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AnomalyFlagDTO> getEmployeeAnomalies(Long employeeId) {

        return anomalyFlagRepository
                .findByEmployeeIdOrderByDetectedAtDesc(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AnomalyFlagDTO> getPendingAnomaliesForManager(
            Long managerId) {

        Employee manager = getActiveManager(managerId);

        return anomalyFlagRepository
                .findByEmployee_Manager_IdAndReviewStatusOrderByDetectedAtDesc(
                        manager.getId(),
                        AnomalyReviewStatus.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AnomalyFlagDTO reviewAnomaly(
            Long anomalyId,
            Long managerId,
            String reviewOutcome,
            String correlationId) {

        if (reviewOutcome == null || reviewOutcome.isBlank()) {
            throw new IllegalArgumentException(
                    "Review outcome is required");
        }

        if (reviewOutcome.length() > 500) {
            throw new IllegalArgumentException(
                    "Review outcome cannot exceed 500 characters");
        }

        Employee manager = getActiveManager(managerId);

        // Load the anomaly before checking ownership.
        AnomalyFlag anomaly = anomalyFlagRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Anomaly not found with ID: " + anomalyId));

        // Managers can review only anomalies belonging to their direct reports.
        if (anomaly.getEmployee().getManager() == null
                || !anomaly.getEmployee().getManager().getId()
                        .equals(manager.getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to review this anomaly");
        }

        // Prevent a second manager review from overwriting the first outcome.
        if (anomaly.getReviewStatus() == AnomalyReviewStatus.REVIEWED) {
            throw new IllegalArgumentException(
                    "This anomaly has already been reviewed");
        }

        // Preserve the previous review state for the audit record.
        AnomalyReviewStatus previousStatus = anomaly.getReviewStatus();

        anomaly.setReviewStatus(AnomalyReviewStatus.REVIEWED);
        anomaly.setReviewedBy(manager);
        anomaly.setReviewedAt(Instant.now());
        anomaly.setReviewOutcome(reviewOutcome.trim());

        AnomalyFlag savedAnomaly =
                anomalyFlagRepository.save(anomaly);

        // Retrieve the login account of the authenticated manager.
        UserAccount managerAccount = userAccountRepository
                .findByEmployeeId(manager.getId())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new IllegalStateException(
                        "Active manager login account not found"));

        // Record the anomaly review in the audit trail.
        AuditRecord audit = new AuditRecord();

        audit.setActor(managerAccount);
        audit.setAction("ANOMALY_REVIEWED");
        audit.setTargetRecord("AnomalyFlag:" + savedAnomaly.getId());
        audit.setTimestamp(Instant.now());
        audit.setRelevantChanges(
                "Previous review status: " + previousStatus
                        + "; New review status: REVIEWED"
                        + "; Rule: " + savedAnomaly.getRuleIdentifier()
                        + "; Severity: " + savedAnomaly.getSeverity()
                        + "; Review outcome: " + savedAnomaly.getReviewOutcome()
                        + "; Reviewed by employee ID: " + manager.getId());

        audit.setRequestCorrelationId(correlationId);

        auditRecordRepository.save(audit);

        return convertToDTO(savedAnomaly);
    }

    /*
     * Saves a suspicious attendance attempt independently of the main
     * attendance transaction, so its evidence survives attendance rejection.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLocationAnomaly(
            Employee employee,
            AttendanceEventType eventType,
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            String ruleIdentifier,
            AnomalySeverity severity) {

        Instant detectedTime = Instant.now();

        // Preserve the attempted location even though attendance is rejected.
        AttendanceEvent event = new AttendanceEvent();
        event.setAttendanceSession(null);
        event.setEventType(eventType);
        event.setServerReceivedAt(detectedTime);
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        event.setLocationAccuracyMeters(accuracyMeters);

        AttendanceEvent savedEvent =
                attendanceEventRepository.save(event);

        AnomalyFlag anomaly = new AnomalyFlag();
        anomaly.setEmployee(employee);
        anomaly.setAttendanceEvent(savedEvent);
        anomaly.setRuleIdentifier(ruleIdentifier);
        anomaly.setDetectedAt(detectedTime);
        anomaly.setSeverity(severity);
        anomaly.setReviewStatus(AnomalyReviewStatus.PENDING);

        anomalyFlagRepository.save(anomaly);
    }

    private Employee getActiveManager(Long managerId) {

        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Manager not found with ID: " + managerId));

        if (!manager.isActive()) {
            throw new IllegalArgumentException(
                    "Manager is inactive");
        }

        return manager;
    }

    // Converts the entity into a DTO without exposing JPA entity objects
    // or triggering unnecessary database serialization.
    private AnomalyFlagDTO convertToDTO(AnomalyFlag anomalyFlag) {

        AnomalyFlagDTO dto = new AnomalyFlagDTO();

        dto.setId(anomalyFlag.getId());
        dto.setEmployeeId(anomalyFlag.getEmployee().getId());
        dto.setEmployeeName(anomalyFlag.getEmployee().getName());

        if (anomalyFlag.getAttendanceEvent() != null) {
            dto.setAttendanceEventId(
                    anomalyFlag.getAttendanceEvent().getId());
        }

        if (anomalyFlag.getBreakRecord() != null) {
            dto.setBreakRecordId(
                    anomalyFlag.getBreakRecord().getId());
        }

        dto.setRuleIdentifier(anomalyFlag.getRuleIdentifier());
        dto.setDetectedAt(anomalyFlag.getDetectedAt());
        dto.setSeverity(anomalyFlag.getSeverity());
        dto.setReviewStatus(anomalyFlag.getReviewStatus());

        if (anomalyFlag.getReviewedBy() != null) {
            dto.setReviewedBy(anomalyFlag.getReviewedBy().getId());
        }

        dto.setReviewedAt(anomalyFlag.getReviewedAt());
        dto.setReviewOutcome(anomalyFlag.getReviewOutcome());

        return dto;
    }
}
