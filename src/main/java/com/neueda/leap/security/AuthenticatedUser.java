package com.neueda.leap.security;

import java.util.UUID;

public class AuthenticatedUser {

    private final String subject;
    private final String username;
    private final String email;
    private final UUID clientId;
    private final UUID accountId;

    public AuthenticatedUser(String subject, String username, String email, UUID clientId, UUID accountId) {
        this.subject = subject;
        this.username = username;
        this.email = email;
        this.clientId = clientId;
        this.accountId = accountId;
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

    public UUID getClientId() {
        return clientId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
