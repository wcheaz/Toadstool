package com.neueda.leap.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;

/**
 * Validates JWT tokens from external identity service without calling it.
 * Trusts that the token was issued by the external provider and validates:
 * - Token signature using public key
 * - Token expiration
 * - Required claims (subject, issued-at)
 */
@Component
public class JwtTokenValidator {

    private final JwtProperties jwtProperties;
    private final PublicKey verificationKey;

    public JwtTokenValidator(JwtProperties jwtProperties, JwtKeyProvider jwtKeyProvider) {
        this.jwtProperties = jwtProperties;
        this.verificationKey = jwtProperties.isEnabled() ? jwtKeyProvider.getVerificationKey() : null;
    }

    /**
     * Validates a JWT token using the external identity service's public key.
     * @param token The JWT token string
     * @return ValidatedToken containing extracted claims if valid
     * @throws JwtException if token is invalid, expired, or signature doesn't match
     */
    public ValidatedToken validateToken(String token) {
        if (!jwtProperties.isEnabled()) {
            return ValidatedToken.unauthenticated();
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(verificationKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String subject = claims.getSubject();
            String username = claims.get("username", String.class);
            Date issuedAt = claims.getIssuedAt();
            if (!StringUtils.hasText(subject)) {
                throw new JwtException("JWT subject claim is required");
            }
            if (issuedAt == null) {
                throw new JwtException("JWT issued-at claim is required");
            }
            if (!StringUtils.hasText(username)) {
                username = subject;
            }

            return ValidatedToken.authenticated(
                    subject,
                    username,
                    claims.get("email", String.class),
                    claims.get("roles", String.class),
                    toInstant(issuedAt),
                    claims.getId(),
                    claims.get("tokenType", String.class),
                    claims.get("clientId", String.class),
                    claims.get("accountId", String.class)
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid or expired JWT token", e);
        }
    }

    private Instant toInstant(Date value) {
        return value.toInstant();
    }
}
