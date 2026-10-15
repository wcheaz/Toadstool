package com.neueda.leap.auth;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SessionKeyService {

    private final AuthSessionMapper authSessionMapper;
    private final AdminAuthSessionMapper adminAuthSessionMapper;

    public SessionKeyService(AuthSessionMapper authSessionMapper, AdminAuthSessionMapper adminAuthSessionMapper) {
        this.authSessionMapper = authSessionMapper;
        this.adminAuthSessionMapper = adminAuthSessionMapper;
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

    public void createAdminSession(UUID sessionKeyId, UUID credentialId, UUID adminUserId,
                                   OffsetDateTime issuedAt, OffsetDateTime accessTokenExpiresAt) {
        adminAuthSessionMapper.insertSession(
                sessionKeyId,
                credentialId,
                adminUserId,
                issuedAt,
                accessTokenExpiresAt,
                issuedAt
        );
    }

    /**
     * Session keys are random UUIDs, so a key lives in exactly one of the client or admin
     * session tables; check the client table first, then the admin table.
     */
    public boolean isActiveSession(String sessionKeyId) {
        if (sessionKeyId == null || sessionKeyId.isBlank()) {
            return false;
        }
        UUID key = UUID.fromString(sessionKeyId);
        AuthSession session = authSessionMapper.selectBySessionKeyId(key);
        if (session == null) {
            session = adminAuthSessionMapper.selectBySessionKeyId(key);
        }
        return session != null
                && session.getRevokedAt() == null
                && session.getAccessTokenExpiresAt() != null
                && session.getAccessTokenExpiresAt().isAfter(OffsetDateTime.now());
    }

    public boolean revokeSession(UUID sessionKeyId) {
        return authSessionMapper.revokeSession(sessionKeyId) > 0
                || adminAuthSessionMapper.revokeSession(sessionKeyId) > 0;
    }
}
