
package com.workforce.attendance.service;

import java.util.List;

import com.workforce.attendance.dto.AttendanceCheckInDTO;
import com.workforce.attendance.dto.AttendanceCheckOutDTO;
import com.workforce.attendance.dto.AttendanceResponseDTO;

public interface AttendanceService {

    AttendanceResponseDTO checkIn(AttendanceCheckInDTO dto);

    AttendanceResponseDTO checkOut(AttendanceCheckOutDTO dto);

    List<AttendanceResponseDTO> getAttendanceHistory(Long employeeId);
}
