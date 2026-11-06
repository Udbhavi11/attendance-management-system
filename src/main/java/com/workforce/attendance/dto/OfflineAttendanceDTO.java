package com.workforce.attendance.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workforce.attendance.entity.AttendanceEventType;
import com.workforce.attendance.entity.SubmissionStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class OfflineAttendanceDTO {

    // Request fields

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Event type is required")
    private AttendanceEventType eventType;

    @NotNull(message = "Client reported timestamp is required")
    private Instant clientReportedAt;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "Latitude is required")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "Longitude is required")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "GPS accuracy is required")
    @Positive(message = "GPS accuracy must be positive")
    private Double locationAccuracyMeters;

    // Response fields

    private Long eventId;

    private SubmissionStatus status;

    private Instant serverReceivedAt;

    private String message;

    public OfflineAttendanceDTO() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public AttendanceEventType getEventType() {
        return eventType;
    }

    public void setEventType(AttendanceEventType eventType) {
        this.eventType = eventType;
    }

    public Instant getClientReportedAt() {
        return clientReportedAt;
    }

    public void setClientReportedAt(Instant clientReportedAt) {
        this.clientReportedAt = clientReportedAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLocationAccuracyMeters() {
        return locationAccuracyMeters;
    }

    public void setLocationAccuracyMeters(Double locationAccuracyMeters) {
        this.locationAccuracyMeters = locationAccuracyMeters;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public Instant getServerReceivedAt() {
        return serverReceivedAt;
    }

    public void setServerReceivedAt(Instant serverReceivedAt) {
        this.serverReceivedAt = serverReceivedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}