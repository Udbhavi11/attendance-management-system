package com.workforce.attendance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.workforce.attendance.dto.OfficeLocationDTO;
import com.workforce.attendance.service.OfficeLocationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/locations")
public class OfficeLocationController {

    private final OfficeLocationService officeLocationService;

    public OfficeLocationController(
            OfficeLocationService officeLocationService) {

        this.officeLocationService = officeLocationService;
    }

    // ADMIN only: Create office location
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfficeLocationDTO> createOfficeLocation(
            @Valid @RequestBody OfficeLocationDTO officeLocationDTO) {

        OfficeLocationDTO savedOffice =
                officeLocationService.createOfficeLocation(
                        officeLocationDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedOffice);
    }

    // All authenticated roles: View all office locations
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<List<OfficeLocationDTO>> getAllOfficeLocations() {

        return ResponseEntity.ok(
                officeLocationService.getAllOfficeLocations());
    }

    // All authenticated roles: View office location by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE', 'AUDITOR')")
    public ResponseEntity<OfficeLocationDTO> getOfficeLocationById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                officeLocationService.getOfficeLocationById(id));
    }

    // ADMIN only: Update office location
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfficeLocationDTO> updateOfficeLocation(
            @PathVariable Long id,
            @Valid @RequestBody OfficeLocationDTO officeLocationDTO) {

        return ResponseEntity.ok(
                officeLocationService.updateOfficeLocation(
                        id, officeLocationDTO));
    }
}