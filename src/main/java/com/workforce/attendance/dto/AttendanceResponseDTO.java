
package com.workforce.attendance.dto;

import java.time.Instant;

import com.workforce.attendance.entity.AttendanceStatus;

public class AttendanceResponseDTO {

    private Long id;
    private Long employeeId;
    private Instant checkInTime;
    private Instant checkOutTime;
    private AttendanceStatus status;

    // Calculated from the assigned shift.
    // Null means no applicable roster/shift.
    private Long lateArrivalMinutes;
    private Long earlyDepartureMinutes;

    public AttendanceResponseDTO() {
    }

    public AttendanceResponseDTO(
            Long id,
            Long employeeId,
            Instant checkInTime,
            Instant checkOutTime,
            AttendanceStatus status) {

        this.id = id;
        this.employeeId = employeeId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
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

    public Instant getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Instant checkInTime) {
        this.checkInTime = checkInTime;
    }

    public Instant getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Instant checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Long getLateArrivalMinutes() {
        return lateArrivalMinutes;
    }

    public void setLateArrivalMinutes(Long lateArrivalMinutes) {
        this.lateArrivalMinutes = lateArrivalMinutes;
    }

    public Long getEarlyDepartureMinutes() {
        return earlyDepartureMinutes;
    }

    public void setEarlyDepartureMinutes(Long earlyDepartureMinutes) {
        this.earlyDepartureMinutes = earlyDepartureMinutes;
    }

    @Override
    public String toString() {
        return "AttendanceResponseDTO [id=" + id
                + ", employeeId=" + employeeId
                + ", checkInTime=" + checkInTime
                + ", checkOutTime=" + checkOutTime
                + ", status=" + status
                + ", lateArrivalMinutes=" + lateArrivalMinutes
                + ", earlyDepartureMinutes=" + earlyDepartureMinutes
                + "]";
    }
}
