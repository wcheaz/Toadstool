package com.neueda.leap.auth;

import java.time.Instant;

public class TokenValidationResponse {

    private final boolean authenticated;
    private final String subject;
    private final String username;
    private final String email;
    private final String role;
    private final String tokenType;
    private final String sessionKeyId;
    private final String clientId;
    private final String accountId;
    private final Instant issuedAt;

    public TokenValidationResponse(boolean authenticated, String subject, String username, String email, String role, String tokenType,
                                   String sessionKeyId, String clientId, String accountId, Instant issuedAt) {
        this.authenticated = authenticated;
        this.subject = subject;
        this.username = username;
        this.email = email;
        this.role = role;
        this.tokenType = tokenType;
        this.sessionKeyId = sessionKeyId;
        this.clientId = clientId;
        this.accountId = accountId;
        this.issuedAt = issuedAt;
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

    public String getRole() {
        return role;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getSessionKeyId() {
        return sessionKeyId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getAccountId() {
        return accountId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
