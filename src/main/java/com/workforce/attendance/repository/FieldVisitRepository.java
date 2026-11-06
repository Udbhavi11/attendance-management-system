package com.workforce.attendance.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.workforce.attendance.entity.FieldVisit;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.VisitStatus;

public interface FieldVisitRepository
        extends JpaRepository<FieldVisit, Long> {

    List<FieldVisit> findByEmployeeOrderByStartTimeDesc(
            Employee employee);

    List<FieldVisit> findByEmployeeAndStatusOrderByStartTimeDesc(
            Employee employee, VisitStatus status);

    boolean existsByEmployeeAndStatus(
            Employee employee, VisitStatus status);

    List<FieldVisit> findByEmployeeAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeDesc(
            Employee employee, Instant start, Instant end);

    List<FieldVisit> findByEmployee_Manager_IdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeDesc(
            Long managerId, Instant start, Instant end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fv FROM FieldVisit fv WHERE fv.id = :visitId")
    Optional<FieldVisit> findByIdForUpdate(
            @Param("visitId") Long visitId);

    // Check whether a field visit belongs to an employee
    boolean existsByIdAndEmployee_Id(
            Long visitId, Long employeeId);

    // Check whether a field visit belongs to a manager's direct report
    boolean existsByIdAndEmployee_Manager_Id(
            Long visitId, Long managerId);

    @Query(value = """
            SELECT COUNT(*)
            FROM field_visits fv
            JOIN employees e ON e.id = fv.employee_id
            WHERE fv.start_time >= :startTime
              AND fv.start_time < :endTime
              AND (
                  CAST(:managerId AS BIGINT) IS NULL
                  OR e.manager_id = :managerId
              )
              AND (
                  CAST(:employeeId AS BIGINT) IS NULL
                  OR e.id = :employeeId
              )
            """, nativeQuery = true)
    long countReportVisits(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("managerId") Long managerId,
            @Param("employeeId") Long employeeId);

    @Query(value = """
            SELECT COUNT(*)
            FROM field_visits fv
            JOIN employees e ON e.id = fv.employee_id
            WHERE fv.start_time >= :startTime
              AND fv.start_time < :endTime
              AND fv.status = 'COMPLETED'
              AND (
                  CAST(:managerId AS BIGINT) IS NULL
                  OR e.manager_id = :managerId
              )
              AND (
                  CAST(:employeeId AS BIGINT) IS NULL
                  OR e.id = :employeeId
              )
            """, nativeQuery = true)
    long countCompletedReportVisits(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("managerId") Long managerId,
            @Param("employeeId") Long employeeId);

    @Query(value = """
            SELECT COUNT(*)
            FROM field_visits fv
            JOIN employees e ON e.id = fv.employee_id
            WHERE fv.start_time >= :startTime
              AND fv.start_time < :endTime
              AND fv.status = 'IN_PROGRESS'
              AND (
                  CAST(:managerId AS BIGINT) IS NULL
                  OR e.manager_id = :managerId
              )
              AND (
                  CAST(:employeeId AS BIGINT) IS NULL
                  OR e.id = :employeeId
              )
            """, nativeQuery = true)
    long countIncompleteReportVisits(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("managerId") Long managerId,
            @Param("employeeId") Long employeeId);

    @Query(value = """
            SELECT COALESCE(
                SUM(
                    EXTRACT(
                        EPOCH FROM (
                            COALESCE(fv.end_time, NOW()) - fv.start_time
                        )
                    )
                ),
                0
            )::BIGINT
            FROM field_visits fv
            JOIN employees e ON e.id = fv.employee_id
            WHERE fv.start_time >= :startTime
              AND fv.start_time < :endTime
              AND (
                  CAST(:managerId AS BIGINT) IS NULL
                  OR e.manager_id = :managerId
              )
              AND (
                  CAST(:employeeId AS BIGINT) IS NULL
                  OR e.id = :employeeId
              )
            """, nativeQuery = true)
    long calculateTotalVisitDuration(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("managerId") Long managerId,
            @Param("employeeId") Long employeeId);

    @Query(value = """
            SELECT
                fv.visit_type AS "visitType",
                COUNT(*) AS "visitCount"
            FROM field_visits fv
            JOIN employees e ON e.id = fv.employee_id
            WHERE fv.start_time >= :startTime
              AND fv.start_time < :endTime
              AND (
                  CAST(:managerId AS BIGINT) IS NULL
                  OR e.manager_id = :managerId
              )
              AND (
                  CAST(:employeeId AS BIGINT) IS NULL
                  OR e.id = :employeeId
              )
            GROUP BY fv.visit_type
            ORDER BY fv.visit_type
            """, nativeQuery = true)
    List<FieldVisitTypeProjection> getVisitTypeBreakdown(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("managerId") Long managerId,
            @Param("employeeId") Long employeeId);
}