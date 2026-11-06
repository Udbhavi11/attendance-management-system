
package com.workforce.attendance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.workforce.attendance.dto.BreakDTO;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.AttendanceSessionRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.BreakService;

@RestController
@RequestMapping("/api/breaks")
public class BreakController {

    private final BreakService breakService;
    private final UserAccountRepository userAccountRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;

    public BreakController(
            BreakService breakService,
            UserAccountRepository userAccountRepository,
            AttendanceSessionRepository attendanceSessionRepository) {

        this.breakService = breakService;
        this.userAccountRepository = userAccountRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
    }

    @PostMapping("/start/{employeeId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public BreakDTO startBreak(
            @PathVariable Long employeeId,
            Authentication authentication) {

        validateOwnEmployee(employeeId, authentication);

        return breakService.startBreak(employeeId);
    }

    @PostMapping("/end/{employeeId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public BreakDTO endBreak(
            @PathVariable Long employeeId,
            Authentication authentication) {

        validateOwnEmployee(employeeId, authentication);

        return breakService.endBreak(employeeId);
    }

    @GetMapping("/total-duration/{attendanceSessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<String> getTotalBreakDuration(
            @PathVariable Long attendanceSessionId,
            Authentication authentication) {

        validateSessionAccess(attendanceSessionId, authentication);

        String duration =
                breakService.getTotalBreakDuration(attendanceSessionId);

        return ResponseEntity.ok(duration);
    }

    @GetMapping("/net-working-duration/{attendanceSessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<String> getNetWorkingDuration(
            @PathVariable Long attendanceSessionId,
            Authentication authentication) {

        validateSessionAccess(attendanceSessionId, authentication);

        String duration =
                breakService.getNetWorkingDuration(attendanceSessionId);

        return ResponseEntity.ok(duration);
    }

    private void validateOwnEmployee(
            Long requestedEmployeeId,
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);

        Employee employee = account.getEmployee();

        if (employee == null) {
            throw new AccessDeniedException(
                    "Your account is not linked to an employee");
        }

        if (!employee.getId().equals(requestedEmployeeId)) {
            throw new AccessDeniedException(
                    "You can perform break operations only for yourself");
        }
    }

    private void validateSessionAccess(
            Long attendanceSessionId,
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);

        String role = account.getRole().getName();

        // Admin and auditor can view organization-wide durations.
        if ("ADMIN".equals(role) || "AUDITOR".equals(role)) {

            if (!attendanceSessionRepository.existsById(attendanceSessionId)) {
                throw new ResourceNotFoundException(
                        "Attendance session not found with ID: "
                                + attendanceSessionId);
            }

            return;
        }

        Employee employee = account.getEmployee();

        if (employee == null) {
            throw new AccessDeniedException(
                    "Your account is not linked to an employee");
        }

        Long employeeId = employee.getId();

        // Employee and manager can view their own session.
        if (attendanceSessionRepository.existsByIdAndEmployee_Id(
                attendanceSessionId, employeeId)) {
            return;
        }

        // Manager can view sessions belonging to direct reports.
        if ("MANAGER".equals(role)
                && attendanceSessionRepository
                        .existsByIdAndEmployee_Manager_Id(
                                attendanceSessionId, employeeId)) {
            return;
        }

        if (!attendanceSessionRepository.existsById(attendanceSessionId)) {
            throw new ResourceNotFoundException(
                    "Attendance session not found with ID: "
                            + attendanceSessionId);
        }

        throw new AccessDeniedException(
                "You are not authorized to access this attendance session");
    }

    private UserAccount getAuthenticatedAccount(
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Authentication is required");
        }

        UserAccount account = userAccountRepository
                .findAccountWithEmployeeAndRoleByUsername(
                        authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "User account not found"));

        if (!account.isActive()) {
            throw new AccessDeniedException(
                    "Your user account is inactive");
        }

        return account;
    }
}
