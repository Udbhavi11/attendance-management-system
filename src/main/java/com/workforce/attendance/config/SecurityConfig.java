package com.workforce.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Authentication manager
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    // Session-based security context repository
    @Bean
    public SecurityContextRepository securityContextRepository() {

        return new HttpSessionSecurityContextRepository();
    }

    // Security filter chain
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository)
            throws Exception {

        http
            // CSRF disabled for the current REST API setup.
            // Revisit this before production deployment.
            .csrf(csrf -> csrf.disable())

            // Store authentication in HTTP session
            .securityContext(context -> context
                .securityContextRepository(
                        securityContextRepository)
            )

            // Session-based authentication
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                        SessionCreationPolicy.IF_REQUIRED)
            )

            // Public and protected endpoints
            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/",
                    "/index.html",
                    "/css/**",
                    "/js/**",
                    "/favicon.ico",
                    "/api/auth/login"
                ).permitAll()

                .anyRequest().authenticated()
            )

            // Return 401 for unauthenticated API requests
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(
                            HttpStatus.UNAUTHORIZED)
                )
            )

            // Disable default authentication mechanisms
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // Logout configuration
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(
                    (request, response, authentication) ->
                        response.setStatus(
                                HttpStatus.NO_CONTENT.value())
                )
            );

        return http.build();
    }
}