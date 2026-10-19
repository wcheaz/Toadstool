package com.neueda.leap.auth;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class SessionKeyService {

    private final AuthSessionMapper authSessionMapper;

    public SessionKeyService(AuthSessionMapper authSessionMapper) {
        this.authSessionMapper = authSessionMapper;
    }

    public void createSession(UUID sessionKeyId, UUID credentialId, UUID clientId,
                              OffsetDateTime issuedAt, OffsetDateTime accessTokenExpiresAt,
                              String refreshTokenHash, OffsetDateTime refreshTokenExpiresAt) {
        authSessionMapper.insertSession(
                sessionKeyId,
                credentialId,
                clientId,
                issuedAt,
                accessTokenExpiresAt,
                refreshTokenHash,
                refreshTokenExpiresAt,
                issuedAt
        );
    }

    public boolean isActiveSession(String sessionKeyId) {
        if (sessionKeyId == null || sessionKeyId.isBlank()) {
            return false;
        }
        UUID sessionId = UUID.fromString(sessionKeyId);
        AuthSession session = authSessionMapper.selectActiveBySessionKeyId(sessionId);
        return session != null
                && session.getAccessTokenExpiresAt() != null
                && revokeIfExpired(sessionId, session);
    }

    public boolean revokeSession(UUID sessionKeyId) {
        return authSessionMapper.revokeSession(sessionKeyId) > 0;
    }

    public boolean extendSession(UUID sessionKeyId, OffsetDateTime accessTokenExpiresAt) {
        return authSessionMapper.extendSession(sessionKeyId, accessTokenExpiresAt) > 0;
    }

    public int revokeActiveSessionsByCredentialId(UUID credentialId) {
        return authSessionMapper.revokeActiveSessionsByCredentialId(credentialId);
    }

    public int revokeExpiredSessions() {
        return authSessionMapper.revokeExpiredSessions(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private boolean revokeIfExpired(UUID sessionId, AuthSession session) {
        if (session.getAccessTokenExpiresAt().isAfter(OffsetDateTime.now())) {
            return true;
        }
        revokeSession(sessionId);
        return false;
    }

    public AuthSession getActiveSessionByRefreshTokenHash(String refreshTokenHash) {
        if (refreshTokenHash == null || refreshTokenHash.isBlank()) {
            return null;
        }
        AuthSession session = authSessionMapper.selectActiveByRefreshTokenHash(refreshTokenHash);
        if (session == null || session.getRefreshTokenExpiresAt() == null) {
            return null;
        }
        return session.getRefreshTokenExpiresAt().isAfter(OffsetDateTime.now()) ? session : null;
    }

    public boolean rotateSessionTokens(UUID sessionKeyId, OffsetDateTime accessTokenExpiresAt,
                                       String refreshTokenHash, OffsetDateTime refreshTokenExpiresAt) {
        return authSessionMapper.rotateSessionTokens(sessionKeyId, accessTokenExpiresAt, refreshTokenHash, refreshTokenExpiresAt) > 0;
    }
}
