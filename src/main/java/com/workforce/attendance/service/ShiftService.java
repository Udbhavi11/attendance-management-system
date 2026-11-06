package com.workforce.attendance.service;

import java.util.List;

import com.workforce.attendance.dto.ShiftDTO;

public interface ShiftService {

    ShiftDTO createShift(ShiftDTO dto);

    List<ShiftDTO> getAllShifts();

    ShiftDTO getShiftById(Long id);

    ShiftDTO updateShift(Long id, ShiftDTO dto);

    ShiftDTO deactivateShift(Long id);
    
    ShiftDTO activateShift(Long id);
}