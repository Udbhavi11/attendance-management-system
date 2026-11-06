package com.workforce.attendance.service;

import java.time.LocalDate;
import java.util.List;

import com.workforce.attendance.dto.FieldVisitDTO;

public interface FieldVisitService {

    FieldVisitDTO startVisit(FieldVisitDTO dto);

    FieldVisitDTO completeVisit(Long visitId, FieldVisitDTO dto);

    FieldVisitDTO getVisitById(Long visitId);

    List<FieldVisitDTO> getAllVisits();

    List<FieldVisitDTO> getEmployeeVisits(Long employeeId);

    List<FieldVisitDTO> getEmployeeDailyVisits(
            Long employeeId, LocalDate date);

    List<FieldVisitDTO> getTeamVisits(Long managerId, LocalDate date);

    FieldVisitDTO correctVisit(
            Long visitId,
            FieldVisitDTO dto,
            String actorUsername,
            String correlationId);
}