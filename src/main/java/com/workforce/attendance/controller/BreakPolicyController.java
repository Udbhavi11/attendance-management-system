package com.workforce.attendance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.workforce.attendance.dto.BreakPolicyDTO;
import com.workforce.attendance.service.BreakPolicyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/break-policies")
public class BreakPolicyController {

    private final BreakPolicyService breakPolicyService;

    public BreakPolicyController(
            BreakPolicyService breakPolicyService) {

        this.breakPolicyService = breakPolicyService;
    }

    // ADMIN only: Create break policy
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BreakPolicyDTO> createPolicy(
            @Valid @RequestBody BreakPolicyDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(breakPolicyService.createPolicy(dto));
    }

    // All authenticated roles: View all policies
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<List<BreakPolicyDTO>> getAllPolicies() {

        return ResponseEntity.ok(
                breakPolicyService.getAllPolicies());
    }

    // All authenticated roles: View policy by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<BreakPolicyDTO> getPolicyById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                breakPolicyService.getPolicyById(id));
    }

    // ADMIN only: Update policy
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BreakPolicyDTO> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody BreakPolicyDTO dto) {

        return ResponseEntity.ok(
                breakPolicyService.updatePolicy(id, dto));
    }

    // ADMIN only: Deactivate policy
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BreakPolicyDTO> deactivatePolicy(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                breakPolicyService.deactivatePolicy(id));
    }

    // All authenticated roles: View active policy
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<BreakPolicyDTO> getActivePolicy() {

        return ResponseEntity.ok(
                breakPolicyService.getActivePolicy());
    }
}