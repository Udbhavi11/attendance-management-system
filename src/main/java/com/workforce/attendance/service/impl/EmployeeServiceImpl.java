
package com.workforce.attendance.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.EmployeeDTO;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.OfficeLocation;
import com.workforce.attendance.entity.Role;
import com.workforce.attendance.entity.Shift;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.exception.DuplicateResourceException;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.OfficeLocationRepository;
import com.workforce.attendance.repository.RoleRepository;
import com.workforce.attendance.repository.ShiftRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.EmployeeService;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final OfficeLocationRepository officeLocationRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            ShiftRepository shiftRepository,
            OfficeLocationRepository officeLocationRepository,
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
        this.officeLocationRepository = officeLocationRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -----------------------------------------------------
    // CREATE EMPLOYEE AND LOGIN ACCOUNT
    // -----------------------------------------------------

    @Override
    public EmployeeDTO createEmployee(EmployeeDTO employeeDTO) {

        if (employeeRepository.existsByEmployeeCode(
                employeeDTO.getEmployeeCode())) {

            throw new DuplicateResourceException(
                    "Employee code already exists");
        }

        if (employeeRepository.existsByEmail(employeeDTO.getEmail())) {

            throw new DuplicateResourceException(
                    "Employee email already exists");
        }

        String username = validateUsername(employeeDTO.getUsername());

        if (userAccountRepository.findByUsername(username).isPresent()) {
            throw new DuplicateResourceException(
                    "Username already exists");
        }

        String rawPassword = validatePassword(employeeDTO.getPassword());

        String systemRole = validateSystemRole(
                employeeDTO.getSystemRole());

        Role role = getRoleByName(systemRole);

        Employee employee = new Employee();

        employee.setEmployeeCode(employeeDTO.getEmployeeCode());
        employee.setName(employeeDTO.getName());
        employee.setEmail(employeeDTO.getEmail());
        employee.setMobileNumber(employeeDTO.getMobileNumber());
        employee.setDepartment(employeeDTO.getDepartment());
        employee.setActive(true);
        employee.setRole(systemRole);

        employee.setAssignmentType(
                validateAssignmentType(employeeDTO.getAssignmentType()));

        assignRelationships(employee, employeeDTO);

        Employee savedEmployee = employeeRepository.save(employee);

        UserAccount account = new UserAccount();

        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setEmployee(savedEmployee);
        account.setRole(role);
        account.setActive(true);

        userAccountRepository.save(account);

        return convertToDTO(savedEmployee);
    }

    // -----------------------------------------------------
    // GET ALL EMPLOYEES
    // -----------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {

        return employeeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // -----------------------------------------------------
    // GET EMPLOYEE BY ID
    // -----------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeById(Long id) {

        Employee employee = getEmployeeEntity(id);

        return convertToDTO(employee);
    }

    // -----------------------------------------------------
    // UPDATE EMPLOYEE AND LOGIN ACCOUNT
    // -----------------------------------------------------

    @Override
    public EmployeeDTO updateEmployee(
            Long id,
            EmployeeDTO employeeDTO) {

        Employee employee = getEmployeeEntity(id);

        if (!employee.getEmployeeCode().equals(
                employeeDTO.getEmployeeCode())
                && employeeRepository.existsByEmployeeCode(
                        employeeDTO.getEmployeeCode())) {

            throw new DuplicateResourceException(
                    "Employee code already exists");
        }

        if (!employee.getEmail().equalsIgnoreCase(
                employeeDTO.getEmail())
                && employeeRepository.existsByEmail(
                        employeeDTO.getEmail())) {

            throw new DuplicateResourceException(
                    "Employee email already exists");
        }

        employee.setEmployeeCode(employeeDTO.getEmployeeCode());
        employee.setName(employeeDTO.getName());
        employee.setEmail(employeeDTO.getEmail());
        employee.setMobileNumber(employeeDTO.getMobileNumber());
        employee.setDepartment(employeeDTO.getDepartment());

        employee.setAssignmentType(
                validateAssignmentType(employeeDTO.getAssignmentType()));

        assignRelationships(employee, employeeDTO);

        UserAccount account = userAccountRepository
                .findByEmployeeId(employee.getId())
                .orElse(null);

        String requestedUsername = employeeDTO.getUsername();
        String requestedPassword = employeeDTO.getPassword();
        String requestedRole = employeeDTO.getSystemRole();

        // Existing employee has no linked login account.
        if (account == null) {

            String username = validateUsername(requestedUsername);
            String rawPassword = validatePassword(requestedPassword);
            String systemRole = validateSystemRole(requestedRole);

            if (userAccountRepository.findByUsername(username).isPresent()) {
                throw new DuplicateResourceException(
                        "Username already exists");
            }

            Role role = getRoleByName(systemRole);

            account = new UserAccount();

            account.setUsername(username);
            account.setPassword(passwordEncoder.encode(rawPassword));
            account.setEmployee(employee);
            account.setRole(role);
            account.setActive(employee.isActive());

            employee.setRole(systemRole);

            userAccountRepository.save(account);

        } else {

            // Update username only when provided.
            if (requestedUsername != null
                    && !requestedUsername.isBlank()) {

                String username = validateUsername(requestedUsername);

                var existingAccount =
                        userAccountRepository.findByUsername(username);

                if (existingAccount.isPresent()
                        && !existingAccount.get().getId()
                                .equals(account.getId())) {

                    throw new DuplicateResourceException(
                            "Username already exists");
                }

                account.setUsername(username);
            }

            // Blank password means retain existing password.
            if (requestedPassword != null
                    && !requestedPassword.isBlank()) {

                account.setPassword(
                        passwordEncoder.encode(
                                validatePassword(requestedPassword)));
            }

            // Update role when provided.
            if (requestedRole != null && !requestedRole.isBlank()) {

                String systemRole = validateSystemRole(requestedRole);

                account.setRole(getRoleByName(systemRole));
                employee.setRole(systemRole);
            }

            userAccountRepository.save(account);
        }

        Employee updatedEmployee = employeeRepository.save(employee);

        return convertToDTO(updatedEmployee);
    }

    // -----------------------------------------------------
    // DEACTIVATE EMPLOYEE AND LOGIN ACCOUNT
    // -----------------------------------------------------

    @Override
    public EmployeeDTO deactivateEmployee(Long id) {

        Employee employee = getEmployeeEntity(id);

        employee.setActive(false);

        userAccountRepository.findByEmployeeId(id)
                .ifPresent(account -> account.setActive(false));

        Employee updatedEmployee = employeeRepository.save(employee);

        return convertToDTO(updatedEmployee);
    }

    // -----------------------------------------------------
    // ACTIVATE EMPLOYEE AND LOGIN ACCOUNT
    // -----------------------------------------------------

    @Override
    public EmployeeDTO activateEmployee(Long id) {

        Employee employee = getEmployeeEntity(id);

        employee.setActive(true);

        userAccountRepository.findByEmployeeId(id)
                .ifPresent(account -> account.setActive(true));

        Employee updatedEmployee = employeeRepository.save(employee);

        return convertToDTO(updatedEmployee);
    }

    // -----------------------------------------------------
    // VALIDATE USERNAME
    // -----------------------------------------------------

    private String validateUsername(String username) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required");
        }

        String normalizedUsername = username.trim();

        if (normalizedUsername.length() > 100) {
            throw new IllegalArgumentException(
                    "Username cannot exceed 100 characters");
        }

        return normalizedUsername;
    }

    // -----------------------------------------------------
    // VALIDATE PASSWORD
    // -----------------------------------------------------

    private String validatePassword(String password) {

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required");
        }

        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException(
                    "Password must contain between 8 and 72 characters");
        }

        return password;
    }

    // -----------------------------------------------------
    // VALIDATE SYSTEM ROLE
    // -----------------------------------------------------

    private String validateSystemRole(String systemRole) {

        if (systemRole == null || systemRole.isBlank()) {
            throw new IllegalArgumentException(
                    "System role is required");
        }

        String normalizedRole = systemRole.trim()
                .toUpperCase(Locale.ROOT);

        if (!normalizedRole.equals("EMPLOYEE")
                && !normalizedRole.equals("MANAGER")
                && !normalizedRole.equals("AUDITOR")) {

            throw new IllegalArgumentException(
                    "System role must be EMPLOYEE, MANAGER or AUDITOR");
        }

        return normalizedRole;
    }

    // -----------------------------------------------------
    // GET ROLE FROM DATABASE
    // -----------------------------------------------------

    private Role getRoleByName(String roleName) {

        return roleRepository.findAll()
                .stream()
                .filter(role -> role.getName() != null
                        && role.getName().equalsIgnoreCase(roleName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found in database: " + roleName));
    }

    // -----------------------------------------------------
    // VALIDATE ASSIGNMENT TYPE
    // -----------------------------------------------------

    private String validateAssignmentType(String assignmentType) {

        if (assignmentType == null || assignmentType.isBlank()) {
            return "OFFICE";
        }

        String normalizedType = assignmentType.trim()
                .toUpperCase(Locale.ROOT);

        if (!normalizedType.equals("OFFICE")
                && !normalizedType.equals("FIELD_WORK")) {

            throw new IllegalArgumentException(
                    "Assignment type must be OFFICE or FIELD_WORK");
        }

        return normalizedType;
    }

    // -----------------------------------------------------
    // ASSIGN MANAGER, SHIFT AND OFFICE
    // -----------------------------------------------------

    private void assignRelationships(
            Employee employee,
            EmployeeDTO employeeDTO) {

        // Manager assignment
        if (employeeDTO.getManagerId() == null) {

            employee.setManager(null);

        } else {

            if (employee.getId() != null
                    && employee.getId().equals(employeeDTO.getManagerId())) {

                throw new IllegalArgumentException(
                        "An employee cannot be their own manager");
            }

            Employee manager = getEmployeeEntity(
                    employeeDTO.getManagerId());

            if (!manager.isActive()) {
                throw new IllegalArgumentException(
                        "Cannot assign an inactive employee as manager");
            }

            UserAccount managerAccount = userAccountRepository
                    .findByEmployeeId(manager.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Selected manager does not have a user account"));

            if (managerAccount.getRole() == null
                    || !"MANAGER".equalsIgnoreCase(
                            managerAccount.getRole().getName())) {

                throw new IllegalArgumentException(
                        "Selected employee does not have the MANAGER role");
            }

            if (!managerAccount.isActive()) {
                throw new IllegalArgumentException(
                        "Cannot assign an inactive user account as manager");
            }

            employee.setManager(manager);
        }

        // Shift assignment
        if (employeeDTO.getShiftId() == null) {

            employee.setAssignedShift(null);

        } else {

            Shift shift = shiftRepository.findById(
                    employeeDTO.getShiftId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Shift not found with id: "
                                    + employeeDTO.getShiftId()));

            if (!shift.isActive()) {
                throw new IllegalArgumentException(
                        "Cannot assign an inactive shift");
            }

            employee.setAssignedShift(shift);
        }

        // Office assignment
        if (employeeDTO.getOfficeId() == null) {

            employee.setAssignedOffice(null);

        } else {

            OfficeLocation office = officeLocationRepository.findById(
                    employeeDTO.getOfficeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Office location not found with id: "
                                    + employeeDTO.getOfficeId()));

            if (!office.isActive()) {
                throw new IllegalArgumentException(
                        "Cannot assign an inactive office location");
            }

            employee.setAssignedOffice(office);
        }
    }

    // -----------------------------------------------------
    // FIND EMPLOYEE
    // -----------------------------------------------------

    private Employee getEmployeeEntity(Long id) {

        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + id));
    }

    // -----------------------------------------------------
    // CONVERT ENTITY TO DTO
    // Never return the password.
    // -----------------------------------------------------

    private EmployeeDTO convertToDTO(Employee employee) {

        EmployeeDTO dto = new EmployeeDTO();

        dto.setId(employee.getId());
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setMobileNumber(employee.getMobileNumber());
        dto.setDepartment(employee.getDepartment());
        dto.setActive(employee.isActive());

        dto.setManagerId(
                employee.getManager() != null
                        ? employee.getManager().getId()
                        : null);

        dto.setShiftId(
                employee.getAssignedShift() != null
                        ? employee.getAssignedShift().getId()
                        : null);

        dto.setOfficeId(
                employee.getAssignedOffice() != null
                        ? employee.getAssignedOffice().getId()
                        : null);

        dto.setAssignmentType(employee.getAssignmentType());

        userAccountRepository.findByEmployeeId(employee.getId())
                .ifPresent(account -> {
                    dto.setUsername(account.getUsername());

                    if (account.getRole() != null) {
                        dto.setSystemRole(
                                account.getRole().getName());
                    }
                });

        if (dto.getSystemRole() == null) {
            dto.setSystemRole(employee.getRole());
        }

        return dto;
    }
}
