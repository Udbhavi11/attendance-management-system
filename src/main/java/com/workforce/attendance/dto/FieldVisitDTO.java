
package com.workforce.attendance.dto;

import java.time.Instant;

import com.workforce.attendance.entity.VisitStatus;
import com.workforce.attendance.entity.VisitType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FieldVisitDTO {

    private Long id;

    @NotNull
    private Long employeeId;

    private String employeeName;

    @NotNull
    private VisitType visitType;

    @NotBlank
    @Size(max = 100)
    private String customerReference;

    private Instant startTime;

    private Instant endTime;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double startLatitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double startLongitude;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double endLatitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double endLongitude;

    private VisitStatus status;

    @Size(max = 500)
    private String remarks;

    private Long durationInSeconds;

    public FieldVisitDTO() {
    }

	public FieldVisitDTO(Long id,Long employeeId, String employeeName, VisitType visitType,
			String customerReference, Instant startTime, Instant endTime,
			Double startLatitude,
			Double startLongitude,
			Double endLatitude,
			Double endLongitude, VisitStatus status,
			String remarks, Long durationInSeconds) {
		super();
		this.id = id;
		this.employeeId = employeeId;
		this.employeeName = employeeName;
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
		this.durationInSeconds = durationInSeconds;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Long getDurationInSeconds() {
		return durationInSeconds;
	}

	public void setDurationInSeconds(Long durationInSeconds) {
		this.durationInSeconds = durationInSeconds;
	}
}
