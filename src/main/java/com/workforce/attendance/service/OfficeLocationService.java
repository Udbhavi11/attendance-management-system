
package com.workforce.attendance.service;

import java.util.List;

import com.workforce.attendance.dto.OfficeLocationDTO;

public interface OfficeLocationService {

    OfficeLocationDTO createOfficeLocation(
            OfficeLocationDTO officeLocationDTO);

    List<OfficeLocationDTO> getAllOfficeLocations();

    OfficeLocationDTO getOfficeLocationById(Long id);

    OfficeLocationDTO updateOfficeLocation(
            Long id, OfficeLocationDTO officeLocationDTO);
}
