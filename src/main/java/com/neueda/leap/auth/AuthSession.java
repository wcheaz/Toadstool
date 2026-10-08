package com.neueda.leap.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuthSession {

    private UUID sessionKeyId;
    private UUID credentialId;
    private UUID accountId;
    private OffsetDateTime issuedAt;
    private OffsetDateTime accessTokenExpiresAt;
    private OffsetDateTime revokedAt;
    private OffsetDateTime lastUsedAt;

    public UUID getSessionKeyId() {
        return sessionKeyId;
    }

    public void setSessionKeyId(UUID sessionKeyId) {
        this.sessionKeyId = sessionKeyId;
    }

    public UUID getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(UUID credentialId) {
        this.credentialId = credentialId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(OffsetDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public OffsetDateTime getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(OffsetDateTime accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(OffsetDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
