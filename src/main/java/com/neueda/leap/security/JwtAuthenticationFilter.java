package com.neueda.leap.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter that intercepts all requests and validates JWT tokens.
 * Expects bearer token in Authorization header: "Authorization: Bearer <token>"
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator tokenValidator;
    private final com.neueda.leap.auth.SessionKeyService sessionKeyService;

    public JwtAuthenticationFilter(JwtTokenValidator tokenValidator, com.neueda.leap.auth.SessionKeyService sessionKeyService) {
        this.tokenValidator = tokenValidator;
        this.sessionKeyService = sessionKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = extractBearerToken(request);
            if (StringUtils.hasText(token)) {
                ValidatedToken validatedToken = tokenValidator.validateToken(token);
                if (validatedToken.isAuthenticated() && isAccessToken(validatedToken)) {
                    if (StringUtils.hasText(validatedToken.getTokenId())
                            && !sessionKeyService.isActiveSession(validatedToken.getTokenId())) {
                        throw new JwtException("JWT session is no longer active");
                    }
                    Authentication authentication = createAuthentication(validatedToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else if (validatedToken.isAuthenticated() && "REFRESH".equalsIgnoreCase(validatedToken.getTokenType())) {
                    throw new JwtException("Refresh token cannot be used for API authentication");
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("JWT validation failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the Bearer token from the Authorization header.
     * @param request the HTTP request
     * @return the token string without "Bearer " prefix, or null if not present
     */
    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Creates a Spring Security Authentication object from the validated token.
     * @param validatedToken the validated JWT token
     * @return Authentication object with user details and authorities
     */
    private Authentication createAuthentication(ValidatedToken validatedToken) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (StringUtils.hasText(validatedToken.getRoles())) {
            // Roles are comma-separated in the JWT, e.g., "ROLE_ADMIN,ROLE_USER"
            String[] roleArray = validatedToken.getRoles().split(",");
            for (String role : roleArray) {
                authorities.add(new SimpleGrantedAuthority(role.trim()));
            }
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                validatedToken.getSubject(),
                validatedToken.getUsername(),
                validatedToken.getEmail(),
                parseUuid(validatedToken.getClientId()),
                parseUuid(validatedToken.getAccountId())
        );

        return new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                authorities
        );
    }

    private boolean isAccessToken(ValidatedToken validatedToken) {
        return !StringUtils.hasText(validatedToken.getTokenType())
                || "ACCESS".equalsIgnoreCase(validatedToken.getTokenType());
    }

    private UUID parseUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return UUID.fromString(value);
    }
}
