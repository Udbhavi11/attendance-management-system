package com.workforce.attendance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.workforce.attendance.dto.EmployeeDTO;
import com.workforce.attendance.service.EmployeeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/employees")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // CREATE EMPLOYEE
    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(
            @Valid @RequestBody EmployeeDTO employeeDTO) {

        EmployeeDTO savedEmployee =
                employeeService.createEmployee(employeeDTO);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedEmployee);
    }

    // GET ALL EMPLOYEES
    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {

        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // GET EMPLOYEE BY ID
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(
            @PathVariable Long id) {

        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // UPDATE EMPLOYEE
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDTO employeeDTO) {

        return ResponseEntity.ok(
                employeeService.updateEmployee(id, employeeDTO));
    }

    // DEACTIVATE EMPLOYEE
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<EmployeeDTO> deactivateEmployee(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                employeeService.deactivateEmployee(id));
    }

    // ACTIVATE EMPLOYEE
    @PatchMapping("/{id}/activate")
    public ResponseEntity<EmployeeDTO> activateEmployee(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                employeeService.activateEmployee(id));
    }
}