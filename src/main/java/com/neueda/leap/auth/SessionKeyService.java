package com.neueda.leap.auth;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class SessionKeyService {

    private final AuthSessionMapper authSessionMapper;

    public SessionKeyService(AuthSessionMapper authSessionMapper) {
        this.authSessionMapper = authSessionMapper;
    }

    public void createSession(UUID sessionKeyId, UUID credentialId, UUID accountId, String refreshToken,
                              OffsetDateTime issuedAt, OffsetDateTime accessTokenExpiresAt,
                              OffsetDateTime refreshTokenExpiresAt) {
        authSessionMapper.insertSession(
                sessionKeyId,
                credentialId,
                accountId,
                hashToken(refreshToken),
                issuedAt,
                accessTokenExpiresAt,
                refreshTokenExpiresAt,
                issuedAt
        );
    }

    public boolean isActiveSession(String sessionKeyId) {
        if (sessionKeyId == null || sessionKeyId.isBlank()) {
            return false;
        }
        AuthSession session = authSessionMapper.selectBySessionKeyId(UUID.fromString(sessionKeyId));
        return session != null && session.getRevokedAt() == null;
    }

    public AuthSession findActiveSessionByRefreshToken(String refreshToken) {
        return authSessionMapper.selectActiveByRefreshTokenHash(hashToken(refreshToken));
    }

    public void revokeSession(UUID sessionKeyId) {
        authSessionMapper.revokeSession(sessionKeyId);
    }

    public void touchSession(UUID sessionKeyId) {
        authSessionMapper.touchSession(sessionKeyId);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for session token hashing", e);
        }
    }
}
