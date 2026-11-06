package com.workforce.attendance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.workforce.attendance.dto.ShiftDTO;
import com.workforce.attendance.service.ShiftService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    // ADMIN only: Create a shift
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShiftDTO> createShift(
            @Valid @RequestBody ShiftDTO dto) {

        ShiftDTO createdShift = shiftService.createShift(dto);

        return new ResponseEntity<>(
                createdShift, HttpStatus.CREATED);
    }

    // All authenticated roles: View all shifts
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<List<ShiftDTO>> getAllShifts() {

        return ResponseEntity.ok(
                shiftService.getAllShifts());
    }

    // All authenticated roles: View a shift by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<ShiftDTO> getShiftById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                shiftService.getShiftById(id));
    }

    // ADMIN only: Update a shift
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShiftDTO> updateShift(
            @PathVariable Long id,
            @Valid @RequestBody ShiftDTO dto) {

        return ResponseEntity.ok(
                shiftService.updateShift(id, dto));
    }

    // ADMIN only: Deactivate a shift
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShiftDTO> deactivateShift(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                shiftService.deactivateShift(id));
    }

    // ADMIN only: Activate a shift
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShiftDTO> activateShift(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                shiftService.activateShift(id));
    }
}