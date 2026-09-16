package com.neueda.leap;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Client {

    private UUID clientId;
    private String email;
    private String displayName;
    private ClientStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Constructors
    public Client() {
    }

    public Client(String email, String displayName, ClientStatus status) {
        this.email = email;
        this.displayName = displayName;
        this.status = status;
    }

    public Client(UUID clientId, String email, String displayName, ClientStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
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

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
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

    // Enums matching database constraints
    public enum ClientStatus {
        ACTIVE,
        SUSPENDED,
        CLOSED
    }
}
