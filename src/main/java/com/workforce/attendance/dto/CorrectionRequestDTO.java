
package com.workforce.attendance.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CorrectionRequestDTO {

    private Long id;

    @NotNull(message = "Attendance session ID is required")
    private Long attendanceSessionId;

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotBlank(message = "Correction reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    private Instant originalCheckInTime;
    private Instant originalCheckOutTime;

    private Instant requestedCheckInTime;
    private Instant requestedCheckOutTime;

    private Instant requestedTime;

    private String status;

    private Long reviewedBy;
    private Instant reviewedAt;

    public CorrectionRequestDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttendanceSessionId() {
        return attendanceSessionId;
    }

    public void setAttendanceSessionId(Long attendanceSessionId) {
        this.attendanceSessionId = attendanceSessionId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getOriginalCheckInTime() {
        return originalCheckInTime;
    }

    public void setOriginalCheckInTime(Instant originalCheckInTime) {
        this.originalCheckInTime = originalCheckInTime;
    }

    public Instant getOriginalCheckOutTime() {
        return originalCheckOutTime;
    }

    public void setOriginalCheckOutTime(Instant originalCheckOutTime) {
        this.originalCheckOutTime = originalCheckOutTime;
    }

    public Instant getRequestedCheckInTime() {
        return requestedCheckInTime;
    }

    public void setRequestedCheckInTime(Instant requestedCheckInTime) {
        this.requestedCheckInTime = requestedCheckInTime;
    }

    public Instant getRequestedCheckOutTime() {
        return requestedCheckOutTime;
    }

    public void setRequestedCheckOutTime(Instant requestedCheckOutTime) {
        this.requestedCheckOutTime = requestedCheckOutTime;
    }

    public Instant getRequestedTime() {
        return requestedTime;
    }

    public void setRequestedTime(Instant requestedTime) {
        this.requestedTime = requestedTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
