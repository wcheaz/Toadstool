package com.neueda.leap.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for client data
 */
public class ClientResponse {
    private UUID clientId;
    private String email;
    private String displayName;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ClientResponse() {
    }

    public ClientResponse(UUID clientId, String email, String displayName, String status, 
                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.clientId = clientId;
        this.email = email;
        this.displayName = displayName;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

/**
 * Request DTO for updating client profile
 */
class UpdateClientRequest {
    private String displayName;

    public UpdateClientRequest() {
    }

    public UpdateClientRequest(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
