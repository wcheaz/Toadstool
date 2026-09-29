package com.neueda.leap.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.neueda.leap.auth.SessionKeyService;

/**
 * Spring Security configuration for JWT-based authentication.
 * Configures:
 * - Stateless session (no session cookies)
 * - JWT filter to validate tokens on every request
 * - Public endpoints that don't require authentication
 * - Protected endpoints that require valid JWT
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private final JwtTokenValidator jwtTokenValidator;
    private final SessionKeyService sessionKeyService;
    private final boolean jwtEnabled;

    public SecurityConfig(JwtTokenValidator jwtTokenValidator, SessionKeyService sessionKeyService, JwtProperties jwtProperties) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.sessionKeyService = sessionKeyService;
        this.jwtEnabled = jwtProperties.isEnabled();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless API
                .csrf(csrf -> csrf.disable())
                // Stateless session management (no cookies)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Handle authentication errors
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"" + accessDeniedException.getMessage() + "\"}");
                        })
                );

        if (!jwtEnabled) {
            http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/api/health", "/api/health/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Require authentication for every other API endpoint
                        .anyRequest().authenticated()
                )
                // Add JWT filter before username/password auth filter
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenValidator, sessionKeyService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
