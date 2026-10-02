package com.neueda.leap.security;

import java.time.Instant;

/**
 * Represents a validated JWT token with extracted claims.
 */
public class ValidatedToken {
    private final boolean authenticated;
    private final String subject;
    private final String username;
    private final String email;
    private final String roles;
    private final Instant issuedAt;
    private final String tokenId;
    private final String tokenType;
    private final String clientId;
    private final String accountId;

    private ValidatedToken(boolean authenticated, String subject, String username, String email, String roles, Instant issuedAt,
                           String tokenId, String tokenType, String clientId, String accountId) {
        this.authenticated = authenticated;
        this.subject = subject;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.issuedAt = issuedAt;
        this.tokenId = tokenId;
        this.tokenType = tokenType;
        this.clientId = clientId;
        this.accountId = accountId;
    }

    public static ValidatedToken authenticated(String subject, String username, String email, String roles, Instant issuedAt) {
        return new ValidatedToken(true, subject, username, email, roles, issuedAt, null, null, null, null);
    }

    public static ValidatedToken authenticated(String subject, String username, String email, String roles, Instant issuedAt,
                                               String tokenId, String tokenType, String clientId, String accountId) {
        return new ValidatedToken(true, subject, username, email, roles, issuedAt, tokenId, tokenType, clientId, accountId);
    }

    public static ValidatedToken unauthenticated() {
        return new ValidatedToken(false, null, null, null, null, null, null, null, null, null);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getSubject() {
        return subject;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRoles() {
        return roles;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAccountId() {
        return accountId;
    }
}
