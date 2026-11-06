package com.workforce.attendance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.workforce.attendance.dto.EmployeeDTO;
import com.workforce.attendance.dto.TeamDTO;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.UserAccountRepository;
import com.workforce.attendance.service.TeamService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class TeamController {

    private final TeamService teamService;
    private final UserAccountRepository userAccountRepository;

    public TeamController(
            TeamService teamService,
            UserAccountRepository userAccountRepository) {

        this.teamService = teamService;
        this.userAccountRepository = userAccountRepository;
    }

    // ADMIN - CREATE TEAM
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/teams")
    public ResponseEntity<TeamDTO> createTeam(
            @Valid @RequestBody TeamDTO teamDTO) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createTeam(teamDTO));
    }

    // ADMIN - GET ALL TEAMS
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/teams")
    public ResponseEntity<List<TeamDTO>> getAllTeams() {

        return ResponseEntity.ok(teamService.getAllTeams());
    }

    // ADMIN - GET TEAM BY ID
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/teams/{id}")
    public ResponseEntity<TeamDTO> getTeamById(
            @PathVariable Long id) {

        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    // ADMIN - UPDATE TEAM
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/teams/{id}")
    public ResponseEntity<TeamDTO> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody TeamDTO teamDTO) {

        return ResponseEntity.ok(
                teamService.updateTeam(id, teamDTO));
    }

    // ADMIN - DELETE TEAM
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/teams/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id) {

        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    // MANAGER - ADD TEAM MEMBER
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/manager/my-team/members/{employeeId}")
    public ResponseEntity<EmployeeDTO> addTeamMember(
            @PathVariable Long employeeId,
            Authentication authentication) {

        Long managerId = getAuthenticatedManagerId(authentication);

        EmployeeDTO employeeDTO =
                teamService.addTeamMember(managerId, employeeId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeDTO);
    }

    // MANAGER - VIEW OWN TEAM MEMBERS
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/manager/my-team/members")
    public ResponseEntity<List<EmployeeDTO>> getMyTeamMembers(
            Authentication authentication) {

        Long managerId = getAuthenticatedManagerId(authentication);

        return ResponseEntity.ok(
                teamService.getMyTeamMembers(managerId));
    }

    // GET AUTHENTICATED MANAGER'S EMPLOYEE ID
    private Long getAuthenticatedManagerId(
            Authentication authentication) {

        UserAccount account = userAccountRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new AccessDeniedException("User account not found"));

        if (account.getEmployee() == null) {
            throw new AccessDeniedException(
                    "No employee record is associated with this manager account");
        }

        return account.getEmployee().getId();
    }
}