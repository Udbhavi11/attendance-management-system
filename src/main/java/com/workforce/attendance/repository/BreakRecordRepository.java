package com.workforce.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workforce.attendance.entity.AttendanceSession;
import com.workforce.attendance.entity.BreakRecord;

public interface BreakRecordRepository extends JpaRepository<BreakRecord, Long> {

    boolean existsByAttendanceSessionAndEndTimeIsNull(AttendanceSession attendanceSession);

    Optional<BreakRecord> findByAttendanceSessionAndEndTimeIsNull(AttendanceSession attendanceSession);

    List<BreakRecord> findByAttendanceSessionOrderByStartTimeAsc(AttendanceSession attendanceSession);

    List<BreakRecord> findByEndTimeIsNull();
}