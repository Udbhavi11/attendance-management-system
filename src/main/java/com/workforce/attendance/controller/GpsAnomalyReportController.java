package com.workforce.attendance.controller;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workforce.attendance.dto.GpsAnomalyReportDTO;
import com.workforce.attendance.entity.AnomalySeverity;
import com.workforce.attendance.service.GpsAnomalyReportService;

@RestController
@RequestMapping("/api/reports")
public class GpsAnomalyReportController {

    private final GpsAnomalyReportService gpsAnomalyReportService;

    public GpsAnomalyReportController(
            GpsAnomalyReportService gpsAnomalyReportService) {
        this.gpsAnomalyReportService = gpsAnomalyReportService;
    }

    @GetMapping("/gps-anomalies")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    public ResponseEntity<GpsAnomalyReportDTO> getGpsAnomalyReport(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            AnomalySeverity severity,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            Authentication authentication) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size);

        GpsAnomalyReportDTO report =
                gpsAnomalyReportService.getGpsAnomalyReport(
                        startDate,
                        endDate,
                        severity,
                        pageable,
                        authentication);

        return ResponseEntity.ok(report);
    }
}