package com.workforce.attendance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.workforce.attendance.dto.EmployeeRosterDTO;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.EmployeeRosterRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.EmployeeRosterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rosters")
public class EmployeeRosterController {

    private final EmployeeRosterService rosterService;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeRosterRepository rosterRepository;

    public EmployeeRosterController(
            EmployeeRosterService rosterService,
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository,
            EmployeeRosterRepository rosterRepository) {

        this.rosterService = rosterService;
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.rosterRepository = rosterRepository;
    }

    // ADMIN only: create a roster
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeRosterDTO> createRoster(
            @Valid @RequestBody EmployeeRosterDTO dto) {

        EmployeeRosterDTO createdRoster =
                rosterService.createRoster(dto);

        return new ResponseEntity<>(
                createdRoster, HttpStatus.CREATED);
    }

    // ADMIN only: view all rosters
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeRosterDTO>> getAllRosters() {

        return ResponseEntity.ok(
                rosterService.getAllRosters());
    }

    // ADMIN: any roster
    // MANAGER: own roster or direct report roster
    // EMPLOYEE: own roster only
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<EmployeeRosterDTO> getRosterById(
            @PathVariable Long id,
            Authentication authentication) {

        UserAccount account =
                getAuthenticatedAccount(authentication);

        if (hasRole(authentication, "ADMIN")) {
            return ResponseEntity.ok(
                    rosterService.getRosterById(id));
        }

        if (account.getEmployee() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No employee profile is linked to this account");
        }

        Long employeeId = account.getEmployee().getId();

        boolean isOwnRoster =
                rosterRepository.existsByIdAndEmployee_Id(
                        id, employeeId);

        if (isOwnRoster) {
            return ResponseEntity.ok(
                    rosterService.getRosterById(id));
        }

        if (hasRole(authentication, "MANAGER")) {

            boolean isDirectReportRoster =
                    rosterRepository.existsByIdAndEmployee_Manager_Id(
                            id, employeeId);

            if (isDirectReportRoster) {
                return ResponseEntity.ok(
                        rosterService.getRosterById(id));
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not authorized to view this roster");
    }

    // ADMIN only: update a roster
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeRosterDTO> updateRoster(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRosterDTO dto) {

        return ResponseEntity.ok(
                rosterService.updateRoster(id, dto));
    }

    // ADMIN: any employee
    // MANAGER: own roster or direct reports
    // EMPLOYEE: own roster only
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<EmployeeRosterDTO>> getEmployeeRoster(
            @PathVariable Long employeeId,
            Authentication authentication) {

        UserAccount account =
                getAuthenticatedAccount(authentication);

        if (hasRole(authentication, "ADMIN")) {
            return ResponseEntity.ok(
                    rosterService.getEmployeeRoster(employeeId));
        }

        if (account.getEmployee() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No employee profile is linked to this account");
        }

        Long loggedInEmployeeId =
                account.getEmployee().getId();

        // Employee or manager viewing their own roster
        if (loggedInEmployeeId.equals(employeeId)) {
            return ResponseEntity.ok(
                    rosterService.getEmployeeRoster(employeeId));
        }

        // Employee cannot view another employee's roster
        if (hasRole(authentication, "EMPLOYEE")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Employees can view only their own rosters");
        }

        // Manager can view only direct reports
        if (hasRole(authentication, "MANAGER")) {

            boolean isDirectReport =
                    employeeRepository.existsByIdAndManager_Id(
                            employeeId, loggedInEmployeeId);

            if (isDirectReport) {
                return ResponseEntity.ok(
                        rosterService.getEmployeeRoster(employeeId));
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not authorized to view this employee's roster");
    }

    // ADMIN only: create multiple rosters
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmployeeRosterDTO>> createBulkRoster(
            @Valid @RequestBody List<@Valid EmployeeRosterDTO> rosters) {

        List<EmployeeRosterDTO> createdRosters =
                rosterService.createBulkRoster(rosters);

        return new ResponseEntity<>(
                createdRosters, HttpStatus.CREATED);
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