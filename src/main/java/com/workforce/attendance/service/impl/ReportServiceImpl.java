package com.workforce.attendance.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.workforce.attendance.dto.DailyAttendanceEmployeeDTO;
import com.workforce.attendance.dto.DailyAttendanceReportDTO;
import com.workforce.attendance.dto.FieldVisitSummaryReportDTO;
import com.workforce.attendance.dto.FieldVisitTypeBreakdownDTO;
import com.workforce.attendance.dto.MonthlyAttendanceReportDTO;
import com.workforce.attendance.entity.AttendanceStatus;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.UserAccount;

import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.repository.DailyAttendanceProjection;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.FieldVisitRepository;
import com.workforce.attendance.repository.FieldVisitTypeProjection;
import com.workforce.attendance.repository.ReportRepository;

import com.workforce.attendance.service.ReportService;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final ZoneId APP_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final ReportRepository reportRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final FieldVisitRepository fieldVisitRepository;

    public ReportServiceImpl(
            ReportRepository reportRepository,
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository,
            FieldVisitRepository fieldVisitRepository) {

        this.reportRepository = reportRepository;
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.fieldVisitRepository = fieldVisitRepository;
    }

    @Override
    public DailyAttendanceReportDTO getDailyAttendanceReport(
            LocalDate reportDate,
            Pageable pageable,
            Authentication authentication) {

        if (reportDate == null) {
            throw new IllegalArgumentException(
                    "Report date is required");
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
                    "You are not allowed to view attendance reports");
        }

        Long managerId = null;

        if (isManager) {
            UserAccount account = userAccountRepository
                    .findAccountWithEmployeeAndRoleByUsername(
                            authentication.getName())
                    .orElseThrow(() ->
                            new AccessDeniedException(
                                    "Manager account not found"));

            Employee manager = account.getEmployee();

            if (manager == null) {
                throw new AccessDeniedException(
                        "Manager employee profile is not linked");
            }

            managerId = manager.getId();
        }

        Page<DailyAttendanceProjection> attendancePage =
                reportRepository.findDailyAttendance(
                        reportDate,
                        managerId,
                        pageable);

        long expected = reportRepository.countExpectedEmployees(
                reportDate, managerId);

        long checkedIn =
                reportRepository.countEmployeesByAttendanceStatus(
                        reportDate,
                        managerId,
                        AttendanceStatus.CHECKED_IN);

        long onBreak =
                reportRepository.countEmployeesByAttendanceStatus(
                        reportDate,
                        managerId,
                        AttendanceStatus.ON_BREAK);

        long checkedOut =
                reportRepository.countEmployeesByAttendanceStatus(
                        reportDate,
                        managerId,
                        AttendanceStatus.CHECKED_OUT);

        long present = reportRepository.countPresentEmployees(
                reportDate, managerId);

        long absent = Math.max(0, expected - present);

        long lateArrivals = reportRepository.countLateArrivals(
                reportDate, managerId);

        List<DailyAttendanceEmployeeDTO> employees =
                attendancePage.getContent()
                        .stream()
                        .map(this::convertToDTO)
                        .toList();

        DailyAttendanceReportDTO response =
                new DailyAttendanceReportDTO();

        response.setReportDate(reportDate);
        response.setExpectedEmployees(expected);
        response.setCheckedInEmployees(checkedIn);
        response.setAbsentEmployees(absent);
        response.setLateArrivals(lateArrivals);
        response.setEmployeesOnBreak(onBreak);
        response.setCheckedOutEmployees(checkedOut);

        response.setEmployees(employees);

        response.setCurrentPage(attendancePage.getNumber());
        response.setPageSize(attendancePage.getSize());
        response.setTotalElements(attendancePage.getTotalElements());
        response.setTotalPages(attendancePage.getTotalPages());

        return response;
    }

    private boolean hasRole(
            Authentication authentication,
            String role) {

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_" + role));
    }

    private DailyAttendanceEmployeeDTO convertToDTO(
            DailyAttendanceProjection projection) {

        DailyAttendanceEmployeeDTO dto =
                new DailyAttendanceEmployeeDTO();

        dto.setEmployeeId(projection.getEmployeeId());
        dto.setEmployeeCode(projection.getEmployeeCode());
        dto.setEmployeeName(projection.getEmployeeName());
        dto.setDepartment(projection.getDepartment());

        dto.setRosterId(projection.getRosterId());
        dto.setRosterDate(projection.getRosterDate());

        dto.setAttendanceSessionId(
                projection.getAttendanceSessionId());

        dto.setAttendanceStatus(
                projection.getAttendanceStatus());

        dto.setCheckInTime(projection.getCheckInTime());
        dto.setCheckOutTime(projection.getCheckOutTime());

        dto.setLateArrivalMinutes(
                calculateLateArrivalMinutes(projection));

        return dto;
    }

    private long calculateLateArrivalMinutes(
            DailyAttendanceProjection projection) {

        if (projection.getAttendanceSessionId() == null
                || projection.getCheckInTime() == null
                || projection.getShiftStartTime() == null) {

            return 0;
        }

        int graceMinutes =
                projection.getLateArrivalGraceMinutes() == null
                        ? 0
                        : projection.getLateArrivalGraceMinutes();

        LocalDateTime scheduledStart =
                LocalDateTime.of(
                        projection.getRosterDate(),
                        projection.getShiftStartTime());

        Instant graceDeadline = scheduledStart
                .atZone(APP_ZONE)
                .plusMinutes(graceMinutes)
                .toInstant();

        if (!projection.getCheckInTime().isAfter(graceDeadline)) {
            return 0;
        }

        return Duration.between(
                graceDeadline,
                projection.getCheckInTime())
                .toMinutes();
    }

    @Override
    public MonthlyAttendanceReportDTO getMonthlyAttendanceReport(
            Long employeeId,
            YearMonth month,
            Authentication authentication) {

        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException(
                    "Valid employee ID is required");
        }

        if (month == null) {
            throw new IllegalArgumentException(
                    "Report month is required");
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
                    "You are not allowed to view attendance reports");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found"));

        // Managers can view only their directly assigned employees.
        if (isManager) {

            UserAccount account = userAccountRepository
                    .findAccountWithEmployeeAndRoleByUsername(
                            authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException(
                            "Manager account not found"));

            Employee manager = account.getEmployee();

            if (manager == null
                    || employee.getManager() == null
                    || !employee.getManager().getId()
                            .equals(manager.getId())) {

                throw new AccessDeniedException(
                        "You are not allowed to view this employee's report");
            }
        }

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.plusMonths(1).atDay(1);

        long workingDays =
                reportRepository.countMonthlyWorkingDays(
                        employeeId, startDate, endDate);

        long presentDays =
                reportRepository.countMonthlyPresentDays(
                        employeeId, startDate, endDate);

        long weeklyOffs =
                reportRepository.countMonthlyWeeklyOffs(
                        employeeId, startDate, endDate);

        long holidays =
                reportRepository.countMonthlyHolidays(
                        startDate, endDate);

        long absentDays =
                Math.max(0, workingDays - presentDays);

        long lateArrivals =
                reportRepository.countMonthlyLateArrivals(
                        employeeId, startDate, endDate);

        long earlyDepartures =
                reportRepository.countMonthlyEarlyDepartures(
                        employeeId, startDate, endDate);

        long workingDuration =
                reportRepository.calculateMonthlyWorkingDuration(
                        employeeId, startDate, endDate);

        long breakDuration =
                reportRepository.calculateMonthlyBreakDuration(
                        employeeId, startDate, endDate);

        long pendingCorrections =
                reportRepository.countMonthlyPendingCorrections(
                        employeeId, startDate, endDate);

        MonthlyAttendanceReportDTO response =
                new MonthlyAttendanceReportDTO();

        response.setEmployeeId(employee.getId());
        response.setEmployeeName(employee.getName());
        response.setMonth(month);

        response.setWorkingDays(workingDays);
        response.setPresentDays(presentDays);
        response.setAbsentDays(absentDays);
        response.setWeeklyOffs(weeklyOffs);
        response.setHolidays(holidays);

        response.setLateArrivals(lateArrivals);
        response.setEarlyDepartures(earlyDepartures);

        response.setTotalWorkingDurationSeconds(
                workingDuration);

        response.setTotalBreakDurationSeconds(
                breakDuration);

        response.setPendingCorrectionRequests(
                pendingCorrections);

        return response;
    }
    
    @Override
    public FieldVisitSummaryReportDTO getFieldVisitSummaryReport(
            LocalDate startDate,
            LocalDate endDate,
            Long employeeId,
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
                    "You are not allowed to view field visit reports");
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

            // If a specific employee is requested, verify ownership.
            if (employeeId != null) {

                Employee employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Employee not found"));

                if (employee.getManager() == null
                        || !employee.getManager().getId().equals(managerId)) {

                    throw new AccessDeniedException(
                            "You are not allowed to view this employee's visits");
                }
            }
        }

        if (employeeId != null && employeeId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid employee ID");
        }

        Instant startTime = startDate
                .atStartOfDay(APP_ZONE)
                .toInstant();

        Instant endTime = endDate
                .plusDays(1)
                .atStartOfDay(APP_ZONE)
                .toInstant();

        long totalVisits =
                fieldVisitRepository.countReportVisits(
                        startTime, endTime, managerId, employeeId);

        long completedVisits =
                fieldVisitRepository.countCompletedReportVisits(
                        startTime, endTime, managerId, employeeId);

        long incompleteVisits =
                fieldVisitRepository.countIncompleteReportVisits(
                        startTime, endTime, managerId, employeeId);

        long totalDuration =
                fieldVisitRepository.calculateTotalVisitDuration(
                        startTime, endTime, managerId, employeeId);

        List<FieldVisitTypeProjection> typeResults =
                fieldVisitRepository.getVisitTypeBreakdown(
                        startTime, endTime, managerId, employeeId);

        List<FieldVisitTypeBreakdownDTO> breakdown =
                typeResults.stream()
                        .map(result -> new FieldVisitTypeBreakdownDTO(
                                result.getVisitType(),
                                result.getVisitCount()))
                        .toList();

        FieldVisitSummaryReportDTO response =
                new FieldVisitSummaryReportDTO();

        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setEmployeeId(employeeId);

        response.setTotalVisits(totalVisits);
        response.setCompletedVisits(completedVisits);
        response.setIncompleteVisits(incompleteVisits);

        response.setTotalVisitDurationSeconds(totalDuration);
        response.setVisitTypeBreakdown(breakdown);

        return response;
    }
    
}