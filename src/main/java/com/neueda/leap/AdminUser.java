package com.neueda.leap;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminUser {

    private UUID adminUserId;
    private String email;
    private String displayName;
    private AdminRole role;
    private AdminUserStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Constructors
    public AdminUser() {
    }

    public AdminUser(String email, String displayName, AdminRole role, AdminUserStatus status) {
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
    }

    public AdminUser(UUID adminUserId, String email, String displayName, AdminRole role, AdminUserStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.adminUserId = adminUserId;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public UUID getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(UUID adminUserId) {
        this.adminUserId = adminUserId;
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

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
    }

    public AdminUserStatus getStatus() {
        return status;
    }

    public void setStatus(AdminUserStatus status) {
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
    public enum AdminRole {
        ADMIN,
        ANALYST
    }

    public enum AdminUserStatus {
        ACTIVE,
        SUSPENDED
    }
}
