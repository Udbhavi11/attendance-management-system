package com.workforce.attendance.service.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.AnomalyFlagDTO;
import com.workforce.attendance.dto.GpsAnomalyReportDTO;
import com.workforce.attendance.entity.AnomalyFlag;
import com.workforce.attendance.entity.AnomalySeverity;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.AnomalyFlagRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.GpsAnomalyReportService;

@Service
@Transactional(readOnly = true)
public class GpsAnomalyReportServiceImpl
        implements GpsAnomalyReportService {

    private static final ZoneId APP_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final List<String> OUTSIDE_GEOFENCE_RULES =
            List.of("OUTSIDE_GEOFENCE");

    private static final List<String> POOR_ACCURACY_RULES =
            List.of("POOR_LOCATION_ACCURACY");

    private static final List<String> DELAYED_OFFLINE_RULES =
            List.of("DELAYED_OFFLINE_SUBMISSION");

    private static final List<String> DUPLICATE_REQUEST_RULES =
            List.of("DUPLICATE_CHECK_IN");

    private final AnomalyFlagRepository anomalyFlagRepository;
    private final UserAccountRepository userAccountRepository;

    public GpsAnomalyReportServiceImpl(
            AnomalyFlagRepository anomalyFlagRepository,
            UserAccountRepository userAccountRepository) {

        this.anomalyFlagRepository = anomalyFlagRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public GpsAnomalyReportDTO getGpsAnomalyReport(
            LocalDate startDate,
            LocalDate endDate,
            AnomalySeverity severity,
            Pageable pageable,
            Authentication authentication) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date");
        }

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication is required");
        }

        boolean isAdmin = hasRole(authentication, "ADMIN");
        boolean isManager = hasRole(authentication, "MANAGER");
        boolean isAuditor = hasRole(authentication, "AUDITOR");

        if (!isAdmin && !isManager && !isAuditor) {
            throw new AccessDeniedException(
                    "You are not allowed to view GPS anomaly reports");
        }

        Long managerId = null;

        if (isManager) {

            UserAccount account = userAccountRepository
                    .findAccountWithEmployeeAndRoleByUsername(
                            authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Manager account not found"));

            Employee manager = account.getEmployee();

            if (manager == null) {
                throw new AccessDeniedException(
                        "Manager employee profile is not linked");
            }

            managerId = manager.getId();
        }

        Instant startTime = startDate
                .atStartOfDay(APP_ZONE)
                .toInstant();

        Instant endTime = endDate
                .plusDays(1)
                .atStartOfDay(APP_ZONE)
                .toInstant();

        long outsideGeofence =
                anomalyFlagRepository.countByRulesForReport(
                        startTime,
                        endTime,
                        managerId,
                        OUTSIDE_GEOFENCE_RULES);

        long poorAccuracy =
                anomalyFlagRepository.countByRulesForReport(
                        startTime,
                        endTime,
                        managerId,
                        POOR_ACCURACY_RULES);

        long delayedOffline =
                anomalyFlagRepository.countByRulesForReport(
                        startTime,
                        endTime,
                        managerId,
                        DELAYED_OFFLINE_RULES);

        long duplicateRequests =
                anomalyFlagRepository.countByRulesForReport(
                        startTime,
                        endTime,
                        managerId,
                        DUPLICATE_REQUEST_RULES);

        long awaitingReview =
                anomalyFlagRepository.countPendingForReport(
                        startTime,
                        endTime,
                        managerId);

        Page<AnomalyFlag> anomalyPage =
                anomalyFlagRepository.findReportAnomalies(
                        startTime,
                        endTime,
                        managerId,
                        severity,
                        pageable);

        List<AnomalyFlagDTO> anomalyDTOs =
                anomalyPage.getContent()
                        .stream()
                        .map(this::convertToDTO)
                        .toList();

        GpsAnomalyReportDTO response =
                new GpsAnomalyReportDTO();

        response.setStartDate(startDate);
        response.setEndDate(endDate);

        response.setOutsideGeofenceEvents(outsideGeofence);
        response.setPoorAccuracyEvents(poorAccuracy);
        response.setDelayedOfflineEvents(delayedOffline);
        response.setDuplicateRequestAttempts(duplicateRequests);
        response.setEventsAwaitingReview(awaitingReview);

        response.setAnomalies(anomalyDTOs);

        response.setCurrentPage(anomalyPage.getNumber());
        response.setPageSize(anomalyPage.getSize());
        response.setTotalElements(anomalyPage.getTotalElements());
        response.setTotalPages(anomalyPage.getTotalPages());

        return response;
    }

    private boolean hasRole(
            Authentication authentication,
            String role) {

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_" + role));
    }

    private AnomalyFlagDTO convertToDTO(AnomalyFlag anomaly) {

        AnomalyFlagDTO dto = new AnomalyFlagDTO();

        dto.setId(anomaly.getId());
        dto.setEmployeeId(anomaly.getEmployee().getId());
        dto.setEmployeeName(anomaly.getEmployee().getName());

        if (anomaly.getAttendanceEvent() != null) {
            dto.setAttendanceEventId(
                    anomaly.getAttendanceEvent().getId());
        }

        if (anomaly.getBreakRecord() != null) {
            dto.setBreakRecordId(
                    anomaly.getBreakRecord().getId());
        }

        dto.setRuleIdentifier(anomaly.getRuleIdentifier());
        dto.setDetectedAt(anomaly.getDetectedAt());
        dto.setSeverity(anomaly.getSeverity());
        dto.setReviewStatus(anomaly.getReviewStatus());

        if (anomaly.getReviewedBy() != null) {
            dto.setReviewedBy(anomaly.getReviewedBy().getId());
        }

        dto.setReviewedAt(anomaly.getReviewedAt());
        dto.setReviewOutcome(anomaly.getReviewOutcome());

        return dto;
    }
}