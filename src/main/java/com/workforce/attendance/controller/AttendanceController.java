
package com.workforce.attendance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.workforce.attendance.dto.AttendanceCheckInDTO;
import com.workforce.attendance.dto.AttendanceCheckOutDTO;
import com.workforce.attendance.dto.AttendanceResponseDTO;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.AttendanceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceController(
            AttendanceService attendanceService,
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository) {

        this.attendanceService = attendanceService;
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public AttendanceResponseDTO checkIn(
            @Valid @RequestBody AttendanceCheckInDTO dto,
            Authentication authentication) {

        validateOwnAttendance(dto.getEmployeeId(), authentication);

        return attendanceService.checkIn(dto);
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public AttendanceResponseDTO checkOut(
            @Valid @RequestBody AttendanceCheckOutDTO dto,
            Authentication authentication) {

        validateOwnAttendance(dto.getEmployeeId(), authentication);

        return attendanceService.checkOut(dto);
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public List<AttendanceResponseDTO> getAttendanceHistory(
            @PathVariable Long employeeId,
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);
        String role = account.getRole().getName();

        if ("ADMIN".equals(role) || "AUDITOR".equals(role)) {
            return attendanceService.getAttendanceHistory(employeeId);
        }

        Employee loggedInEmployee = account.getEmployee();

        if (loggedInEmployee == null) {
            throw new AccessDeniedException(
                    "Your account is not linked to an employee");
        }

        Long loggedInEmployeeId = loggedInEmployee.getId();

        // Employees and managers can view their own attendance history.
        if (loggedInEmployeeId.equals(employeeId)) {
            return attendanceService.getAttendanceHistory(employeeId);
        }

        // Managers can view only their direct reports.
        if ("MANAGER".equals(role)
                && employeeRepository.existsByIdAndManager_Id(
                        employeeId, loggedInEmployeeId)) {

            return attendanceService.getAttendanceHistory(employeeId);
        }

        throw new AccessDeniedException(
                "You are not authorized to view this employee's attendance history");
    }

    private void validateOwnAttendance(
            Long requestedEmployeeId,
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);

        Employee loggedInEmployee = account.getEmployee();

        if (loggedInEmployee == null) {
            throw new AccessDeniedException(
                    "Your account is not linked to an employee");
        }

        if (!loggedInEmployee.getId().equals(requestedEmployeeId)) {
            throw new AccessDeniedException(
                    "You can perform attendance operations only for yourself");
        }
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
