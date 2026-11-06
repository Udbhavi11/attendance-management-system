
package com.workforce.attendance.service;

import java.util.List;

import com.workforce.attendance.dto.AnomalyFlagDTO;
import com.workforce.attendance.entity.AnomalySeverity;
import com.workforce.attendance.entity.AttendanceEventType;
import com.workforce.attendance.entity.Employee;

public interface AnomalyFlagService {

    List<AnomalyFlagDTO> getAllAnomalies();

    List<AnomalyFlagDTO> getEmployeeAnomalies(Long employeeId);

    // Returns pending anomalies belonging to employees assigned to this manager.
    List<AnomalyFlagDTO> getPendingAnomaliesForManager(Long managerId);

    // Records the manager's review outcome and changes status to REVIEWED.
    AnomalyFlagDTO reviewAnomaly(
            Long anomalyId,
            Long managerId,
            String reviewOutcome,
            String correlationId);

    // Saves suspicious location evidence independently of attendance transactions.
    void recordLocationAnomaly(
            Employee employee,
            AttendanceEventType eventType,
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            String ruleIdentifier,
            AnomalySeverity severity);
}
