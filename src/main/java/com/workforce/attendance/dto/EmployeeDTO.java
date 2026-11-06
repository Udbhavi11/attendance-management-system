
package com.workforce.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmployeeDTO {

    private Long id;

    @NotBlank(message = "Employee code is required")
    @Size(max = 30, message = "Employee code cannot exceed 30 characters")
    private String employeeCode;

    @NotBlank(message = "Employee name is required")
    @Size(max = 100, message = "Employee name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @Size(max = 15, message = "Mobile number cannot exceed 15 characters")
    private String mobileNumber;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    private boolean active;

    private Long managerId;
    private Long shiftId;
    private Long officeId;

    @NotBlank(message = "Assignment type is required")
    @Pattern(
        regexp = "OFFICE|FIELD_WORK",
        message = "Assignment type must be OFFICE or FIELD_WORK"
    )
    private String assignmentType = "OFFICE";

    // Login account details
    @Size(max = 100, message = "Username cannot exceed 100 characters")
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(
        min = 8,
        max = 72,
        message = "Password must contain between 8 and 72 characters"
    )
    private String password;

    // Application login role, separate from assignmentType
    @Pattern(
        regexp = "EMPLOYEE|MANAGER",
        message = "System role must be EMPLOYEE or MANAGER"
    )
    private String systemRole = "EMPLOYEE";

    public EmployeeDTO() {
    }

    // Existing constructor retained for compatibility
    public EmployeeDTO(
            Long id,
            String employeeCode,
            String name,
            String email,
            String mobileNumber,
            String department,
            boolean active,
            Long managerId,
            Long shiftId,
            Long officeId) {

        this(
            id,
            employeeCode,
            name,
            email,
            mobileNumber,
            department,
            active,
            managerId,
            shiftId,
            officeId,
            "OFFICE"
        );
    }

    // Existing constructor including assignment type
    public EmployeeDTO(
            Long id,
            String employeeCode,
            String name,
            String email,
            String mobileNumber,
            String department,
            boolean active,
            Long managerId,
            Long shiftId,
            Long officeId,
            String assignmentType) {

        this.id = id;
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.department = department;
        this.active = active;
        this.managerId = managerId;
        this.shiftId = shiftId;
        this.officeId = officeId;
        this.assignmentType = assignmentType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public Long getOfficeId() {
        return officeId;
    }

    public void setOfficeId(Long officeId) {
        this.officeId = officeId;
    }

    public String getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    @Override
    public String toString() {
        return "EmployeeDTO{" +
                "id=" + id +
                ", employeeCode='" + employeeCode + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", department='" + department + '\'' +
                ", active=" + active +
                ", managerId=" + managerId +
                ", shiftId=" + shiftId +
                ", officeId=" + officeId +
                ", assignmentType='" + assignmentType + '\'' +
                ", username='" + username + '\'' +
                ", systemRole='" + systemRole + '\'' +
                '}';
    }
}
