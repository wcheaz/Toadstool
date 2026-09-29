package com.neueda.leap.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
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
    private PublicKey verificationKey;

    public JwtTokenValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void validateConfiguration() {
        if (jwtProperties.isEnabled() && !StringUtils.hasText(jwtProperties.getPublicKey())) {
            throw new IllegalStateException("auth.jwt.public-key must be configured when JWT authentication is enabled");
        }
        if (jwtProperties.isEnabled()) {
            verificationKey = JwtKeySupport.loadPublicKey(jwtProperties.getPublicKey());
        }
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
            Date issuedAt = claims.getIssuedAt();
            if (!StringUtils.hasText(subject)) {
                throw new JwtException("JWT subject claim is required");
            }
            if (issuedAt == null) {
                throw new JwtException("JWT issued-at claim is required");
            }

            return ValidatedToken.authenticated(
                    subject,
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
