
package com.workforce.attendance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "break_records")
@Getter
@Setter
@NoArgsConstructor
public class BreakRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_session_id", nullable = false)
    private AttendanceSession attendanceSession;

    @Column(nullable = false)
    private Instant startTime;

    private Instant endTime;

	public BreakRecord() {
		super();
	}
	
	public BreakRecord(Long id, AttendanceSession attendanceSession, Instant startTime, Instant endTime) {
		super();
		this.id = id;
		this.attendanceSession = attendanceSession;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public AttendanceSession getAttendanceSession() {
		return attendanceSession;
	}

	public void setAttendanceSession(AttendanceSession attendanceSession) {
		this.attendanceSession = attendanceSession;
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
}
