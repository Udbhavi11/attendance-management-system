
package com.workforce.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "break_policies")
public class BreakPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Min(1)
    @Column(nullable = false)
    private int maxBreaksPerShift;

    @Min(1)
    @Column(nullable = false)
    private int maxDurationPerBreakMinutes;

    @Min(1)
    @Column(nullable = false)
    private int maxTotalBreakMinutes;

    @Column(nullable = false)
    private boolean active = true;

	public BreakPolicy() {
		super();
	}
	
	public BreakPolicy(Long id, @NotBlank @Size(max = 100) String name, @Min(1) int maxBreaksPerShift,
			@Min(1) int maxDurationPerBreakMinutes, @Min(1) int maxTotalBreakMinutes, boolean active) {
		super();
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
