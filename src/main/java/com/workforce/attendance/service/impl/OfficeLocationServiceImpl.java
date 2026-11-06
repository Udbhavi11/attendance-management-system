
package com.workforce.attendance.service.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.workforce.attendance.dto.OfficeLocationDTO;
import com.workforce.attendance.entity.OfficeLocation;
import com.workforce.attendance.repository.OfficeLocationRepository;
import com.workforce.attendance.service.OfficeLocationService;

@Service
public class OfficeLocationServiceImpl implements OfficeLocationService {

    private final OfficeLocationRepository officeLocationRepository;

    public OfficeLocationServiceImpl(
            OfficeLocationRepository officeLocationRepository) {
        this.officeLocationRepository = officeLocationRepository;
    }

    // Validate office details before saving.
    private void validateOfficeLocation(OfficeLocationDTO dto) {

        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Office details are required.");
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Office name is required.");
        }

        if (dto.getName().trim().length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Office name cannot exceed 100 characters.");
        }

        if (dto.getAddress() == null || dto.getAddress().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Office address is required.");
        }

        if (dto.getAddress().trim().length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Office address cannot exceed 255 characters.");
        }

        if (dto.getLatitude() == null
                || !Double.isFinite(dto.getLatitude())
                || dto.getLatitude() < -90
                || dto.getLatitude() > 90) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Latitude must be a valid value between -90 and 90.");
        }

        if (dto.getLongitude() == null
                || !Double.isFinite(dto.getLongitude())
                || dto.getLongitude() < -180
                || dto.getLongitude() > 180) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Longitude must be a valid value between -180 and 180.");
        }

        if (dto.getAllowedRadiusMeters() == null
                || !Double.isFinite(dto.getAllowedRadiusMeters())
                || dto.getAllowedRadiusMeters() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Allowed radius must be greater than zero.");
        }
    }

    // Convert Entity to DTO.
    private OfficeLocationDTO convertToDTO(OfficeLocation office) {

        OfficeLocationDTO dto = new OfficeLocationDTO();

        dto.setId(office.getId());
        dto.setName(office.getName());
        dto.setAddress(office.getAddress());
        dto.setLatitude(office.getLatitude());
        dto.setLongitude(office.getLongitude());
        dto.setAllowedRadiusMeters(office.getAllowedRadiusMeters());
        dto.setActive(office.isActive());

        return dto;
    }

    // Convert DTO to Entity.
    private OfficeLocation convertToEntity(OfficeLocationDTO dto) {

        OfficeLocation office = new OfficeLocation();

        office.setName(dto.getName().trim());
        office.setAddress(dto.getAddress().trim());
        office.setLatitude(dto.getLatitude());
        office.setLongitude(dto.getLongitude());
        office.setAllowedRadiusMeters(dto.getAllowedRadiusMeters());
        office.setActive(dto.isActive());

        return office;
    }

    // Create office.
    @Override
    public OfficeLocationDTO createOfficeLocation(
            OfficeLocationDTO officeLocationDTO) {

        validateOfficeLocation(officeLocationDTO);

        OfficeLocation office = convertToEntity(officeLocationDTO);

        OfficeLocation savedOffice =
                officeLocationRepository.save(office);

        return convertToDTO(savedOffice);
    }

    // Get all offices.
    @Override
    public List<OfficeLocationDTO> getAllOfficeLocations() {

        return officeLocationRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // Get office by ID.
    @Override
    public OfficeLocationDTO getOfficeLocationById(Long id) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid office ID.");
        }

        OfficeLocation office = officeLocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Office location not found with ID: " + id));

        return convertToDTO(office);
    }

    // Update office.
    @Override
    public OfficeLocationDTO updateOfficeLocation(
            Long id, OfficeLocationDTO officeLocationDTO) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid office ID.");
        }

        validateOfficeLocation(officeLocationDTO);

        OfficeLocation existingOffice =
                officeLocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Office location not found with ID: " + id));

        existingOffice.setName(officeLocationDTO.getName().trim());
        existingOffice.setAddress(officeLocationDTO.getAddress().trim());
        existingOffice.setLatitude(officeLocationDTO.getLatitude());
        existingOffice.setLongitude(officeLocationDTO.getLongitude());

        existingOffice.setAllowedRadiusMeters(
                officeLocationDTO.getAllowedRadiusMeters());

        existingOffice.setActive(officeLocationDTO.isActive());

        OfficeLocation updatedOffice =
                officeLocationRepository.save(existingOffice);

        return convertToDTO(updatedOffice);
    }
}
