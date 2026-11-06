package com.workforce.attendance.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workforce.attendance.entity.BreakPolicy;

public interface BreakPolicyRepository extends JpaRepository<BreakPolicy, Long> {

    boolean existsByNameIgnoreCase(String name);
    
    Optional<BreakPolicy> findFirstByActiveTrue();
}