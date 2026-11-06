
package com.workforce.attendance.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.workforce.attendance.dto.FieldVisitDTO;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.FieldVisitRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.FieldVisitService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/field-visits")
public class FieldVisitController {

    private final FieldVisitService fieldVisitService;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final FieldVisitRepository fieldVisitRepository;

    public FieldVisitController(
            FieldVisitService fieldVisitService,
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository,
            FieldVisitRepository fieldVisitRepository) {

        this.fieldVisitService = fieldVisitService;
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.fieldVisitRepository = fieldVisitRepository;
    }

    // EMPLOYEE or MANAGER: Start own field visit
    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<FieldVisitDTO> startVisit(
            @Valid @RequestBody FieldVisitDTO dto,
            Authentication authentication) {

        UserAccount account =
                getAuthenticatedAccount(authentication);

        validateOwnEmployee(account, dto.getEmployeeId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fieldVisitService.startVisit(dto));
    }

    // EMPLOYEE or MANAGER: Complete own field visit
    @PutMapping("/{visitId}/complete")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<FieldVisitDTO> completeVisit(
            @PathVariable Long visitId,
            @Valid @RequestBody FieldVisitDTO dto,
            Authentication authentication) {

        UserAccount account =
                getAuthenticatedAccount(authentication);

        Long employeeId = getEmployeeId(account);

        boolean isOwnVisit =
                fieldVisitRepository.existsByIdAndEmployee_Id(
                        visitId, employeeId);

        if (!isOwnVisit) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can complete only your own field visits");
        }

        validateOwnEmployee(account, dto.getEmployeeId());

        return ResponseEntity.ok(
                fieldVisitService.completeVisit(visitId, dto));
    }

    // ADMIN or MANAGER: Correct a field visit
    @PutMapping("/{visitId}/correct")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<FieldVisitDTO> correctVisit(
            @PathVariable Long visitId,
            @Valid @RequestBody FieldVisitDTO dto,
            Authentication authentication,
            @RequestHeader(
                    value = "X-Correlation-ID",
                    required = false) String correlationId) {

        UserAccount account =
                getAuthenticatedAccount(authentication);

        if (hasRole(authentication, "MANAGER")) {

            Long managerId = getEmployeeId(account);

            boolean isDirectReportVisit =
                    fieldVisitRepository
                            .existsByIdAndEmployee_Manager_Id(
                                    visitId, managerId);

            if (!isDirectReportVisit) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can correct visits belonging only to your direct reports");
            }
        }

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        if (correlationId.length() > 100) {
            throw new IllegalArgumentException(
                    "X-Correlation-ID cannot exceed 100 characters");
        }

        return ResponseEntity.ok(
                fieldVisitService.correctVisit(
                        visitId,
                        dto,
                        authentication.getName(),
                        correlationId));
    }

    // ADMIN: Any visit with coordinates
    // AUDITOR: Any visit without exact coordinates
    // MANAGER: Own or direct report visit with coordinates
    // EMPLOYEE: Own visit with coordinates
    @GetMapping("/{visitId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<FieldVisitDTO> getVisitById(
            @PathVariable Long visitId,
            Authentication authentication) {

        if (hasRole(authentication, "ADMIN")) {
            return ResponseEntity.ok(
                    fieldVisitService.getVisitById(visitId));
        }

        if (hasRole(authentication, "AUDITOR")) {
            FieldVisitDTO dto =
                    fieldVisitService.getVisitById(visitId);

            return ResponseEntity.ok(
                    hideCoordinates(dto));
        }

        Long employeeId = getEmployeeId(
                getAuthenticatedAccount(authentication));

        boolean isOwnVisit =
                fieldVisitRepository.existsByIdAndEmployee_Id(
                        visitId, employeeId);

        if (isOwnVisit) {
            return ResponseEntity.ok(
                    fieldVisitService.getVisitById(visitId));
        }

        if (hasRole(authentication, "MANAGER")) {

            boolean isDirectReportVisit =
                    fieldVisitRepository
                            .existsByIdAndEmployee_Manager_Id(
                                    visitId, employeeId);

            if (isDirectReportVisit) {
                return ResponseEntity.ok(
                        fieldVisitService.getVisitById(visitId));
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not authorized to view this field visit");
    }

    // ADMIN: View all field visits with coordinates
    // AUDITOR: View all field visits without exact coordinates
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<List<FieldVisitDTO>> getAllVisits(
            Authentication authentication) {

        List<FieldVisitDTO> visits =
                fieldVisitService.getAllVisits();

        if (hasRole(authentication, "AUDITOR")) {
            visits.forEach(this::hideCoordinates);
        }

        return ResponseEntity.ok(visits);
    }

    // ADMIN/AUDITOR: Any employee
    // MANAGER: Own or direct report
    // EMPLOYEE: Own visits only
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<List<FieldVisitDTO>> getEmployeeVisits(
            @PathVariable Long employeeId,
            Authentication authentication) {

        if (hasRole(authentication, "ADMIN")) {
            return ResponseEntity.ok(
                    fieldVisitService.getEmployeeVisits(employeeId));
        }

        if (hasRole(authentication, "AUDITOR")) {
            List<FieldVisitDTO> visits =
                    fieldVisitService.getEmployeeVisits(employeeId);

            visits.forEach(this::hideCoordinates);

            return ResponseEntity.ok(visits);
        }

        Long loggedInEmployeeId = getEmployeeId(
                getAuthenticatedAccount(authentication));

        if (loggedInEmployeeId.equals(employeeId)) {
            return ResponseEntity.ok(
                    fieldVisitService.getEmployeeVisits(employeeId));
        }

        if (hasRole(authentication, "MANAGER")) {

            boolean isDirectReport =
                    employeeRepository.existsByIdAndManager_Id(
                            employeeId, loggedInEmployeeId);

            if (isDirectReport) {
                return ResponseEntity.ok(
                        fieldVisitService.getEmployeeVisits(employeeId));
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not authorized to view this employee's visits");
    }

    // ADMIN/AUDITOR: Any employee
    // MANAGER: Own or direct report
    // EMPLOYEE: Own daily visit history
    @GetMapping("/employee/{employeeId}/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<List<FieldVisitDTO>> getEmployeeDailyVisits(
            @PathVariable Long employeeId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication) {

        if (hasRole(authentication, "ADMIN")) {
            return ResponseEntity.ok(
                    fieldVisitService.getEmployeeDailyVisits(
                            employeeId, date));
        }

        if (hasRole(authentication, "AUDITOR")) {
            List<FieldVisitDTO> visits =
                    fieldVisitService.getEmployeeDailyVisits(
                            employeeId, date);

            visits.forEach(this::hideCoordinates);

            return ResponseEntity.ok(visits);
        }

        Long loggedInEmployeeId = getEmployeeId(
                getAuthenticatedAccount(authentication));

        if (loggedInEmployeeId.equals(employeeId)) {
            return ResponseEntity.ok(
                    fieldVisitService.getEmployeeDailyVisits(
                            employeeId, date));
        }

        if (hasRole(authentication, "MANAGER")) {

            boolean isDirectReport =
                    employeeRepository.existsByIdAndManager_Id(
                            employeeId, loggedInEmployeeId);

            if (isDirectReport) {
                return ResponseEntity.ok(
                        fieldVisitService.getEmployeeDailyVisits(
                                employeeId, date));
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not authorized to view this employee's daily visits");
    }

    // MANAGER: View daily visits of direct reports
    @GetMapping("/manager/{managerId}/team")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<FieldVisitDTO>> getTeamVisits(
            @PathVariable Long managerId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication) {

        UserAccount account =
                getAuthenticatedAccount(authentication);

        Long authenticatedManagerId = getEmployeeId(account);

        if (!authenticatedManagerId.equals(managerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to view another manager's team visits");
        }

        return ResponseEntity.ok(
                fieldVisitService.getTeamVisits(
                        authenticatedManagerId, date));
    }

    // Removes exact GPS coordinates from auditor responses.
    // Does not modify the database entity.
    private FieldVisitDTO hideCoordinates(FieldVisitDTO dto) {

        dto.setStartLatitude(null);
        dto.setStartLongitude(null);
        dto.setEndLatitude(null);
        dto.setEndLongitude(null);

        return dto;
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

    private Long getEmployeeId(UserAccount account) {

        if (account.getEmployee() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No employee profile is linked to this account");
        }

        return account.getEmployee().getId();
    }

    private void validateOwnEmployee(
            UserAccount account, Long requestedEmployeeId) {

        Long loggedInEmployeeId = getEmployeeId(account);

        if (requestedEmployeeId == null
                || !loggedInEmployeeId.equals(requestedEmployeeId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can perform this operation only for your own employee account");
        }
    }

    private boolean hasRole(
            Authentication authentication, String role) {

        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_" + role));
    }
}
