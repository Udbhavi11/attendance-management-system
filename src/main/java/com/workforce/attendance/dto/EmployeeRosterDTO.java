
package com.workforce.attendance.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class EmployeeRosterDTO {

    private Long id;

    @NotNull(message = "Employee ID is required")
    @Positive(message = "Employee ID must be positive")
    private Long employeeId;

    private Long shiftId;

    @NotNull(message = "Roster date is required")
    private LocalDate rosterDate;

    private boolean weeklyOff;

    public EmployeeRosterDTO() {
    }

    public EmployeeRosterDTO(Long id, Long employeeId,
            Long shiftId, LocalDate rosterDate, boolean weeklyOff) {

        this.id = id;
        this.employeeId = employeeId;
        this.shiftId = shiftId;
        this.rosterDate = rosterDate;
        this.weeklyOff = weeklyOff;
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

	public Long getShiftId() {
		return shiftId;
	}

	public void setShiftId(Long shiftId) {
		this.shiftId = shiftId;
	}

	public LocalDate getRosterDate() {
		return rosterDate;
	}

	public void setRosterDate(LocalDate rosterDate) {
		this.rosterDate = rosterDate;
	}

	public boolean isWeeklyOff() {
		return weeklyOff;
	}

	public void setWeeklyOff(boolean weeklyOff) {
		this.weeklyOff = weeklyOff;
	}
    
}
