package com.workforce.attendance.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.FieldVisitDTO;
import com.workforce.attendance.entity.AttendanceEventType;
import com.workforce.attendance.entity.AttendanceStatus;
import com.workforce.attendance.entity.AnomalySeverity;
import com.workforce.attendance.entity.AuditRecord;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.FieldVisit;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.entity.VisitStatus;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.AttendanceSessionRepository;
import com.workforce.attendance.repository.AuditRecordRepository;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.FieldVisitRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.AnomalyFlagService;
import com.workforce.attendance.service.FieldVisitService;

@Service
@Transactional
public class FieldVisitServiceImpl implements FieldVisitService {

    private final FieldVisitRepository fieldVisitRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AnomalyFlagService anomalyFlagService;
    private final AuditRecordRepository auditRecordRepository;
    private final UserAccountRepository userAccountRepository;

    public FieldVisitServiceImpl(
            FieldVisitRepository fieldVisitRepository,
            EmployeeRepository employeeRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            AnomalyFlagService anomalyFlagService,
            AuditRecordRepository auditRecordRepository,
            UserAccountRepository userAccountRepository) {

        this.fieldVisitRepository = fieldVisitRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.anomalyFlagService = anomalyFlagService;
        this.auditRecordRepository = auditRecordRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // Start a new field visit
    @Override
    public FieldVisitDTO startVisit(FieldVisitDTO dto) {

        if (dto.getEmployeeId() == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        Employee employee = employeeRepository
                .findByIdForUpdate(dto.getEmployeeId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found"));

        if (!employee.isActive()) {
            throw new IllegalArgumentException("Employee is inactive");
        }

        if (dto.getVisitType() == null) {
            throw new IllegalArgumentException("Visit type is required");
        }

        if (dto.getCustomerReference() == null
                || dto.getCustomerReference().isBlank()) {
            throw new IllegalArgumentException(
                    "Customer reference is required");
        }

        if (dto.getCustomerReference().length() > 100) {
            throw new IllegalArgumentException(
                    "Customer reference cannot exceed 100 characters");
        }

        validateCoordinates(
                dto.getStartLatitude(),
                dto.getStartLongitude());

        attendanceSessionRepository
                .findByEmployeeAndStatus(
                        employee, AttendanceStatus.CHECKED_IN)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee must be checked in before starting a field visit"));

        boolean activeVisitExists =
                fieldVisitRepository.existsByEmployeeAndStatus(
                        employee, VisitStatus.IN_PROGRESS);

        if (activeVisitExists) {

            anomalyFlagService.recordLocationAnomaly(
                    employee,
                    AttendanceEventType.FIELD_VISIT_START,
                    dto.getStartLatitude(),
                    dto.getStartLongitude(),
                    null,
                    "OVERLAPPING_FIELD_VISIT",
                    AnomalySeverity.MEDIUM);

            throw new IllegalArgumentException(
                    "Employee already has an active field visit");
        }

        FieldVisit fieldVisit = new FieldVisit();

        fieldVisit.setEmployee(employee);
        fieldVisit.setVisitType(dto.getVisitType());
        fieldVisit.setCustomerReference(
                dto.getCustomerReference().trim());

        fieldVisit.setStartTime(Instant.now());
        fieldVisit.setStartLatitude(dto.getStartLatitude());
        fieldVisit.setStartLongitude(dto.getStartLongitude());

        fieldVisit.setStatus(VisitStatus.IN_PROGRESS);
        fieldVisit.setRemarks(dto.getRemarks());

        FieldVisit savedVisit = fieldVisitRepository.save(fieldVisit);

        return convertToDTO(savedVisit);
    }

    // Complete an existing field visit
    @Override
    public FieldVisitDTO completeVisit(Long visitId, FieldVisitDTO dto) {

        FieldVisit fieldVisit = fieldVisitRepository.findById(visitId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Field visit not found"));

        if (fieldVisit.getStatus() != VisitStatus.IN_PROGRESS) {
            throw new IllegalArgumentException(
                    "This field visit is already completed");
        }

        validateCoordinates(
                dto.getEndLatitude(),
                dto.getEndLongitude());

        fieldVisit.setEndTime(Instant.now());
        fieldVisit.setEndLatitude(dto.getEndLatitude());
        fieldVisit.setEndLongitude(dto.getEndLongitude());

        fieldVisit.setStatus(VisitStatus.COMPLETED);

        if (dto.getRemarks() != null) {
            fieldVisit.setRemarks(dto.getRemarks());
        }

        FieldVisit updatedVisit = fieldVisitRepository.save(fieldVisit);

        return convertToDTO(updatedVisit);
    }

    // Correct an existing field visit and record its audit history
    @Override
    public FieldVisitDTO correctVisit(
            Long visitId,
            FieldVisitDTO dto,
            String actorUsername,
            String correlationId) {

        FieldVisit visit = fieldVisitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Field visit not found"));

        UserAccount actor = userAccountRepository
                .findAccountWithEmployeeAndRoleByUsername(actorUsername)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Authenticated user account not found"));

        if (actor.getRole() == null
                || actor.getRole().getName() == null) {
            throw new AccessDeniedException("User role is missing");
        }

        String role = actor.getRole().getName().trim().toUpperCase();

        if (!role.equals("ADMIN") && !role.equals("MANAGER")) {
            throw new AccessDeniedException(
                    "Only administrators and managers can correct field visits");
        }

        // Managers can correct only their own direct reports' visits.
        if (role.equals("MANAGER")) {

            Employee manager = actor.getEmployee();

            if (manager == null || visit.getEmployee().getManager() == null
                    || !visit.getEmployee().getManager().getId()
                            .equals(manager.getId())) {

                throw new AccessDeniedException(
                        "You can correct visits belonging only to your direct reports");
            }
        }

        // The employee assigned to a visit cannot be changed by correction.
        if (dto.getEmployeeId() == null
                || !dto.getEmployeeId().equals(
                        visit.getEmployee().getId())) {

            throw new IllegalArgumentException(
                    "The employee assigned to a field visit cannot be changed");
        }

        if (dto.getVisitType() == null) {
            throw new IllegalArgumentException("Visit type is required");
        }

        if (dto.getCustomerReference() == null
                || dto.getCustomerReference().isBlank()
                || dto.getCustomerReference().length() > 100) {

            throw new IllegalArgumentException(
                    "A valid customer reference is required");
        }

        if (dto.getStartTime() == null) {
            throw new IllegalArgumentException(
                    "Visit start time is required");
        }

        validateCoordinates(
                dto.getStartLatitude(),
                dto.getStartLongitude());

        boolean completed = dto.getEndTime() != null;

        if (completed) {

            if (!dto.getEndTime().isAfter(dto.getStartTime())) {
                throw new IllegalArgumentException(
                        "Visit end time must be after start time");
            }

            validateCoordinates(
                    dto.getEndLatitude(),
                    dto.getEndLongitude());

        } else if (dto.getEndLatitude() != null
                || dto.getEndLongitude() != null) {

            throw new IllegalArgumentException(
                    "End coordinates cannot be provided without an end time");
        }

        if (dto.getRemarks() != null
                && dto.getRemarks().length() > 500) {

            throw new IllegalArgumentException(
                    "Remarks cannot exceed 500 characters");
        }

        // Capture original values before making changes.
        String originalValues = buildVisitSnapshot(visit);

        // Apply corrected values.
        visit.setVisitType(dto.getVisitType());
        visit.setCustomerReference(dto.getCustomerReference().trim());

        visit.setStartTime(dto.getStartTime());
        visit.setStartLatitude(dto.getStartLatitude());
        visit.setStartLongitude(dto.getStartLongitude());

        visit.setEndTime(dto.getEndTime());
        visit.setEndLatitude(dto.getEndLatitude());
        visit.setEndLongitude(dto.getEndLongitude());

        visit.setRemarks(dto.getRemarks());

        visit.setStatus(
                completed ? VisitStatus.COMPLETED : VisitStatus.IN_PROGRESS);

        FieldVisit savedVisit = fieldVisitRepository.save(visit);

        String correctedValues = buildVisitSnapshot(savedVisit);

        AuditRecord audit = new AuditRecord();

        audit.setActor(actor);
        audit.setAction("FIELD_VISIT_CORRECTED");
        audit.setTargetRecord("FieldVisit:" + savedVisit.getId());
        audit.setTimestamp(Instant.now());

        audit.setRelevantChanges(
                "Original values: " + originalValues
                        + " | Corrected values: " + correctedValues);

        audit.setRequestCorrelationId(correlationId);

        auditRecordRepository.save(audit);

        return convertToDTO(savedVisit);
    }

    // Get visit by ID
    @Override
    @Transactional(readOnly = true)
    public FieldVisitDTO getVisitById(Long visitId) {

        FieldVisit fieldVisit = fieldVisitRepository.findById(visitId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Field visit not found"));

        return convertToDTO(fieldVisit);
    }

    // Get all field visits
    @Override
    @Transactional(readOnly = true)
    public List<FieldVisitDTO> getAllVisits() {

        return fieldVisitRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get all visits of an employee
    @Override
    @Transactional(readOnly = true)
    public List<FieldVisitDTO> getEmployeeVisits(Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found"));

        return fieldVisitRepository
                .findByEmployeeOrderByStartTimeDesc(employee)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get employee's visits for a particular date
    @Override
    @Transactional(readOnly = true)
    public List<FieldVisitDTO> getEmployeeDailyVisits(
            Long employeeId, LocalDate date) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found"));

        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        ZoneId zone = ZoneId.systemDefault();

        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();

        return fieldVisitRepository
                .findByEmployeeAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeDesc(
                        employee, start, end)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Get visits belonging to a manager's direct reports
    @Override
    @Transactional(readOnly = true)
    public List<FieldVisitDTO> getTeamVisits(
            Long managerId, LocalDate date) {

        employeeRepository.findById(managerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Manager not found"));

        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        ZoneId zone = ZoneId.systemDefault();

        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();

        return fieldVisitRepository
                .findByEmployee_Manager_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeDesc(
                        managerId, start, end)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Validate GPS coordinates
    private void validateCoordinates(
            Double latitude, Double longitude) {

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException(
                    "GPS coordinates are required");
        }

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid latitude");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid longitude");
        }
    }

    // Build a readable before/after snapshot for the audit record
    private String buildVisitSnapshot(FieldVisit visit) {

        return "{"
                + "employeeId=" + visit.getEmployee().getId()
                + ", visitType=" + visit.getVisitType()
                + ", customerReference=" + visit.getCustomerReference()
                + ", startTime=" + visit.getStartTime()
                + ", endTime=" + visit.getEndTime()
                + ", startLatitude=" + visit.getStartLatitude()
                + ", startLongitude=" + visit.getStartLongitude()
                + ", endLatitude=" + visit.getEndLatitude()
                + ", endLongitude=" + visit.getEndLongitude()
                + ", status=" + visit.getStatus()
                + ", remarks=" + visit.getRemarks()
                + "}";
    }

    // Convert entity to DTO
    private FieldVisitDTO convertToDTO(FieldVisit fieldVisit) {

        FieldVisitDTO dto = new FieldVisitDTO();

        dto.setId(fieldVisit.getId());

        dto.setEmployeeId(fieldVisit.getEmployee().getId());
        dto.setEmployeeName(fieldVisit.getEmployee().getName());

        dto.setVisitType(fieldVisit.getVisitType());
        dto.setCustomerReference(fieldVisit.getCustomerReference());

        dto.setStartTime(fieldVisit.getStartTime());
        dto.setEndTime(fieldVisit.getEndTime());

        dto.setStartLatitude(fieldVisit.getStartLatitude());
        dto.setStartLongitude(fieldVisit.getStartLongitude());

        dto.setEndLatitude(fieldVisit.getEndLatitude());
        dto.setEndLongitude(fieldVisit.getEndLongitude());

        dto.setStatus(fieldVisit.getStatus());
        dto.setRemarks(fieldVisit.getRemarks());

        if (fieldVisit.getEndTime() != null) {

            long duration = Duration.between(
                    fieldVisit.getStartTime(),
                    fieldVisit.getEndTime()).getSeconds();

            dto.setDurationInSeconds(duration);
        }

        return dto;
    }
}