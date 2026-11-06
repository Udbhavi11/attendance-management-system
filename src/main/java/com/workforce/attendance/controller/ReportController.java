package com.workforce.attendance.controller;

import java.time.LocalDate;
import java.time.YearMonth;

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

import com.workforce.attendance.dto.DailyAttendanceReportDTO;
import com.workforce.attendance.dto.MonthlyAttendanceReportDTO;
import com.workforce.attendance.dto.FieldVisitSummaryReportDTO;
import com.workforce.attendance.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    public ResponseEntity<DailyAttendanceReportDTO>
            getDailyAttendanceReport(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

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

        DailyAttendanceReportDTO report =
                reportService.getDailyAttendanceReport(
                        date, pageable, authentication);

        return ResponseEntity.ok(report);
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    public ResponseEntity<MonthlyAttendanceReportDTO>
            getMonthlyAttendanceReport(

            @RequestParam Long employeeId,

            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth month,

            Authentication authentication) {

        MonthlyAttendanceReportDTO report =
                reportService.getMonthlyAttendanceReport(
                        employeeId, month, authentication);

        return ResponseEntity.ok(report);
    }

    @GetMapping("/field-visits")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    public ResponseEntity<FieldVisitSummaryReportDTO>
            getFieldVisitSummaryReport(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            Long employeeId,

            Authentication authentication) {

        FieldVisitSummaryReportDTO report =
                reportService.getFieldVisitSummaryReport(
                        startDate,
                        endDate,
                        employeeId,
                        authentication);

        return ResponseEntity.ok(report);
    }
}