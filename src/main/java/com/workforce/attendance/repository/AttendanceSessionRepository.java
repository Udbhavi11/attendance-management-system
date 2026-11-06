package com.workforce.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.workforce.attendance.entity.AttendanceSession;
import com.workforce.attendance.entity.AttendanceStatus;
import com.workforce.attendance.entity.Employee;

public interface AttendanceSessionRepository
        extends JpaRepository<AttendanceSession, Long> {

    boolean existsByEmployeeAndStatus(
            Employee employee,
            AttendanceStatus status);

    List<AttendanceSession> findByEmployeeOrderByCheckInTimeDesc(
            Employee employee);

    Optional<AttendanceSession> findByEmployeeAndStatus(
            Employee employee,
            AttendanceStatus status);

 // Retrieves the most recent attendance session of an employee
 	 // with the specified attendance status.
    Optional<AttendanceSession> findFirstByEmployeeAndStatusOrderByCheckInTimeDesc(
            Employee employee,
            AttendanceStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AttendanceSession a WHERE a.id = :sessionId")
    Optional<AttendanceSession> findByIdForUpdate(
            @Param("sessionId") Long sessionId);
    
    boolean existsByIdAndEmployee_Id(Long sessionId, Long employeeId);

    boolean existsByIdAndEmployee_Manager_Id(Long sessionId, Long managerId);
}
