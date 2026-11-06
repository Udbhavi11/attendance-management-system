
package com.workforce.attendance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.workforce.attendance.dto.CorrectionRequestDTO;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.CorrectionRequestService;

@RestController
@RequestMapping("/api/corrections")
public class CorrectionRequestController {

    private final CorrectionRequestService correctionRequestService;
    private final UserAccountRepository userAccountRepository;

    public CorrectionRequestController(
            CorrectionRequestService correctionRequestService,
            UserAccountRepository userAccountRepository) {

        this.correctionRequestService = correctionRequestService;
        this.userAccountRepository = userAccountRepository;
    }

    // Employee or manager submits their own correction request.
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<CorrectionRequestDTO> submitCorrection(
            @Valid @RequestBody CorrectionRequestDTO dto,
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);

        validateOwnEmployee(dto.getEmployeeId(), account);

        CorrectionRequestDTO response =
                correctionRequestService.submitCorrection(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Employee or manager views only their own correction requests.
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<List<CorrectionRequestDTO>> getEmployeeRequests(
            @PathVariable Long employeeId,
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);

        validateOwnEmployee(employeeId, account);

        return ResponseEntity.ok(
                correctionRequestService.getEmployeeRequests(employeeId));
    }

    // Manager views pending correction requests from direct reports.
    @GetMapping("/manager/{managerId}/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<CorrectionRequestDTO>> getPendingRequests(
            @PathVariable Long managerId,
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);

        validateOwnEmployee(managerId, account);

        return ResponseEntity.ok(
                correctionRequestService.getPendingRequestsForManager(
                        managerId));
    }

    // Manager approves a correction request from a direct report.
    @PutMapping("/{requestId}/approve/{managerId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<CorrectionRequestDTO> approveCorrection(
            @PathVariable Long requestId,
            @PathVariable Long managerId,
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);

        validateOwnEmployee(managerId, account);

        return ResponseEntity.ok(
                correctionRequestService.approveCorrection(
                        requestId, managerId));
    }

    // Manager rejects a correction request from a direct report.
    @PutMapping("/{requestId}/reject/{managerId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<CorrectionRequestDTO> rejectCorrection(
            @PathVariable Long requestId,
            @PathVariable Long managerId,
            Authentication authentication) {

        UserAccount account = getAuthenticatedAccount(authentication);

        validateOwnEmployee(managerId, account);

        return ResponseEntity.ok(
                correctionRequestService.rejectCorrection(
                        requestId, managerId));
    }

    // Verify that the requested employee ID belongs to the logged-in user.
    private void validateOwnEmployee(
            Long requestedEmployeeId,
            UserAccount account) {

        Employee employee = account.getEmployee();

        if (employee == null) {
            throw new AccessDeniedException(
                    "Your account is not linked to an employee");
        }

        if (!employee.getId().equals(requestedEmployeeId)) {
            throw new AccessDeniedException(
                    "You are not authorized to access this employee's correction requests");
        }
    }

    // Retrieve the authenticated active user account.
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
