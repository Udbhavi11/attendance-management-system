
package com.workforce.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean active;
    

	@Column(nullable = false)
	private int lateArrivalGraceMinutes = 0;


	public Shift() {
		super();
	}
	
	public Shift(Long id, @NotBlank @Size(max = 100) String name, LocalTime startTime, LocalTime endTime,
			boolean active, int lateArrivalGraceMinutes) {
		super();
		this.id = id;
		this.name = name;
		this.startTime = startTime;
		this.endTime = endTime;
		this.active = active;
		this.lateArrivalGraceMinutes = lateArrivalGraceMinutes;
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getLateArrivalGraceMinutes() {
		return lateArrivalGraceMinutes;
	}

	public void setLateArrivalGraceMinutes(int lateArrivalGraceMinutes) {
		this.lateArrivalGraceMinutes = lateArrivalGraceMinutes;
	}
	

}
