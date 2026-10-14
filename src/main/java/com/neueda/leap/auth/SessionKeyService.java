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
                              OffsetDateTime issuedAt, OffsetDateTime accessTokenExpiresAt) {
        authSessionMapper.insertSession(
                sessionKeyId,
                credentialId,
                clientId,
                issuedAt,
                accessTokenExpiresAt,
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
}
