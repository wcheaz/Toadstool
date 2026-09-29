package com.neueda.leap.auth;

import java.util.UUID;

public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;
    private final UUID clientId;
    private final UUID accountId;
    private final String username;
    private final String email;
    private final String role;

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UUID clientId, UUID accountId,
                        String username, String email, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.clientId = clientId;
        this.accountId = accountId;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getAccountId() {
        return accountId;
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
}
