package com.workforce.attendance.dto;

import java.time.YearMonth;

public class MonthlyAttendanceReportDTO {

    private Long employeeId;
    private String employeeName;
    private YearMonth month;

    private long workingDays;
    private long presentDays;
    private long absentDays;
    private long weeklyOffs;
    private long holidays;

    private long lateArrivals;
    private long earlyDepartures;

    // Durations are returned in seconds.
    private long totalWorkingDurationSeconds;
    private long totalBreakDurationSeconds;

    private long pendingCorrectionRequests;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = month;
    }

    public long getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(long workingDays) {
        this.workingDays = workingDays;
    }

    public long getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(long presentDays) {
        this.presentDays = presentDays;
    }

    public long getAbsentDays() {
        return absentDays;
    }

    public void setAbsentDays(long absentDays) {
        this.absentDays = absentDays;
    }

    public long getWeeklyOffs() {
        return weeklyOffs;
    }

    public void setWeeklyOffs(long weeklyOffs) {
        this.weeklyOffs = weeklyOffs;
    }

    public long getHolidays() {
        return holidays;
    }

    public void setHolidays(long holidays) {
        this.holidays = holidays;
    }

    public long getLateArrivals() {
        return lateArrivals;
    }

    public void setLateArrivals(long lateArrivals) {
        this.lateArrivals = lateArrivals;
    }

    public long getEarlyDepartures() {
        return earlyDepartures;
    }

    public void setEarlyDepartures(long earlyDepartures) {
        this.earlyDepartures = earlyDepartures;
    }

    public long getTotalWorkingDurationSeconds() {
        return totalWorkingDurationSeconds;
    }

    public void setTotalWorkingDurationSeconds(
            long totalWorkingDurationSeconds) {
        this.totalWorkingDurationSeconds =
                totalWorkingDurationSeconds;
    }

    public long getTotalBreakDurationSeconds() {
        return totalBreakDurationSeconds;
    }

    public void setTotalBreakDurationSeconds(
            long totalBreakDurationSeconds) {
        this.totalBreakDurationSeconds =
                totalBreakDurationSeconds;
    }

    public long getPendingCorrectionRequests() {
        return pendingCorrectionRequests;
    }

    public void setPendingCorrectionRequests(
            long pendingCorrectionRequests) {
        this.pendingCorrectionRequests =
                pendingCorrectionRequests;
    }
}