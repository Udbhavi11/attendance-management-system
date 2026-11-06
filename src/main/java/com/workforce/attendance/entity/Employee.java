
package com.workforce.attendance.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employees")
@Data
@Getter
@Setter
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, unique = true, length = 30)
    private String employeeCode;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Size(max = 15)
    @Column(length = 15)
    private String mobileNumber;

    @Size(max = 100)
    @Column(length = 100)
    private String department;

    @Column(nullable = false)
    private boolean active = true;

    // Employee role: EMPLOYEE or MANAGER
    @NotBlank
    @Column(name = "role", nullable = false, length = 20)
    private String role = "EMPLOYEE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // Reporting manager of this employee
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift assignedShift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_location_id")
    private OfficeLocation assignedOffice;

    @Column(name = "assignment_type", nullable = false, length = 20)
    private String assignmentType = "OFFICE";

    public Employee() {
        super();
    }

    // Existing constructor preserved for compatibility
    public Employee(
            Long id,
            @NotBlank @Size(max = 30) String employeeCode,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @Size(max = 15) String mobileNumber,
            @Size(max = 100) String department,
            boolean active,
            Team team,
            Employee manager,
            Instant createdAt,
            Shift assignedShift,
            OfficeLocation assignedOffice,
            String assignmentType) {

        super();

        this.id = id;
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.department = department;
        this.active = active;
        this.team = team;
        this.manager = manager;
        this.createdAt = createdAt;
        this.assignedShift = assignedShift;
        this.assignedOffice = assignedOffice;
        this.assignmentType = assignmentType;
        this.role = "EMPLOYEE";
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Shift getAssignedShift() {
        return assignedShift;
    }

    public void setAssignedShift(Shift assignedShift) {
        this.assignedShift = assignedShift;
    }

    public OfficeLocation getAssignedOffice() {
        return assignedOffice;
    }

    public void setAssignedOffice(OfficeLocation assignedOffice) {
        this.assignedOffice = assignedOffice;
    }

    public String getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }
}
