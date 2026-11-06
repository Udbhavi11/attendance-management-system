package com.workforce.attendance.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workforce.attendance.entity.UserAccount;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmployeeId(Long employeeId);

    Optional<UserAccount> findByUsername(String username);

    @EntityGraph(attributePaths = {"employee"})
    Optional<UserAccount> findAccountWithEmployeeByUsername(String username);

    @EntityGraph(attributePaths = {
            "employee",
            "employee.manager",
            "role"
    })
    Optional<UserAccount> findAccountWithEmployeeAndRoleByUsername(
            String username);
}