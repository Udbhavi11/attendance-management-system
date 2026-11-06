
package com.workforce.attendance.entity;

import java.time.Instant;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "field_visits")
@Getter
@Setter
@NoArgsConstructor
public class FieldVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Employee performing the field visit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // Type of field visit
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisitType visitType;

    // Customer identification or reference
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String customerReference;

    // Server-generated visit start timestamp
    @NotNull
    @Column(nullable = false)
    private Instant startTime;

    // Server-generated visit end timestamp
    private Instant endTime;

    // Starting GPS coordinates
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Column(nullable = false)
    private Double startLatitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Column(nullable = false)
    private Double startLongitude;

    // Ending GPS coordinates
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double endLatitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double endLongitude;

    // IN_PROGRESS or COMPLETED
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus status;

    // Additional visit information
    @Size(max = 500)
    @Column(length = 500)
    private String remarks;

	public FieldVisit() {
		super();	
	}
	
	public FieldVisit(Long id, Employee employee,VisitType visitType,
			String customerReference,Instant startTime, Instant endTime,
			Double startLatitude,
			Double startLongitude,
			Double endLatitude,
			Double endLongitude, VisitStatus status,
			String remarks) {
		super();
		this.id = id;
		this.employee = employee;
		this.visitType = visitType;
		this.customerReference = customerReference;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startLatitude = startLatitude;
		this.startLongitude = startLongitude;
		this.endLatitude = endLatitude;
		this.endLongitude = endLongitude;
		this.status = status;
		this.remarks = remarks;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public VisitType getVisitType() {
		return visitType;
	}

	public void setVisitType(VisitType visitType) {
		this.visitType = visitType;
	}

	public String getCustomerReference() {
		return customerReference;
	}

	public void setCustomerReference(String customerReference) {
		this.customerReference = customerReference;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public Double getStartLatitude() {
		return startLatitude;
	}

	public void setStartLatitude(Double startLatitude) {
		this.startLatitude = startLatitude;
	}

	public Double getStartLongitude() {
		return startLongitude;
	}

	public void setStartLongitude(Double startLongitude) {
		this.startLongitude = startLongitude;
	}

	public Double getEndLatitude() {
		return endLatitude;
	}

	public void setEndLatitude(Double endLatitude) {
		this.endLatitude = endLatitude;
	}

	public Double getEndLongitude() {
		return endLongitude;
	}

	public void setEndLongitude(Double endLongitude) {
		this.endLongitude = endLongitude;
	}

	public VisitStatus getStatus() {
		return status;
	}

	public void setStatus(VisitStatus status) {
		this.status = status;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
}
