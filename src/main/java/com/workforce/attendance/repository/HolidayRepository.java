
package com.workforce.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.workforce.attendance.entity.Holiday;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

}
