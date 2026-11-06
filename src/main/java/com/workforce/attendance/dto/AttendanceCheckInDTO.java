
package com.workforce.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AttendanceCheckInDTO {

    @NotNull(message = "Employee ID is required")
    @Positive(message = "Employee ID must be positive")
    private Long employeeId;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Double locationAccuracyMeters;

    public AttendanceCheckInDTO() {
    }
    
    
    public AttendanceCheckInDTO(Long employeeId, Double latitude, Double longitude, Double locationAccuracyMeters) {
		super();
		this.employeeId = employeeId;
		this.latitude = latitude;
		this.longitude = longitude;
		this.locationAccuracyMeters = locationAccuracyMeters;
	}


	public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLocationAccuracyMeters() {
        return locationAccuracyMeters;
    }

    public void setLocationAccuracyMeters(Double locationAccuracyMeters) {
        this.locationAccuracyMeters = locationAccuracyMeters;
    }

	@Override
	public String toString() {
		return "AttendanceCheckInDTO [employeeId=" + employeeId + ", latitude=" + latitude + ", longitude=" + longitude
				+ ", locationAccuracyMeters=" + locationAccuracyMeters + "]";
	}
    
    
}
