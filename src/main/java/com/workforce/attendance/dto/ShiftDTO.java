package com.workforce.attendance.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ShiftDTO {

    private Long id;

    @NotBlank(message = "Shift name is required")
    @Size(max = 100, message = "Shift name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @Min(value = 0, message = "Grace period cannot be negative")
    private int lateArrivalGraceMinutes;

    private boolean active;

    public ShiftDTO() {
    }

    public ShiftDTO(Long id, String name, LocalTime startTime,
            LocalTime endTime, int lateArrivalGraceMinutes, boolean active) {

        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lateArrivalGraceMinutes = lateArrivalGraceMinutes;
        this.active = active;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public int getLateArrivalGraceMinutes() {
		return lateArrivalGraceMinutes;
	}

	public void setLateArrivalGraceMinutes(int lateArrivalGraceMinutes) {
		this.lateArrivalGraceMinutes = lateArrivalGraceMinutes;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}