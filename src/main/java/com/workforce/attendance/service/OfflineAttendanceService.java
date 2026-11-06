
package com.workforce.attendance.service;

import com.workforce.attendance.dto.OfflineAttendanceDTO;

public interface OfflineAttendanceService {

    // Processes an offline attendance request and prevents duplicate submissions.
    OfflineAttendanceDTO processOfflineAttendance(
            OfflineAttendanceDTO dto,
            String idempotencyKey);

}
