package com.workforce.attendance.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.workforce.attendance.dto.AnomalyFlagDTO;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.AnomalyFlagService;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyFlagController {

    private final AnomalyFlagService anomalyFlagService;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;

    public AnomalyFlagController(
            AnomalyFlagService anomalyFlagService,
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository) {

        this.anomalyFlagService = anomalyFlagService;
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
    }

    // ADMIN and AUDITOR: View organization-wide anomalies
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<List<AnomalyFlagDTO>> getAllAnomalies() {

        return ResponseEntity.ok(
                anomalyFlagService.getAllAnomalies());
    }

    // ADMIN/AUDITOR: Any employee
    // MANAGER: Direct reports only
    // EMPLOYEE: Own anomalies only
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<AnomalyFlagDTO>> getEmployeeAnomalies(
            @PathVariable Long employeeId,
            Authentication authentication) {

        UserAccount account =
                getAuthenticatedAccount(authentication);

        // Admin and auditor can view any employee's anomalies.
        if (hasRole(authentication, "ADMIN")
                || hasRole(authentication, "AUDITOR")) {

            return ResponseEntity.ok(
                    anomalyFlagService.getEmployeeAnomalies(employeeId));
        }

        if (account.getEmployee() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No employee profile is linked to this account");
        }

        Long loggedInEmployeeId =
                account.getEmployee().getId();

        // Employee can view only their own anomalies.
        if (hasRole(authentication, "EMPLOYEE")) {

            if (!loggedInEmployeeId.equals(employeeId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Employees can view only their own anomalies");
            }

            return ResponseEntity.ok(
                    anomalyFlagService.getEmployeeAnomalies(employeeId));
        }

        // Manager can view only direct reports' anomalies.
        if (hasRole(authentication, "MANAGER")) {

            if (!employeeRepository.existsById(employeeId)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Employee not found");
            }

            boolean isDirectReport =
                    employeeRepository.existsByIdAndManager_Id(
                            employeeId, loggedInEmployeeId);

            if (!isDirectReport) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Managers can view anomalies only for their direct reports");
            }

            return ResponseEntity.ok(
                    anomalyFlagService.getEmployeeAnomalies(employeeId));
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not authorized to view anomaly records");
    }

    // MANAGER: View pending anomalies belonging to direct reports.
    @GetMapping("/manager/{managerId}/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<AnomalyFlagDTO>> getPendingAnomalies(
            @PathVariable Long managerId,
            Authentication authentication) {

        UserAccount account =
                getAuthenticatedAccount(authentication);

        if (account.getEmployee() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No employee profile is linked to this account");
        }

        Long authenticatedManagerId =
                account.getEmployee().getId();

        if (!authenticatedManagerId.equals(managerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to view another manager's anomalies");
        }

        return ResponseEntity.ok(
                anomalyFlagService.getPendingAnomaliesForManager(
                        authenticatedManagerId));
    }

    // MANAGER: Review anomalies belonging to direct reports only.
    @PutMapping("/{anomalyId}/review/{managerId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<AnomalyFlagDTO> reviewAnomaly(
            @PathVariable Long anomalyId,
            @PathVariable Long managerId,
            @RequestParam String reviewOutcome,
            Authentication authentication,
            @RequestHeader(
                    value = "X-Correlation-ID",
                    required = false) String correlationId) {

        UserAccount account =
                getAuthenticatedAccount(authentication);

        if (account.getEmployee() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No employee profile is linked to this account");
        }

        Long authenticatedManagerId =
                account.getEmployee().getId();

        if (!authenticatedManagerId.equals(managerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to review anomalies as another manager");
        }

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        if (correlationId.length() > 100) {
            throw new IllegalArgumentException(
                    "X-Correlation-ID cannot exceed 100 characters");
        }

        return ResponseEntity.ok(
                anomalyFlagService.reviewAnomaly(
                        anomalyId,
                        authenticatedManagerId,
                        reviewOutcome,
                        correlationId));
    }

    private UserAccount getAuthenticatedAccount(
            Authentication authentication) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required");
        }

        return userAccountRepository
                .findAccountWithEmployeeAndRoleByUsername(
                        authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Active user account not found"));
    }

    private boolean hasRole(
            Authentication authentication,
            String role) {

        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_" + role));
    }
}