
package com.workforce.attendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "employee_rosters")
public class EmployeeRoster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(nullable = false)
    private LocalDate rosterDate;
    

	@Column(nullable = false)
	private boolean weeklyOff = false;


	public EmployeeRoster() {
		super();
	}
	
	public EmployeeRoster(Long id, Employee employee, Shift shift, LocalDate rosterDate, boolean weeklyOff) {
		super();
		this.id = id;
		this.employee = employee;
		this.shift = shift;
		this.rosterDate = rosterDate;
		this.weeklyOff = weeklyOff;
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

	public Shift getShift() {
		return shift;
	}

	public void setShift(Shift shift) {
		this.shift = shift;
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
