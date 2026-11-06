
package com.workforce.attendance.dto;

import java.time.Instant;

import com.workforce.attendance.entity.AnomalySeverity;
import com.workforce.attendance.entity.AnomalyReviewStatus;

public class AnomalyFlagDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;

    private Long attendanceEventId;
    private Long breakRecordId;

    private String ruleIdentifier;
    private Instant detectedAt;

    private AnomalySeverity severity;
    private AnomalyReviewStatus reviewStatus;

    private Long reviewedBy;
    private Instant reviewedAt;
    private String reviewOutcome;

    public AnomalyFlagDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getAttendanceEventId() {
        return attendanceEventId;
    }

    public void setAttendanceEventId(Long attendanceEventId) {
        this.attendanceEventId = attendanceEventId;
    }

    public Long getBreakRecordId() {
        return breakRecordId;
    }

    public void setBreakRecordId(Long breakRecordId) {
        this.breakRecordId = breakRecordId;
    }

    public String getRuleIdentifier() {
        return ruleIdentifier;
    }

    public void setRuleIdentifier(String ruleIdentifier) {
        this.ruleIdentifier = ruleIdentifier;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public AnomalySeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AnomalySeverity severity) {
        this.severity = severity;
    }

    public AnomalyReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(AnomalyReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewOutcome() {
        return reviewOutcome;
    }

    public void setReviewOutcome(String reviewOutcome) {
        this.reviewOutcome = reviewOutcome;
    }
}
