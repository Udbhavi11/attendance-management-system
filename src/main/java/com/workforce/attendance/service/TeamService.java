
package com.workforce.attendance.service;

import java.util.List;

import com.workforce.attendance.dto.TeamDTO;
import com.workforce.attendance.dto.EmployeeDTO;

public interface TeamService {

    // Admin operations
    TeamDTO createTeam(TeamDTO teamDTO);

    List<TeamDTO> getAllTeams();

    TeamDTO getTeamById(Long id);

    TeamDTO updateTeam(Long id, TeamDTO teamDTO);

    void deleteTeam(Long id);

    // Manager operations
    EmployeeDTO addTeamMember(Long managerId, Long employeeId);

    List<EmployeeDTO> getMyTeamMembers(Long managerId);
}
