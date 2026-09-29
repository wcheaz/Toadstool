package com.neueda.leap.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final PrivateKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        if (!jwtProperties.isEnabled()) {
            this.signingKey = null;
            return;
        }
        if (jwtProperties.getPrivateKey() == null || jwtProperties.getPrivateKey().isBlank()) {
            throw new IllegalStateException("auth.jwt.private-key must be configured when JWT authentication is enabled");
        }
        this.signingKey = JwtKeySupport.loadPrivateKey(jwtProperties.getPrivateKey());
    }

    public IssuedTokenPair issueTokenPair(UUID sessionKeyId, UUID clientId, UUID accountId,
                                          String username, String email, String role) {
        if (!jwtProperties.isEnabled()) {
            return IssuedTokenPair.disabled();
        }

        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(Duration.ofMinutes(jwtProperties.getAccessTokenTtlMinutes()));
        Instant refreshExpiresAt = now.plus(Duration.ofDays(jwtProperties.getRefreshTokenTtlDays()));

        String accessToken = buildToken(sessionKeyId, clientId, accountId, username, email, role,
                "ACCESS", now, accessExpiresAt);
        String refreshToken = buildToken(sessionKeyId, clientId, accountId, username, email, role,
                "REFRESH", now, refreshExpiresAt);

        return new IssuedTokenPair(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
    }

    private String buildToken(UUID sessionKeyId, UUID clientId, UUID accountId, String username, String email,
                              String role, String tokenType, Instant issuedAt, Instant expiresAt) {
        if (signingKey == null) {
            throw new IllegalStateException("JWT signing is disabled");
        }

        return Jwts.builder()
                .setId(sessionKeyId.toString())
                .setSubject(username)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .claim("clientId", clientId.toString())
                .claim("accountId", accountId.toString())
                .claim("email", email)
                .claim("roles", "ROLE_" + role)
                .claim("tokenType", tokenType)
                .signWith(signingKey, JwtKeySupport.signatureAlgorithm(signingKey))
                .compact();
    }

    public static class IssuedTokenPair {
        private final String accessToken;
        private final String refreshToken;
        private final Instant accessTokenExpiresAt;
        private final Instant refreshTokenExpiresAt;

        public IssuedTokenPair(String accessToken, String refreshToken, Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.accessTokenExpiresAt = accessTokenExpiresAt;
            this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        }

        public static IssuedTokenPair disabled() {
            return new IssuedTokenPair(null, null, null, null);
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public Instant getAccessTokenExpiresAt() {
            return accessTokenExpiresAt;
        }

        public Instant getRefreshTokenExpiresAt() {
            return refreshTokenExpiresAt;
        }
    }
}
