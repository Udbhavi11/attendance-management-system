
package com.workforce.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workforce.attendance.entity.OfficeLocation;

public interface OfficeLocationRepository
        extends JpaRepository<OfficeLocation, Long> {

}
