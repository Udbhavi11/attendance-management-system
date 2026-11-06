package com.workforce.attendance.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.workforce.attendance.entity.AttendanceStatus;

public interface DailyAttendanceProjection {

    Long getEmployeeId();

    String getEmployeeCode();

    String getEmployeeName();

    String getDepartment();

    Long getRosterId();

    LocalDate getRosterDate();

    Long getAttendanceSessionId();

    AttendanceStatus getAttendanceStatus();

    Instant getCheckInTime();

    Instant getCheckOutTime();

    LocalTime getShiftStartTime();

    Integer getLateArrivalGraceMinutes();
}