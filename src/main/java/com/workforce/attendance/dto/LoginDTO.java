
package com.workforce.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginDTO {

    @NotBlank(message = "Username is required")
    @Size(max = 100)
    private String username;

    @NotBlank(message = "Password is required")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String role;
    private String message;

    // Linked employee profile information
    private Long employeeId;
    private String employeeName;

    public LoginDTO() {
    }

    // Login request constructor
    public LoginDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Existing response constructor retained
    public LoginDTO(String username, String role, String message) {
        this.username = username;
        this.role = role;
        this.message = message;
    }

    // Response constructor with employee profile
    public LoginDTO(
            String username,
            String role,
            String message,
            Long employeeId,
            String employeeName) {

        this.username = username;
        this.role = role;
        this.message = message;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
}
