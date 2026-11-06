
package com.workforce.attendance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BreakPolicyDTO {

    private Long id;

    @NotBlank(message = "Policy name is required")
    @Size(max = 100, message = "Policy name cannot exceed 100 characters")
    private String name;

    @Min(value = 1, message = "Maximum breaks must be at least 1")
    private int maxBreaksPerShift;

    @Min(value = 1, message = "Maximum duration per break must be at least 1 minute")
    private int maxDurationPerBreakMinutes;

    @Min(value = 1, message = "Maximum total break duration must be at least 1 minute")
    private int maxTotalBreakMinutes;

    private boolean active;

    public BreakPolicyDTO() {
    }

    public BreakPolicyDTO(Long id, String name,
            int maxBreaksPerShift,
            int maxDurationPerBreakMinutes,
            int maxTotalBreakMinutes,
            boolean active) {

        this.id = id;
        this.name = name;
        this.maxBreaksPerShift = maxBreaksPerShift;
        this.maxDurationPerBreakMinutes = maxDurationPerBreakMinutes;
        this.maxTotalBreakMinutes = maxTotalBreakMinutes;
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

    public int getMaxBreaksPerShift() {
        return maxBreaksPerShift;
    }

    public void setMaxBreaksPerShift(int maxBreaksPerShift) {
        this.maxBreaksPerShift = maxBreaksPerShift;
    }

    public int getMaxDurationPerBreakMinutes() {
        return maxDurationPerBreakMinutes;
    }

    public void setMaxDurationPerBreakMinutes(int maxDurationPerBreakMinutes) {
        this.maxDurationPerBreakMinutes = maxDurationPerBreakMinutes;
    }

    public int getMaxTotalBreakMinutes() {
        return maxTotalBreakMinutes;
    }

    public void setMaxTotalBreakMinutes(int maxTotalBreakMinutes) {
        this.maxTotalBreakMinutes = maxTotalBreakMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
