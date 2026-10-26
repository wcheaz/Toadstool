package com.neueda.leap.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final PrivateKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties, JwtKeyProvider jwtKeyProvider) {
        this.jwtProperties = jwtProperties;
        if (!jwtProperties.isEnabled()) {
            this.signingKey = null;
            return;
        }
        this.signingKey = jwtKeyProvider.getSigningKey();
    }

    public IssuedAccessToken issueAccessToken(UUID sessionKeyId, UUID clientId, UUID accountId,
                                              String username, String email, String role) {
        return issueAccessToken(sessionKeyId, clientId, clientId, accountId, username, email, role);
    }

    public IssuedAccessToken issueAccessToken(UUID sessionKeyId, UUID subjectId, UUID clientId, UUID accountId,
                                              String username, String email, String role) {
        IssuedTokenPair issuedTokens = issueTokenPair(sessionKeyId, subjectId, clientId, accountId, username, email, role);
        return new IssuedAccessToken(issuedTokens.getAccessToken(), issuedTokens.getAccessTokenExpiresAt());
    }

    public IssuedTokenPair issueTokenPair(UUID sessionKeyId, UUID subjectId, UUID clientId, UUID accountId,
                                          String username, String email, String role) {
        if (!jwtProperties.isEnabled()) {
            return IssuedTokenPair.disabled();
        }

        if (subjectId == null) {
            throw new IllegalArgumentException("JWT subject is required");
        }
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("JWT username claim is required");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("JWT email claim is required");
        }
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("JWT role claim is required");
        }

        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(Duration.ofMinutes(jwtProperties.getAccessTokenTtlMinutes()));
        Instant refreshExpiresAt = now.plus(Duration.ofMinutes(jwtProperties.getRefreshTokenTtlMinutes()));

        String accessToken = buildToken(sessionKeyId, subjectId, clientId, accountId, username, email, role,
                "ACCESS", now, accessExpiresAt);
        String refreshToken = buildToken(sessionKeyId, subjectId, clientId, accountId, username, email, role,
                "REFRESH", now, refreshExpiresAt);

        return new IssuedTokenPair(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);
    }

    private String buildToken(UUID sessionKeyId, UUID subjectId, UUID clientId, UUID accountId, String username, String email,
                              String role, String tokenType, Instant issuedAt, Instant expiresAt) {
        if (signingKey == null) {
            throw new IllegalStateException("JWT signing is disabled");
        }

        JwtBuilder jwtBuilder = Jwts.builder()
                .setSubject(subjectId.toString())
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .claim("username", username)
                .claim("email", email)
                .claim("roles", "ROLE_" + role)
                .claim("tokenType", tokenType)
                .signWith(signingKey, JwtKeySupport.signatureAlgorithm(signingKey));

        if (sessionKeyId != null) {
            jwtBuilder.setId(sessionKeyId.toString());
        }
        if (clientId != null) {
            jwtBuilder.claim("clientId", clientId.toString());
        }
        if (accountId != null) {
            jwtBuilder.claim("accountId", accountId.toString());
        }

        return jwtBuilder.compact();
    }

    public static class IssuedAccessToken {
        private final String accessToken;
        private final Instant accessTokenExpiresAt;

        public IssuedAccessToken(String accessToken, Instant accessTokenExpiresAt) {
            this.accessToken = accessToken;
            this.accessTokenExpiresAt = accessTokenExpiresAt;
        }

        public static IssuedAccessToken disabled() {
            return new IssuedAccessToken(null, null);
        }

        public String getAccessToken() {
            return accessToken;
        }

        public Instant getAccessTokenExpiresAt() {
            return accessTokenExpiresAt;
        }
    }

    public static class IssuedTokenPair {
        private final String accessToken;
        private final Instant accessTokenExpiresAt;
        private final String refreshToken;
        private final Instant refreshTokenExpiresAt;

        public IssuedTokenPair(String accessToken, Instant accessTokenExpiresAt, String refreshToken, Instant refreshTokenExpiresAt) {
            this.accessToken = accessToken;
            this.accessTokenExpiresAt = accessTokenExpiresAt;
            this.refreshToken = refreshToken;
            this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        }

        public static IssuedTokenPair disabled() {
            return new IssuedTokenPair(null, null, null, null);
        }

        public String getAccessToken() {
            return accessToken;
        }

        public Instant getAccessTokenExpiresAt() {
            return accessTokenExpiresAt;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public Instant getRefreshTokenExpiresAt() {
            return refreshTokenExpiresAt;
        }
    }
}
