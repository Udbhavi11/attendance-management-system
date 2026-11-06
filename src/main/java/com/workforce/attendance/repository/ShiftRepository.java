package com.workforce.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workforce.attendance.entity.Shift;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    boolean existsByNameIgnoreCase(String name);

}