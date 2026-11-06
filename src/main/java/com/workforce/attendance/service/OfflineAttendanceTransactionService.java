package com.workforce.attendance.service;

import com.workforce.attendance.dto.OfflineAttendanceDTO;

public interface OfflineAttendanceTransactionService {

    OfflineAttendanceDTO processCheckIn(
            OfflineAttendanceDTO dto,
            String idempotencyKey);

    OfflineAttendanceDTO processCheckOut(
            OfflineAttendanceDTO dto,
            String idempotencyKey);

    OfflineAttendanceDTO processBreakStart(
            OfflineAttendanceDTO dto,
            String idempotencyKey);

    OfflineAttendanceDTO processBreakEnd(
            OfflineAttendanceDTO dto,
            String idempotencyKey);
}