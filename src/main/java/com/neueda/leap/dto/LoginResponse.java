package com.neueda.leap.dto;

import java.util.UUID;

/**
 * Response DTO for successful login
 */
public class LoginResponse {
    private UUID clientId;
    private String email;
    private String displayName;
    private String status;
    private String message;

    // Constructors
    public LoginResponse() {
    }

    public LoginResponse(UUID clientId, String email, String displayName, String status) {
        this.clientId = clientId;
        this.email = email;
        this.displayName = displayName;
        this.status = status;
        this.message = "Login successful";
    }

    // Getters and Setters
    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
