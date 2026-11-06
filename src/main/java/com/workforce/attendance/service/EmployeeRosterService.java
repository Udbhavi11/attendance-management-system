package com.workforce.attendance.service;

import java.util.List;

import com.workforce.attendance.dto.EmployeeRosterDTO;

public interface EmployeeRosterService {

    EmployeeRosterDTO createRoster(EmployeeRosterDTO dto);

    List<EmployeeRosterDTO> getAllRosters();

    EmployeeRosterDTO getRosterById(Long id);

    EmployeeRosterDTO updateRoster(Long id, EmployeeRosterDTO dto);

    List<EmployeeRosterDTO> getEmployeeRoster(Long employeeId);
    
    List<EmployeeRosterDTO> createBulkRoster(
    	    List<EmployeeRosterDTO> rosters
    	);

}