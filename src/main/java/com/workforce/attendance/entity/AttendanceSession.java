
package com.workforce.attendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "attendance_sessions")
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roster_id")
    private EmployeeRoster roster;

    @Column(nullable = false)
    private Instant checkInTime;

    private Instant checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

	public AttendanceSession(Long id, Employee employee, EmployeeRoster roster, Instant checkInTime,
			Instant checkOutTime, AttendanceStatus status) {
		super();
		this.id = id;
		this.employee = employee;
		this.roster = roster;
		this.checkInTime = checkInTime;
		this.checkOutTime = checkOutTime;
		this.status = status;
	}
	public AttendanceSession() {
		super();
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
	public EmployeeRoster getRoster() {
		return roster;
	}
	public void setRoster(EmployeeRoster roster) {
		this.roster = roster;
	}
	public Instant getCheckInTime() {
		return checkInTime;
	}
	public void setCheckInTime(Instant checkInTime) {
		this.checkInTime = checkInTime;
	}
	public Instant getCheckOutTime() {
		return checkOutTime;
	}
	public void setCheckOutTime(Instant checkOutTime) {
		this.checkOutTime = checkOutTime;
	}
	public AttendanceStatus getStatus() {
		return status;
	}
	public void setStatus(AttendanceStatus status) {
		this.status = status;
	}
}
