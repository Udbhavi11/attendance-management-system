
package com.workforce.attendance.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.dto.EmployeeDTO;
import com.workforce.attendance.dto.TeamDTO;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.Team;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.exception.DuplicateResourceException;
import com.workforce.attendance.exception.ResourceNotFoundException;
import com.workforce.attendance.repository.EmployeeRepository;
import com.workforce.attendance.repository.TeamRepository;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.TeamService;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

	private final TeamRepository teamRepository;
	private final EmployeeRepository employeeRepository;
	private final UserAccountRepository userAccountRepository;

	public TeamServiceImpl(
			TeamRepository teamRepository,
			EmployeeRepository employeeRepository,
			UserAccountRepository userAccountRepository) {

		this.teamRepository = teamRepository;
		this.employeeRepository = employeeRepository;
		this.userAccountRepository = userAccountRepository;
	}

	// CREATE TEAM - ADMIN

	@Override
	public TeamDTO createTeam(TeamDTO teamDTO) {

		String teamName = teamDTO.getName().trim();

		if (teamRepository.existsByName(teamName)) {
			throw new DuplicateResourceException(
					"A team with this name already exists");
		}

		Employee manager = validateManager(teamDTO.getManagerId());

		Team team = new Team();

		team.setName(teamName);
		team.setDescription(teamDTO.getDescription());
		team.setManager(manager);

		Team savedTeam = teamRepository.save(team);

		return convertToDTO(savedTeam);
	}

	// GET ALL TEAMS - ADMIN

	@Override
	@Transactional(readOnly = true)
	public List<TeamDTO> getAllTeams() {

		return teamRepository.findAll()
				.stream()
				.map(this::convertToDTO)
				.toList();
	}

	// GET TEAM BY ID - ADMIN

	@Override
	@Transactional(readOnly = true)
	public TeamDTO getTeamById(Long id) {

		Team team = getTeamEntity(id);

		return convertToDTO(team);
	}

	// UPDATE TEAM - ADMIN

	@Override
	public TeamDTO updateTeam(Long id, TeamDTO teamDTO) {

		Team team = getTeamEntity(id);

		String teamName = teamDTO.getName().trim();

		if (teamRepository.existsByNameAndIdNot(teamName, id)) {
			throw new DuplicateResourceException(
					"Another team with this name already exists");
		}

		Employee manager = validateManager(teamDTO.getManagerId());

		team.setName(teamName);
		team.setDescription(teamDTO.getDescription());
		team.setManager(manager);

		Team updatedTeam = teamRepository.save(team);

		return convertToDTO(updatedTeam);
	}

	// DELETE TEAM - ADMIN

	@Override
	public void deleteTeam(Long id) {

		Team team = getTeamEntity(id);

		if (employeeRepository.existsByTeam_Id(id)) {
			throw new IllegalArgumentException(
					"Cannot delete this team because employees are assigned to it");
		}

		teamRepository.delete(team);
	}


	// ADD EMPLOYEE TO MANAGER'S TEAM

	@Override
	public EmployeeDTO addTeamMember(Long managerId, Long employeeId) {

		Team team = teamRepository.findByManager_Id(managerId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"No team assigned to manager ID: " + managerId));

		Employee manager = team.getManager();

		if (manager == null || !manager.isActive()) {
			throw new IllegalArgumentException(
					"Manager is inactive or unavailable");
		}

		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Employee not found with ID: " + employeeId));

		if (!employee.isActive()) {
			throw new IllegalArgumentException(
					"Cannot add an inactive employee to the team");
		}

		if (employee.getId().equals(managerId)) {
			throw new IllegalArgumentException(
					"Manager cannot be added as their own team member");
		}

		if (employee.getTeam() != null) {
			throw new IllegalArgumentException(
					"Employee already belongs to another team");
		}

		employee.setTeam(team);
		employee.setManager(manager);

		Employee savedEmployee = employeeRepository.save(employee);

		return convertEmployeeToDTO(savedEmployee);
	}

	// GET MANAGER'S TEAM MEMBERS

	@Override
	@Transactional(readOnly = true)
	public List<EmployeeDTO> getMyTeamMembers(Long managerId) {

		Team team = teamRepository.findByManager_Id(managerId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"No team assigned to manager ID: " + managerId));

		return employeeRepository.findByTeam_Id(team.getId())
				.stream()
				.map(this::convertEmployeeToDTO)
				.toList();
	}

	// VALIDATE MANAGER

	private Employee validateManager(Long managerId) {

		if (managerId == null) {
			throw new IllegalArgumentException(
					"Manager ID is required");
		}

		Employee manager = employeeRepository.findById(managerId)
				.orElseThrow(() ->
				new ResourceNotFoundException(
						"Employee not found with ID: " + managerId));

		if (!manager.isActive()) {
			throw new IllegalArgumentException(
					"Inactive employee cannot be assigned as manager");
		}

		UserAccount account = userAccountRepository
				.findByEmployeeId(managerId)
				.orElseThrow(() ->
				new IllegalArgumentException(
						"Selected manager does not have a user account"));

		if (account.getRole() == null
				|| !"MANAGER".equalsIgnoreCase(
						account.getRole().getName())) {

			throw new IllegalArgumentException(
					"Selected employee does not have MANAGER role");
		}

		return manager;
	}

	// GET TEAM ENTITY

	private Team getTeamEntity(Long id) {

		return teamRepository.findById(id)
				.orElseThrow(() ->
				new ResourceNotFoundException(
						"Team not found with ID: " + id));
	}
	// CONVERT EMPLOYEE ENTITY TO DTO

	private EmployeeDTO convertEmployeeToDTO(Employee employee) {

		return new EmployeeDTO(
				employee.getId(),
				employee.getEmployeeCode(),
				employee.getName(),
				employee.getEmail(),
				employee.getMobileNumber(),
				employee.getDepartment(),
				employee.isActive(),
				employee.getManager() != null
				? employee.getManager().getId() : null,
						employee.getAssignedShift() != null
						? employee.getAssignedShift().getId() : null,
								employee.getAssignedOffice() != null
								? employee.getAssignedOffice().getId() : null,
										employee.getAssignmentType()
				);
	}
	
	// CONVERT TEAM ENTITY TO DTO

	private TeamDTO convertToDTO(Team team) {

	    return new TeamDTO(
	            team.getId(),
	            team.getName(),
	            team.getDescription(),
	            team.getManager() != null
	                    ? team.getManager().getId() : null,
	            team.getManager() != null
	                    ? team.getManager().getName() : null
	    );
	}

}
