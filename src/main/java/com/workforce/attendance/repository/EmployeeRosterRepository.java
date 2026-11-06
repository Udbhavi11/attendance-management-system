package com.workforce.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.EmployeeRoster;

@Repository
public interface EmployeeRosterRepository
        extends JpaRepository<EmployeeRoster, Long> {

    boolean existsByEmployeeAndRosterDate(
            Employee employee,
            LocalDate rosterDate);

    Optional<EmployeeRoster> findByEmployeeAndRosterDate(
            Employee employee,
            LocalDate rosterDate);

    List<EmployeeRoster> findByEmployeeOrderByRosterDateDesc(
            Employee employee);

    // Check whether a roster belongs to a particular employee
    boolean existsByIdAndEmployee_Id(
            Long rosterId,
            Long employeeId);

    // Check whether a roster belongs to a manager's direct report
    boolean existsByIdAndEmployee_Manager_Id(
            Long rosterId,
            Long managerId);
}