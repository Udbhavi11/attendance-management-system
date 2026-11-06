package com.workforce.attendance.config;

import java.util.Locale;
import java.util.Set;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workforce.attendance.entity.Employee;
import com.workforce.attendance.entity.UserAccount;
import com.workforce.attendance.repository.UserAccountRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "EMPLOYEE",
            "MANAGER",
            "ADMIN",
            "AUDITOR"
    );

    private final UserAccountRepository userAccountRepository;

    public CustomUserDetailsService(
            UserAccountRepository userAccountRepository) {

        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        UserAccount account = userAccountRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Invalid username or password"));

        // Validate account status
        if (!account.isActive()) {
            throw new UsernameNotFoundException(
                    "Account is inactive");
        }

        // Validate role assignment
        if (account.getRole() == null
                || account.getRole().getName() == null) {

            throw new UsernameNotFoundException(
                    "User role is not assigned");
        }

        String roleName = account.getRole()
                .getName()
                .trim()
                .toUpperCase(Locale.ROOT);

        // Only four configured roles are accepted
        if (!ALLOWED_ROLES.contains(roleName)) {
            throw new UsernameNotFoundException(
                    "Invalid user role");
        }

        Employee employee = account.getEmployee();

        // Employee and Manager accounts must have an employee profile
        if ("EMPLOYEE".equals(roleName)
                || "MANAGER".equals(roleName)) {

            if (employee == null) {
                throw new UsernameNotFoundException(
                        "Employee profile is not linked to this account");
            }

            // Inactive employees cannot authenticate
            if (!employee.isActive()) {
                throw new UsernameNotFoundException(
                        "Employee account is inactive");
            }
        }

        String authority = "ROLE_" + roleName;

        return User.builder()
                .username(account.getUsername())
                .password(account.getPassword())
                .authorities(authority)
                .disabled(false)
                .build();
    }
}