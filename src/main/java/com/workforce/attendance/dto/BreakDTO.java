
package com.workforce.attendance.dto;

import java.time.Instant;

import com.workforce.attendance.entity.AttendanceStatus;

public class BreakDTO {

    private Long id;
    private Long attendanceSessionId;
    private Instant startTime;
    private Instant endTime;
    private Long durationInSeconds;
    private AttendanceStatus attendanceStatus;

    public BreakDTO(Long id, Long attendanceSessionId,
            Instant startTime, Instant endTime, Long durationInSeconds,AttendanceStatus attendanceStatus) {
        this.id = id;
        this.attendanceSessionId = attendanceSessionId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationInSeconds = durationInSeconds;
        this.setAttendanceStatus(attendanceStatus);
    }

    public Long getId() {
        return id;
    }

    public Long getAttendanceSessionId() {
        return attendanceSessionId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Long getDurationInSeconds() {
        return durationInSeconds;
    }

	public void setId(Long id) {
		this.id = id;
	}

	public void setAttendanceSessionId(Long attendanceSessionId) {
		this.attendanceSessionId = attendanceSessionId;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public void setDurationInSeconds(Long durationInSeconds) {
		this.durationInSeconds = durationInSeconds;
	}

	public AttendanceStatus getAttendanceStatus() {
		return attendanceStatus;
	}

	public void setAttendanceStatus(AttendanceStatus attendanceStatus) {
		this.attendanceStatus = attendanceStatus;
	}
}
