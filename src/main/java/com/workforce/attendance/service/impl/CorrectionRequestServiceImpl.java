
package com.workforce.attendance.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.CorrectionRequestDTO;
import com.workforce.attendance.entity.AttendanceSession;
import com.workforce.attendance.entity.AttendanceStatus;
import com.workforce.attendance.entity.AuditRecord;
import com.workforce.attendance.entity.CorrectionRequest;
import com.workforce.attendance.entity.CorrectionStatus;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.AttendanceSessionRepository;
import com.workforce.attendance.repository.AuditRecordRepository;
import com.workforce.attendance.repository.CorrectionRequestRepository;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.CorrectionRequestService;

@Service
public class CorrectionRequestServiceImpl
        implements CorrectionRequestService {

    private final CorrectionRequestRepository correctionRequestRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditRecordRepository auditRecordRepository;

    public CorrectionRequestServiceImpl(
            CorrectionRequestRepository correctionRequestRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            EmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository,
            AuditRecordRepository auditRecordRepository) {

        this.correctionRequestRepository = correctionRequestRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditRecordRepository = auditRecordRepository;
    }

    // 1. Employee submits a correction request

    @Override
    @Transactional
    public CorrectionRequestDTO submitCorrection(CorrectionRequestDTO dto) {

        if (dto.getAttendanceSessionId() == null ||
                dto.getEmployeeId() == null) {
            throw new IllegalArgumentException(
                    "Employee ID and attendance session ID are required");
        }

        if (dto.getRequestedCheckInTime() == null &&
                dto.getRequestedCheckOutTime() == null) {
            throw new IllegalArgumentException(
                    "At least one corrected attendance time is required");
        }

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found"));

        if (!employee.isActive()) {
            throw new IllegalArgumentException(
                    "Inactive employees cannot submit correction requests");
        }

        AttendanceSession session =
                attendanceSessionRepository.findByIdForUpdate(
                        dto.getAttendanceSessionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attendance session not found"));

        if (!session.getEmployee().getId().equals(employee.getId())) {
            throw new IllegalArgumentException(
                    "Attendance session does not belong to this employee");
        }

        if (correctionRequestRepository.existsByAttendanceSessionIdAndStatus(
                session.getId(), CorrectionStatus.PENDING)) {
            throw new IllegalArgumentException(
                    "A pending correction request already exists for this session");
        }

        Instant requestedCheckIn = dto.getRequestedCheckInTime() != null
                ? dto.getRequestedCheckInTime()
                : session.getCheckInTime();

        Instant requestedCheckOut = dto.getRequestedCheckOutTime() != null
                ? dto.getRequestedCheckOutTime()
                : session.getCheckOutTime();

        validateAttendanceTimes(requestedCheckIn, requestedCheckOut);

        CorrectionRequest request = new CorrectionRequest();

        request.setEmployee(employee);
        request.setAttendanceSession(session);
        request.setReason(dto.getReason());

        // Preserve the original attendance values.
        request.setOriginalCheckInTime(session.getCheckInTime());
        request.setOriginalCheckOutTime(session.getCheckOutTime());

        request.setRequestedCheckInTime(dto.getRequestedCheckInTime());
        request.setRequestedCheckOutTime(dto.getRequestedCheckOutTime());

        request.setRequestedTime(Instant.now());
        request.setStatus(CorrectionStatus.PENDING);

        CorrectionRequest saved =
                correctionRequestRepository.save(request);

        // Login account is optional during development.
        UserAccount employeeAccount =
                getActiveUserAccount(employee.getId());

        saveAudit(
                employeeAccount,
                "CORRECTION_SUBMITTED",
                saved.getId(),
                "Submitted by employee ID: " + employee.getId()
                        + "; Reason: " + saved.getReason()
                        + "; Original check-in: "
                        + saved.getOriginalCheckInTime()
                        + "; Original check-out: "
                        + saved.getOriginalCheckOutTime()
                        + "; Requested check-in: "
                        + saved.getRequestedCheckInTime()
                        + "; Requested check-out: "
                        + saved.getRequestedCheckOutTime()
        );

        return convertToDTO(saved);
    }

    // 2. Employee views their correction requests

    @Override
    @Transactional(readOnly = true)
    public List<CorrectionRequestDTO> getEmployeeRequests(Long employeeId) {

        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found");
        }

        return correctionRequestRepository
                .findByEmployeeIdOrderByRequestedTimeDesc(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 3. Manager views pending requests from assigned employees

    @Override
    @Transactional(readOnly = true)
    public List<CorrectionRequestDTO> getPendingRequestsForManager(
            Long managerId) {

        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Manager not found"));

        if (!manager.isActive()) {
            throw new IllegalArgumentException("Manager is inactive");
        }

        return correctionRequestRepository
                .findByEmployee_Manager_IdAndStatusOrderByRequestedTimeAsc(
                        managerId, CorrectionStatus.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 4. Manager approves a correction request

    @Override
    @Transactional
    public CorrectionRequestDTO approveCorrection(
            Long requestId, Long managerId) {

        CorrectionRequest request =
                correctionRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Correction request not found"));

        validateManager(request, managerId);

        if (request.getStatus() != CorrectionStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This correction request has already been reviewed");
        }

        AttendanceSession session =
                attendanceSessionRepository.findByIdForUpdate(
                        request.getAttendanceSession().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attendance session not found"));

        // Prevent applying a correction based on outdated attendance data.
        if (!equalTimes(session.getCheckInTime(),
                        request.getOriginalCheckInTime()) ||
                !equalTimes(session.getCheckOutTime(),
                        request.getOriginalCheckOutTime())) {

            throw new IllegalArgumentException(
                    "Attendance data has changed since this request was submitted");
        }

        Instant correctedCheckIn =
                request.getRequestedCheckInTime() != null
                ? request.getRequestedCheckInTime()
                : session.getCheckInTime();

        Instant correctedCheckOut =
                request.getRequestedCheckOutTime() != null
                ? request.getRequestedCheckOutTime()
                : session.getCheckOutTime();

        validateAttendanceTimes(correctedCheckIn, correctedCheckOut);

        // Apply the approved correction.
        session.setCheckInTime(correctedCheckIn);
        session.setCheckOutTime(correctedCheckOut);

        // Update attendance status when a checkout time exists.
        if (correctedCheckOut != null) {
            session.setStatus(AttendanceStatus.CHECKED_OUT);
        }

        attendanceSessionRepository.save(session);

        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Manager not found"));

        request.setStatus(CorrectionStatus.APPROVED);
        request.setReviewedBy(manager);
        request.setReviewedAt(Instant.now());

        CorrectionRequest saved =
                correctionRequestRepository.save(request);

        // Login account is optional during development.
        UserAccount managerAccount = getActiveUserAccount(managerId);

        saveAudit(
                managerAccount,
                "CORRECTION_APPROVED",
                saved.getId(),
                "Original check-in: " + saved.getOriginalCheckInTime()
                        + "; Original check-out: "
                        + saved.getOriginalCheckOutTime()
                        + "; Applied check-in: " + correctedCheckIn
                        + "; Applied check-out: " + correctedCheckOut
                        + "; Approved by employee ID: " + managerId
        );

        return convertToDTO(saved);
    }

    // 5. Manager rejects a correction request

    @Override
    @Transactional
    public CorrectionRequestDTO rejectCorrection(
            Long requestId, Long managerId) {

        CorrectionRequest request =
                correctionRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Correction request not found"));

        validateManager(request, managerId);

        if (request.getStatus() != CorrectionStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This correction request has already been reviewed");
        }

        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Manager not found"));

        request.setStatus(CorrectionStatus.REJECTED);
        request.setReviewedBy(manager);
        request.setReviewedAt(Instant.now());

        CorrectionRequest saved =
                correctionRequestRepository.save(request);

        UserAccount managerAccount = getActiveUserAccount(managerId);

        saveAudit(
                managerAccount,
                "CORRECTION_REJECTED",
                saved.getId(),
                "Reason: " + saved.getReason()
                        + "; Rejected by employee ID: " + managerId
        );

        return convertToDTO(saved);
    }

    // Validate manager ownership and prevent self-approval.

    private void validateManager(
            CorrectionRequest request, Long managerId) {

        if (request.getEmployee().getId().equals(managerId)) {
            throw new AccessDeniedException(
                    "An employee cannot approve or reject their own correction");
        }

        Employee employee = request.getEmployee();

        if (employee.getManager() == null ||
                !employee.getManager().getId().equals(managerId)) {

            throw new AccessDeniedException(
                    "You are not authorized to review this employee's request");
        }
    }

    // Validate chronological attendance timestamps.

    private void validateAttendanceTimes(
            Instant checkIn, Instant checkOut) {

        if (checkIn == null) {
            throw new IllegalArgumentException(
                    "Check-in time cannot be null");
        }

        if (checkOut != null && !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(
                    "Check-out time must be after check-in time");
        }
    }

    private boolean equalTimes(Instant first, Instant second) {
        if (first == null) {
            return second == null;
        }

        return first.equals(second);
    }

    // Retrieve the linked active login account.
    // Return null if no account exists during development.

    private UserAccount getActiveUserAccount(Long employeeId) {

        return userAccountRepository.findByEmployeeId(employeeId)
                .filter(UserAccount::isActive)
                .orElse(null);
    }

    // Save an audit entry.

    private void saveAudit(
            UserAccount actor,
            String action,
            Long requestId,
            String changes) {

        AuditRecord audit = new AuditRecord();

        audit.setActor(actor);
        audit.setAction(action);
        audit.setTargetRecord("CorrectionRequest:" + requestId);
        audit.setTimestamp(Instant.now());
        audit.setRelevantChanges(changes);

        auditRecordRepository.save(audit);
    }

    // Convert entity to response DTO.

    private CorrectionRequestDTO convertToDTO(
            CorrectionRequest request) {

        CorrectionRequestDTO dto = new CorrectionRequestDTO();

        dto.setId(request.getId());

        dto.setAttendanceSessionId(
                request.getAttendanceSession().getId());

        dto.setEmployeeId(request.getEmployee().getId());

        dto.setReason(request.getReason());

        dto.setOriginalCheckInTime(
                request.getOriginalCheckInTime());

        dto.setOriginalCheckOutTime(
                request.getOriginalCheckOutTime());

        dto.setRequestedCheckInTime(
                request.getRequestedCheckInTime());

        dto.setRequestedCheckOutTime(
                request.getRequestedCheckOutTime());

        dto.setRequestedTime(request.getRequestedTime());
        dto.setStatus(request.getStatus().name());

        if (request.getReviewedBy() != null) {
            dto.setReviewedBy(request.getReviewedBy().getId());
        }

        dto.setReviewedAt(request.getReviewedAt());

        return dto;
    }
}
