package com.workforce.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.workforce.attendance.entity.Employee;

@Repository
public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    // Locks the employee database row until the current transaction completes.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.id = :employeeId")
    Optional<Employee> findByIdForUpdate(
            @Param("employeeId") Long employeeId);

    boolean existsByTeam_Id(Long teamId);

    List<Employee> findByTeam_Id(Long teamId);

    // Check whether an employee reports directly to a manager
    boolean existsByIdAndManager_Id(
            Long employeeId,
            Long managerId);
}