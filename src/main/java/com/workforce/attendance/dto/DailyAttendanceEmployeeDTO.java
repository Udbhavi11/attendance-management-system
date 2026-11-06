package com.workforce.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.workforce.attendance.entity.AttendanceStatus;

public class DailyAttendanceEmployeeDTO {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;

    private Long rosterId;
    private LocalDate rosterDate;

    private Long attendanceSessionId;
    private AttendanceStatus attendanceStatus;

    private Instant checkInTime;
    private Instant checkOutTime;

    private long lateArrivalMinutes;

    public DailyAttendanceEmployeeDTO() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getRosterId() {
        return rosterId;
    }

    public void setRosterId(Long rosterId) {
        this.rosterId = rosterId;
    }

    public LocalDate getRosterDate() {
        return rosterDate;
    }

    public void setRosterDate(LocalDate rosterDate) {
        this.rosterDate = rosterDate;
    }

    public Long getAttendanceSessionId() {
        return attendanceSessionId;
    }

    public void setAttendanceSessionId(Long attendanceSessionId) {
        this.attendanceSessionId = attendanceSessionId;
    }

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
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

    public long getLateArrivalMinutes() {
        return lateArrivalMinutes;
    }

    public void setLateArrivalMinutes(long lateArrivalMinutes) {
        this.lateArrivalMinutes = lateArrivalMinutes;
    }
}