
package com.workforce.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(
    name = "correction_requests",
    indexes = {
        @Index(name = "idx_correction_employee", columnList = "employee_id"),
        @Index(name = "idx_correction_status", columnList = "status"),
        @Index(name = "idx_correction_requested_time", columnList = "requested_time")
    }
)
public class CorrectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_session_id", nullable = false)
    private AttendanceSession attendanceSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotBlank(message = "Correction reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant requestedTime;

    // Original attendance values captured when the request is submitted
    private Instant originalCheckInTime;
    private Instant originalCheckOutTime;

    // Attendance values requested by the employee
    private Instant requestedCheckInTime;
    private Instant requestedCheckOutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CorrectionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Employee reviewedBy;

    private Instant reviewedAt;

    @PrePersist
    protected void onCreate() {
        if (requestedTime == null) {
            requestedTime = Instant.now();
        }

        if (status == null) {
            status = CorrectionStatus.PENDING;
        }
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AttendanceSession getAttendanceSession() {
        return attendanceSession;
    }

    public void setAttendanceSession(AttendanceSession attendanceSession) {
        this.attendanceSession = attendanceSession;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getRequestedTime() {
        return requestedTime;
    }

    public void setRequestedTime(Instant requestedTime) {
        this.requestedTime = requestedTime;
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

    public CorrectionStatus getStatus() {
        return status;
    }

    public void setStatus(CorrectionStatus status) {
        this.status = status;
    }

    public Employee getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Employee reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
