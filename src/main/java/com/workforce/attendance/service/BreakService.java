
package com.workforce.attendance.service;

import com.workforce.attendance.dto.BreakDTO;

public interface BreakService {

    BreakDTO startBreak(Long employeeId);

    BreakDTO endBreak(Long employeeId);
    
    String getTotalBreakDuration(Long attendanceSessionId);

    String getNetWorkingDuration(Long attendanceSessionId);
    
    public void detectForgottenBreaks();

}
