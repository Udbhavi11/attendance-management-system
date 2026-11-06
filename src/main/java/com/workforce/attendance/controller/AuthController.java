
package com.workforce.attendance.controller;

import java.util.Optional;

import com.workforce.attendance.dto.EmployeeDTO;
import com.workforce.attendance.dto.LoginDTO;
import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.UserAccountRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserAccountRepository userAccountRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            UserAccountRepository userAccountRepository) {

        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // -----------------------------------------------------
    // LOGIN
    // -----------------------------------------------------

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginDTO loginDTO,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginDTO.getUsername(),
                                    loginDTO.getPassword()
                            )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context, request, response
            );

            String role = authentication.getAuthorities()
                    .iterator()
                    .next()
                    .getAuthority()
                    .replace("ROLE_", "");

            Long employeeId = null;
            String employeeName = null;

            Optional<UserAccount> accountOptional =
                    userAccountRepository.findAccountWithEmployeeByUsername(
                            authentication.getName()
                    );

            if (accountOptional.isPresent()) {

                Employee employee =
                        accountOptional.get().getEmployee();

                if (employee != null) {
                    employeeId = employee.getId();
                    employeeName = employee.getName();
                }
            }

            LoginDTO responseDTO = new LoginDTO(
                    authentication.getName(),
                    role,
                    "Login successful",
                    employeeId,
                    employeeName
            );

            return ResponseEntity.ok(responseDTO);

        } catch (AuthenticationException ex) {

            SecurityContextHolder.clearContext();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }
    }

    // -----------------------------------------------------
    // GET AUTHENTICATED USER'S PROFILE
    // -----------------------------------------------------

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyProfile(
            Authentication authentication) {

        UserAccount account = userAccountRepository
                .findAccountWithEmployeeByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User account not found"
                ));

        String role = account.getRole() != null
                ? account.getRole().getName()
                : authentication.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
                        .replace("ROLE_", "");

        Employee employee = account.getEmployee();

        // ADMIN / AUDITOR accounts may not have employee profiles.
        if (employee == null) {

            LoginDTO responseDTO = new LoginDTO(
                    account.getUsername(),
                    role,
                    "No employee profile linked to this account",
                    null,
                    null
            );

            return ResponseEntity.ok(responseDTO);
        }

        EmployeeDTO dto = new EmployeeDTO();

        dto.setId(employee.getId());
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setName(employee.getName());
        dto.setEmail(employee.getEmail());
        dto.setMobileNumber(employee.getMobileNumber());
        dto.setDepartment(employee.getDepartment());
        dto.setActive(employee.isActive());
        dto.setAssignmentType(employee.getAssignmentType());

        dto.setManagerId(
                employee.getManager() != null
                        ? employee.getManager().getId()
                        : null
        );

        dto.setShiftId(
                employee.getAssignedShift() != null
                        ? employee.getAssignedShift().getId()
                        : null
        );

        dto.setOfficeId(
                employee.getAssignedOffice() != null
                        ? employee.getAssignedOffice().getId()
                        : null
        );

        dto.setUsername(account.getUsername());
        dto.setSystemRole(role);

        return ResponseEntity.ok(dto);
    }
}
