
package com.workforce.attendance.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.ShiftDTO;
import com.workforce.attendance.entity.Shift;
import com.workforce.attendance.exception.DuplicateResourceException;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.ShiftRepository;
import com.workforce.attendance.service.ShiftService;

@Service
@Transactional
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;

    public ShiftServiceImpl(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    // Validate shift details before creating or updating.
    private void validateShiftData(ShiftDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException(
                "Shift details are required"
            );
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException(
                "Shift name is required"
            );
        }

        if (dto.getName().trim().length() > 100) {
            throw new IllegalArgumentException(
                "Shift name cannot exceed 100 characters"
            );
        }

        if (dto.getStartTime() == null) {
            throw new IllegalArgumentException(
                "Shift start time is required"
            );
        }

        if (dto.getEndTime() == null) {
            throw new IllegalArgumentException(
                "Shift end time is required"
            );
        }

        if (dto.getStartTime().equals(dto.getEndTime())) {
            throw new IllegalArgumentException(
                "Shift start time and end time cannot be the same"
            );
        }

        if (dto.getLateArrivalGraceMinutes() < 0) {
            throw new IllegalArgumentException(
                "Late arrival grace period cannot be negative"
            );
        }
    }

    @Override
    public ShiftDTO createShift(ShiftDTO dto) {

        validateShiftData(dto);

        String shiftName = dto.getName().trim();

        if (shiftRepository.existsByNameIgnoreCase(shiftName)) {
            throw new DuplicateResourceException(
                "Shift already exists with name: " + shiftName
            );
        }

        Shift shift = new Shift();

        shift.setName(shiftName);
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setLateArrivalGraceMinutes(
            dto.getLateArrivalGraceMinutes()
        );
        shift.setActive(true);

        Shift savedShift = shiftRepository.save(shift);

        return convertToDTO(savedShift);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftDTO> getAllShifts() {

        return shiftRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftDTO getShiftById(Long id) {

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Shift not found with ID: " + id
                ));

        return convertToDTO(shift);
    }

    @Override
    public ShiftDTO updateShift(Long id, ShiftDTO dto) {

        validateShiftData(dto);

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Shift not found with ID: " + id
                ));

        String shiftName = dto.getName().trim();

        if (!shift.getName().equalsIgnoreCase(shiftName)
                && shiftRepository.existsByNameIgnoreCase(shiftName)) {

            throw new DuplicateResourceException(
                "Shift already exists with name: " + shiftName
            );
        }

        shift.setName(shiftName);
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setLateArrivalGraceMinutes(
            dto.getLateArrivalGraceMinutes()
        );

        Shift updatedShift = shiftRepository.save(shift);

        return convertToDTO(updatedShift);
    }

    @Override
    public ShiftDTO deactivateShift(Long id) {

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Shift not found with ID: " + id
                ));

        shift.setActive(false);

        Shift updatedShift = shiftRepository.save(shift);

        return convertToDTO(updatedShift);
    }

    @Override
    public ShiftDTO activateShift(Long id) {

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Shift not found with ID: " + id
                ));

        shift.setActive(true);

        Shift activatedShift = shiftRepository.save(shift);

        return convertToDTO(activatedShift);
    }

    private ShiftDTO convertToDTO(Shift shift) {

        return new ShiftDTO(
            shift.getId(),
            shift.getName(),
            shift.getStartTime(),
            shift.getEndTime(),
            shift.getLateArrivalGraceMinutes(),
            shift.isActive()
        );
    }
}
