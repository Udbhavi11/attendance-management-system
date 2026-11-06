package com.workforce.attendance.service;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import com.workforce.attendance.dto.GpsAnomalyReportDTO;
import com.workforce.attendance.entity.AnomalySeverity;

public interface GpsAnomalyReportService {

    GpsAnomalyReportDTO getGpsAnomalyReport(
            LocalDate startDate,
            LocalDate endDate,
            AnomalySeverity severity,
            Pageable pageable,
            Authentication authentication);
}