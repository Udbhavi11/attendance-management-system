package com.workforce.attendance.service;

import java.util.List;

import com.workforce.attendance.dto.EmployeeDTO;

public interface EmployeeService {

    EmployeeDTO createEmployee(EmployeeDTO requestDTO);

    List<EmployeeDTO> getAllEmployees();

    EmployeeDTO getEmployeeById(Long id);
    
    EmployeeDTO updateEmployee(Long id, EmployeeDTO dto);
    
    EmployeeDTO deactivateEmployee(Long id);

	EmployeeDTO activateEmployee(Long id);
}
