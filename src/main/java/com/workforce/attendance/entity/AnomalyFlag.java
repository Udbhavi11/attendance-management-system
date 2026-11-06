
package com.workforce.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(
    name = "anomaly_flags",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_anomaly_break_rule",
            columnNames = {"break_record_id", "rule_identifier"}
        ),
        @UniqueConstraint(
            name = "uk_anomaly_event_rule",
            columnNames = {"attendance_event_id", "rule_identifier"}
        )
    },
    indexes = {
        @Index(
            name = "idx_anomaly_employee",
            columnList = "employee_id"
        ),
        @Index(
            name = "idx_anomaly_review_status",
            columnList = "review_status"
        ),
        @Index(
            name = "idx_anomaly_detected_at",
            columnList = "detected_at"
        )
    }
)
public class AnomalyFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_event_id")
    private AttendanceEvent attendanceEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "break_record_id")
    private BreakRecord breakRecord;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String ruleIdentifier;

    @Column(nullable = false)
    private Instant detectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnomalySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnomalyReviewStatus reviewStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Employee reviewedBy;

    private Instant reviewedAt;

    @Size(max = 500)
    @Column(length = 500)
    private String reviewOutcome;

    public AnomalyFlag() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public AttendanceEvent getAttendanceEvent() {
        return attendanceEvent;
    }

    public void setAttendanceEvent(AttendanceEvent attendanceEvent) {
        this.attendanceEvent = attendanceEvent;
    }

    public BreakRecord getBreakRecord() {
        return breakRecord;
    }

    public void setBreakRecord(BreakRecord breakRecord) {
        this.breakRecord = breakRecord;
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

    public String getReviewOutcome() {
        return reviewOutcome;
    }

    public void setReviewOutcome(String reviewOutcome) {
        this.reviewOutcome = reviewOutcome;
    }
}
