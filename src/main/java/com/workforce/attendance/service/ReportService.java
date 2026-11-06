package com.workforce.attendance.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import com.workforce.attendance.dto.DailyAttendanceReportDTO;
import com.workforce.attendance.dto.MonthlyAttendanceReportDTO;
import com.workforce.attendance.dto.FieldVisitSummaryReportDTO;

public interface ReportService {

    DailyAttendanceReportDTO getDailyAttendanceReport(
            LocalDate reportDate,
            Pageable pageable,
            Authentication authentication
    );

    MonthlyAttendanceReportDTO getMonthlyAttendanceReport(
            Long employeeId,
            YearMonth month,
            Authentication authentication
    );

    FieldVisitSummaryReportDTO getFieldVisitSummaryReport(
            LocalDate startDate,
            LocalDate endDate,
            Long employeeId,
            Authentication authentication
    );
}