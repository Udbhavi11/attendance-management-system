package com.workforce.attendance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.workforce.attendance.dto.OfflineAttendanceDTO;
import com.workforce.attendance.service.OfflineAttendanceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/offline-attendance")
public class OfflineAttendanceController {

    private final OfflineAttendanceService offlineAttendanceService;

    public OfflineAttendanceController(
            OfflineAttendanceService offlineAttendanceService) {
        this.offlineAttendanceService = offlineAttendanceService;
    }

    @PostMapping("/sync")
    public ResponseEntity<OfflineAttendanceDTO> syncAttendance(
            @Valid @RequestBody OfflineAttendanceDTO dto,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        OfflineAttendanceDTO response =
                offlineAttendanceService.processOfflineAttendance(
                        dto, idempotencyKey);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}