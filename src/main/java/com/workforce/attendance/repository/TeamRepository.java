
package com.workforce.attendance.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workforce.attendance.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Team> findByManager_Id(Long managerId);
}
