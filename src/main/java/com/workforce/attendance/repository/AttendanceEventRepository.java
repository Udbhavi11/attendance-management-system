
package com.workforce.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workforce.attendance.entity.AttendanceEvent;
import com.workforce.attendance.entity.AttendanceSession;

public interface AttendanceEventRepository
        extends JpaRepository<AttendanceEvent, Long> {

    // Retrieves all events belonging to a particular attendance session,
    // ordered by the time the server received them.
    List<AttendanceEvent> findByAttendanceSessionOrderByServerReceivedAtAsc(
            AttendanceSession attendanceSession);

    /*
     * Retrieves the most recent reliable location reading for an employee,
     * excluding the current event being evaluated.
     */
    Optional<AttendanceEvent>
            findFirstByAttendanceSession_Employee_IdAndIdNotAndLatitudeIsNotNullAndLongitudeIsNotNullAndLocationAccuracyMetersLessThanEqualOrderByServerReceivedAtDesc(
                    Long employeeId,
                    Long excludedEventId,
                    Double accuracyLimit);
}
