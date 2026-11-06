
package com.workforce.attendance.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attendance_events")
public class AttendanceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    // An event may exist without an attendance session while awaiting review.
    @JoinColumn(name = "attendance_session_id", nullable = true)
    private AttendanceSession attendanceSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttendanceEventType eventType;

    @Column(nullable = false)
    private Instant serverReceivedAt;

    private Instant clientReportedAt;

    private Double latitude;

    private Double longitude;

    private Double locationAccuracyMeters;

    public AttendanceEvent() {
    }

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

    public AttendanceEventType getEventType() {
        return eventType;
    }

    public void setEventType(AttendanceEventType eventType) {
        this.eventType = eventType;
    }

    public Instant getServerReceivedAt() {
        return serverReceivedAt;
    }

    public void setServerReceivedAt(Instant serverReceivedAt) {
        this.serverReceivedAt = serverReceivedAt;
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
}
