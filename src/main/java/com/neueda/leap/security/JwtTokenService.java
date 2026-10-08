package com.neueda.leap.security;

import io.jsonwebtoken.Jwts;
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
        if (!jwtProperties.isEnabled()) {
            return IssuedAccessToken.disabled();
        }

        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(Duration.ofMinutes(jwtProperties.getAccessTokenTtlMinutes()));

        String accessToken = buildToken(sessionKeyId, clientId, accountId, username, email, role,
                "ACCESS", now, accessExpiresAt);

        return new IssuedAccessToken(accessToken, accessExpiresAt);
    }

    private String buildToken(UUID sessionKeyId, UUID clientId, UUID accountId, String username, String email,
                              String role, String tokenType, Instant issuedAt, Instant expiresAt) {
        if (signingKey == null) {
            throw new IllegalStateException("JWT signing is disabled");
        }

        return Jwts.builder()
                .setId(sessionKeyId.toString())
                .setSubject(clientId.toString())
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .claim("username", username)
                .claim("clientId", clientId.toString())
                .claim("accountId", accountId.toString())
                .claim("email", email)
                .claim("roles", "ROLE_" + role)
                .claim("tokenType", tokenType)
                .signWith(signingKey, JwtKeySupport.signatureAlgorithm(signingKey))
                .compact();
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
}
