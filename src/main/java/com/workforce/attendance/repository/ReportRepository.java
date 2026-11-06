package com.workforce.attendance.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.workforce.attendance.entity.AttendanceSession;
import com.workforce.attendance.entity.AttendanceStatus;

@Repository
public interface ReportRepository
extends JpaRepository<AttendanceSession, Long> {

	// Monthly working days, excluding weekly offs and holidays.
	@Query(value = """
			SELECT COUNT(DISTINCT r.roster_date)
			FROM employee_rosters r
			WHERE r.employee_id = :employeeId
			  AND r.roster_date >= :startDate
			  AND r.roster_date < :endDate
			  AND r.weekly_off = FALSE
			  AND NOT EXISTS (
			      SELECT 1
			      FROM holidays h
			      WHERE h.holiday_date = r.roster_date
			  )
			""", nativeQuery = true)
	long countMonthlyWorkingDays(
			@Param("employeeId") Long employeeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
			);


	// Weekly offs.
	@Query(value = """
			SELECT COUNT(DISTINCT r.roster_date)
			FROM employee_rosters r
			WHERE r.employee_id = :employeeId
			  AND r.roster_date >= :startDate
			  AND r.roster_date < :endDate
			  AND r.weekly_off = TRUE
			""", nativeQuery = true)
	long countMonthlyWeeklyOffs(
			@Param("employeeId") Long employeeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
			);


	// Organization-wide holidays in the selected month.
	@Query(value = """
			SELECT COUNT(DISTINCT h.holiday_date)
			FROM holidays h
			WHERE h.holiday_date >= :startDate
			  AND h.holiday_date < :endDate
			""", nativeQuery = true)
	long countMonthlyHolidays(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
			);


	// Present days.
	@Query(value = """
			SELECT COUNT(DISTINCT r.roster_date)
			FROM employee_rosters r
			JOIN attendance_sessions s ON s.roster_id = r.id
			WHERE r.employee_id = :employeeId
			  AND r.roster_date >= :startDate
			  AND r.roster_date < :endDate
			  AND r.weekly_off = FALSE
			  AND NOT EXISTS (
			      SELECT 1
			      FROM holidays h
			      WHERE h.holiday_date = r.roster_date
			  )
			""", nativeQuery = true)
	long countMonthlyPresentDays(
			@Param("employeeId") Long employeeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
			);


	// Late arrivals, including shift grace period.
	@Query(value = """
			SELECT COUNT(DISTINCT s.id)
			FROM attendance_sessions s
			JOIN employee_rosters r ON r.id = s.roster_id
			JOIN shifts sh ON sh.id = r.shift_id
			WHERE r.employee_id = :employeeId
			  AND r.roster_date >= :startDate
			  AND r.roster_date < :endDate
			  AND r.weekly_off = FALSE
			  AND s.check_in_time IS NOT NULL
			  AND s.check_in_time >
			      (
			          (
			              r.roster_date
			              + sh.start_time
			              + (
			                  sh.late_arrival_grace_minutes
			                  * INTERVAL '1 minute'
			                )
			          ) AT TIME ZONE 'Asia/Kolkata'
			      )
			""", nativeQuery = true)
	long countMonthlyLateArrivals(
			@Param("employeeId") Long employeeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
			);


	// Early departures. Handles overnight shifts.
	@Query(value = """
			SELECT COUNT(DISTINCT s.id)
			FROM attendance_sessions s
			JOIN employee_rosters r ON r.id = s.roster_id
			JOIN shifts sh ON sh.id = r.shift_id
			WHERE r.employee_id = :employeeId
			  AND r.roster_date >= :startDate
			  AND r.roster_date < :endDate
			  AND s.check_out_time IS NOT NULL
			  AND s.check_out_time <
			      (
			          (
			              r.roster_date
			              + sh.end_time
			              + CASE
			                  WHEN sh.end_time <= sh.start_time
			                  THEN INTERVAL '1 day'
			                  ELSE INTERVAL '0 day'
			                END
			          ) AT TIME ZONE 'Asia/Kolkata'
			      )
			""", nativeQuery = true)
	long countMonthlyEarlyDepartures(
			@Param("employeeId") Long employeeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
			);


	// Total working duration minus break duration.
	@Query(value = """
			SELECT COALESCE(
			    SUM(
			        GREATEST(
			            EXTRACT(
			                EPOCH FROM (
			                    COALESCE(s.check_out_time, NOW())
			                    - s.check_in_time
			                )
			            ) - COALESCE(
			                (
			                    SELECT SUM(
			                        EXTRACT(
			                            EPOCH FROM (
			                                COALESCE(b.end_time, NOW())
			                                - b.start_time
			                            )
			                        )
			                    )
			                    FROM break_records b
			                    WHERE b.attendance_session_id = s.id
			                ),
			                0
			            ),
			            0
			        )
			    ),
			    0
			)::BIGINT
			FROM attendance_sessions s
			JOIN employee_rosters r ON r.id = s.roster_id
			WHERE r.employee_id = :employeeId
			  AND r.roster_date >= :startDate
			  AND r.roster_date < :endDate
			""", nativeQuery = true)
	long calculateMonthlyWorkingDuration(
			@Param("employeeId") Long employeeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
			);


	// Total break duration.
	@Query(value = """
			SELECT COALESCE(
			    SUM(
			        EXTRACT(
			            EPOCH FROM (
			                COALESCE(b.end_time, NOW())
			                - b.start_time
			            )
			        )
			    ),
			    0
			)::BIGINT
			FROM break_records b
			JOIN attendance_sessions s
			    ON s.id = b.attendance_session_id
			JOIN employee_rosters r
			    ON r.id = s.roster_id
			WHERE r.employee_id = :employeeId
			  AND r.roster_date >= :startDate
			  AND r.roster_date < :endDate
			""", nativeQuery = true)
	long calculateMonthlyBreakDuration(
			@Param("employeeId") Long employeeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
			);


	// Pending correction requests for attendance sessions in the month.
	@Query("""
			SELECT COUNT(c)
			FROM CorrectionRequest c
			WHERE c.employee.id = :employeeId
			  AND c.status = com.workforce.attendance.entity.CorrectionStatus.PENDING
			  AND c.attendanceSession.roster.rosterDate >= :startDate
			  AND c.attendanceSession.roster.rosterDate < :endDate
			""")
	long countMonthlyPendingCorrections(
			@Param("employeeId") Long employeeId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
			);

	@Query(
			value = """
					SELECT
					    e.id AS "employeeId",
					    e.employee_code AS "employeeCode",
					    e.name AS "employeeName",
					    e.department AS "department",
					    r.id AS "rosterId",
					    r.roster_date AS "rosterDate",
					    s.id AS "attendanceSessionId",
					    s.status AS "attendanceStatus",
					    s.check_in_time AS "checkInTime",
					    s.check_out_time AS "checkOutTime",
					    sh.start_time AS "shiftStartTime",
					    sh.late_arrival_grace_minutes AS "lateArrivalGraceMinutes"
					FROM employee_rosters r
					JOIN employees e ON e.id = r.employee_id
					LEFT JOIN shifts sh ON sh.id = r.shift_id
					LEFT JOIN attendance_sessions s ON s.roster_id = r.id
					WHERE r.roster_date = :reportDate
					  AND r.weekly_off = FALSE
					  AND e.active = TRUE
					  AND (
					        :managerId IS NULL
					        OR e.manager_id = :managerId
					      )
					ORDER BY e.name ASC
					""",
					countQuery = """
							SELECT COUNT(DISTINCT r.id)
							FROM employee_rosters r
							JOIN employees e ON e.id = r.employee_id
							WHERE r.roster_date = :reportDate
							  AND r.weekly_off = FALSE
							  AND e.active = TRUE
							  AND (
							        :managerId IS NULL
							        OR e.manager_id = :managerId
							      )
							""",
							nativeQuery = true
			)
	Page<DailyAttendanceProjection> findDailyAttendance(
			@Param("reportDate") LocalDate reportDate,
			@Param("managerId") Long managerId,
			Pageable pageable
			);

	@Query("""
			SELECT COUNT(r)
			FROM EmployeeRoster r
			WHERE r.rosterDate = :reportDate
			  AND r.weeklyOff = false
			  AND r.employee.active = true
			  AND (
			        :managerId IS NULL
			        OR r.employee.manager.id = :managerId
			      )
			""")
	long countExpectedEmployees(
			@Param("reportDate") LocalDate reportDate,
			@Param("managerId") Long managerId
			);

	@Query("""
			SELECT COUNT(DISTINCT r.id)
			FROM EmployeeRoster r
			JOIN AttendanceSession s ON s.roster = r
			WHERE r.rosterDate = :reportDate
			  AND r.weeklyOff = false
			  AND r.employee.active = true
			  AND s.status = :status
			  AND (
			        :managerId IS NULL
			        OR r.employee.manager.id = :managerId
			      )
			""")
	long countEmployeesByAttendanceStatus(
			@Param("reportDate") LocalDate reportDate,
			@Param("managerId") Long managerId,
			@Param("status") AttendanceStatus status
			);

	@Query("""
			SELECT COUNT(DISTINCT r.id)
			FROM EmployeeRoster r
			JOIN AttendanceSession s ON s.roster = r
			WHERE r.rosterDate = :reportDate
			  AND r.weeklyOff = false
			  AND r.employee.active = true
			  AND (
			        :managerId IS NULL
			        OR r.employee.manager.id = :managerId
			      )
			""")
	long countPresentEmployees(
			@Param("reportDate") LocalDate reportDate,
			@Param("managerId") Long managerId
			);

	@Query(value = """
			SELECT COUNT(DISTINCT r.id)
			FROM employee_rosters r
			JOIN employees e ON e.id = r.employee_id
			JOIN shifts sh ON sh.id = r.shift_id
			JOIN attendance_sessions s ON s.roster_id = r.id
			WHERE r.roster_date = :reportDate
			  AND r.weekly_off = FALSE
			  AND e.active = TRUE
			  AND s.check_in_time IS NOT NULL
			  AND s.check_in_time >
			      (
			          (
			              r.roster_date
			              + sh.start_time
			              + (
			                  sh.late_arrival_grace_minutes
			                  * INTERVAL '1 minute'
			                )
			          ) AT TIME ZONE 'Asia/Kolkata'
			      )
			  AND (
			        CAST(:managerId AS BIGINT) IS NULL
			        OR e.manager_id = :managerId
			      )
			""", nativeQuery = true)
	long countLateArrivals(
			@Param("reportDate") LocalDate reportDate,
			@Param("managerId") Long managerId
			);
}