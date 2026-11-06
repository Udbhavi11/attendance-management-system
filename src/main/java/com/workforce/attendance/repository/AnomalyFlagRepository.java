package com.workforce.attendance.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workforce.attendance.entity.AnomalyFlag;
import com.workforce.attendance.entity.AttendanceEvent;
import com.workforce.attendance.entity.AnomalyReviewStatus;
import com.workforce.attendance.entity.AnomalySeverity;
import com.workforce.attendance.entity.BreakRecord;

public interface AnomalyFlagRepository
        extends JpaRepository<AnomalyFlag, Long> {

    boolean existsByAttendanceEventAndRuleIdentifier(
            AttendanceEvent attendanceEvent,
            String ruleIdentifier);

    boolean existsByBreakRecordAndRuleIdentifier(
            BreakRecord breakRecord,
            String ruleIdentifier);

    List<AnomalyFlag> findAllByOrderByDetectedAtDesc();

    List<AnomalyFlag> findByEmployeeIdOrderByDetectedAtDesc(
            Long employeeId);

    List<AnomalyFlag>
    findByEmployee_Manager_IdAndReviewStatusOrderByDetectedAtDesc(
            Long managerId,
            AnomalyReviewStatus reviewStatus);

    // Paginated anomaly records, filtered by date, manager and severity.
    @EntityGraph(attributePaths = {
            "employee",
            "attendanceEvent",
            "breakRecord",
            "reviewedBy"
    })
    @Query("""
        SELECT a
        FROM AnomalyFlag a
        WHERE a.detectedAt >= :startTime
          AND a.detectedAt < :endTime
          AND (
              :managerId IS NULL
              OR a.employee.manager.id = :managerId
          )
          AND (
              :severity IS NULL
              OR a.severity = :severity
          )
        ORDER BY a.detectedAt DESC
        """)
    Page<AnomalyFlag> findReportAnomalies(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("managerId") Long managerId,
            @Param("severity") AnomalySeverity severity,
            Pageable pageable);

    // Counts a particular group of anomaly rules.
    @Query("""
        SELECT COUNT(a)
        FROM AnomalyFlag a
        WHERE a.detectedAt >= :startTime
          AND a.detectedAt < :endTime
          AND a.ruleIdentifier IN :ruleIdentifiers
          AND (
              :managerId IS NULL
              OR a.employee.manager.id = :managerId
          )
        """)
    long countByRulesForReport(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("managerId") Long managerId,
            @Param("ruleIdentifiers") List<String> ruleIdentifiers);

    // Counts all anomalies awaiting review.
    @Query("""
        SELECT COUNT(a)
        FROM AnomalyFlag a
        WHERE a.detectedAt >= :startTime
          AND a.detectedAt < :endTime
          AND a.reviewStatus =
              com.workforce.attendance.entity.AnomalyReviewStatus.PENDING
          AND (
              :managerId IS NULL
              OR a.employee.manager.id = :managerId
          )
        """)
    long countPendingForReport(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("managerId") Long managerId);
}