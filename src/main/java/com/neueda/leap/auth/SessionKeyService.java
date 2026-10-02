package com.neueda.leap.auth;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SessionKeyService {

    private final AuthSessionMapper authSessionMapper;

    public SessionKeyService(AuthSessionMapper authSessionMapper) {
        this.authSessionMapper = authSessionMapper;
    }

    public void createSession(UUID sessionKeyId, UUID credentialId, UUID accountId,
                              OffsetDateTime issuedAt, OffsetDateTime accessTokenExpiresAt) {
        authSessionMapper.insertSession(
                sessionKeyId,
                credentialId,
                accountId,
                issuedAt,
                accessTokenExpiresAt,
                issuedAt
        );
    }

    public boolean isActiveSession(String sessionKeyId) {
        if (sessionKeyId == null || sessionKeyId.isBlank()) {
            return false;
        }
        AuthSession session = authSessionMapper.selectBySessionKeyId(UUID.fromString(sessionKeyId));
        return session != null
                && session.getRevokedAt() == null
                && session.getAccessTokenExpiresAt() != null
                && session.getAccessTokenExpiresAt().isAfter(OffsetDateTime.now());
    }

    public boolean revokeSession(UUID sessionKeyId) {
        return authSessionMapper.revokeSession(sessionKeyId) > 0;
    }
}
