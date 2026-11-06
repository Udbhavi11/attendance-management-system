package com.workforce.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public class FieldVisitSummaryReportDTO {

    private LocalDate startDate;
    private LocalDate endDate;

    private Long employeeId;

    private long totalVisits;
    private long completedVisits;
    private long incompleteVisits;

    private long totalVisitDurationSeconds;

    private List<FieldVisitTypeBreakdownDTO> visitTypeBreakdown;

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public long getTotalVisits() {
        return totalVisits;
    }

    public void setTotalVisits(long totalVisits) {
        this.totalVisits = totalVisits;
    }

    public long getCompletedVisits() {
        return completedVisits;
    }

    public void setCompletedVisits(long completedVisits) {
        this.completedVisits = completedVisits;
    }

    public long getIncompleteVisits() {
        return incompleteVisits;
    }

    public void setIncompleteVisits(long incompleteVisits) {
        this.incompleteVisits = incompleteVisits;
    }

    public long getTotalVisitDurationSeconds() {
        return totalVisitDurationSeconds;
    }

    public void setTotalVisitDurationSeconds(
            long totalVisitDurationSeconds) {
        this.totalVisitDurationSeconds = totalVisitDurationSeconds;
    }

    public List<FieldVisitTypeBreakdownDTO> getVisitTypeBreakdown() {
        return visitTypeBreakdown;
    }

    public void setVisitTypeBreakdown(
            List<FieldVisitTypeBreakdownDTO> visitTypeBreakdown) {
        this.visitTypeBreakdown = visitTypeBreakdown;
    }
}