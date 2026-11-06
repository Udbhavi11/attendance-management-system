
package com.workforce.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.workforce.attendance.entity.CorrectionRequest;
import com.workforce.attendance.entity.CorrectionStatus;

public interface CorrectionRequestRepository
        extends JpaRepository<CorrectionRequest, Long> {

    List<CorrectionRequest> findByEmployeeIdOrderByRequestedTimeDesc(
            Long employeeId);

    List<CorrectionRequest> findByStatusOrderByRequestedTimeAsc(
            CorrectionStatus status);

    List<CorrectionRequest> findByEmployee_Manager_IdAndStatusOrderByRequestedTimeAsc(
            Long managerId,
            CorrectionStatus status);

    boolean existsByAttendanceSessionIdAndStatus(
            Long attendanceSessionId,
            CorrectionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CorrectionRequest c WHERE c.id = :requestId")
    Optional<CorrectionRequest> findByIdForUpdate(
            @Param("requestId") Long requestId);
}
