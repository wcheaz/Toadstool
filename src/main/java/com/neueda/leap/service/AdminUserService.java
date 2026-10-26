package com.neueda.leap.service;

import com.neueda.leap.AdminUser;
import com.neueda.leap.mapper.AdminUserMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for AdminUser operations
 */
@Service
public class AdminUserService {

    private final AdminUserMapper adminUserMapper;

    public AdminUserService(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    /**
     * Get admin user by ID
     */
    public AdminUser getAdminUserById(UUID adminUserId) {
        return adminUserMapper.selectAdminUserById(adminUserId);
    }

    /**
     * Get admin user by email
     */
    public AdminUser getAdminUserByEmail(String email) {
        return adminUserMapper.selectAdminUserByEmail(email);
    }

    /**
     * List all admin users with pagination
     */
    public List<AdminUser> listAllAdminUsers(int limit, int offset) {
        return adminUserMapper.selectAllAdminUsers(limit, offset);
    }

    /**
     * List all admin users without pagination
     */
    public List<AdminUser> listAllAdminUsers() {
        return adminUserMapper.selectAllAdminUsers(Integer.MAX_VALUE, 0);
    }

    /**
     * Count all admin users
     */
    public int countAllAdminUsers() {
        return adminUserMapper.countAllAdminUsers();
    }

    /**
     * List admin users by role with pagination
     */
    public List<AdminUser> listAdminUsersByRole(String role, int limit, int offset) {
        return adminUserMapper.selectAdminUsersByRole(role, limit, offset);
    }

    /**
     * List admin users by role without pagination
     */
    public List<AdminUser> listAdminUsersByRole(String role) {
        return adminUserMapper.selectAdminUsersByRole(role, Integer.MAX_VALUE, 0);
    }

    /**
     * Count admin users by role
     */
    public int countAdminUsersByRole(String role) {
        return adminUserMapper.countAdminUsersByRole(role);
    }

    /**
     * Create a new admin user
     */
    public AdminUser createAdminUser(String email, String displayName, String role) {
        validateAdminUserInput(email, displayName, role);

        // Check if email already exists
        if (getAdminUserByEmail(email) != null) {
            throw new IllegalArgumentException("Email already in use: " + email);
        }

        adminUserMapper.insertAdminUser(email, displayName, role);
        
        // Return the newly created admin user
        return getAdminUserByEmail(email);
    }

    /**
     * Update admin user
     */
    public AdminUser updateAdminUser(UUID adminUserId, String displayName, String role, String status) {
        if (displayName != null) {
            validateDisplayName(displayName);
        }
        if (role != null) {
            validateRole(role);
        }
        if (status != null) {
            validateStatus(status);
        }
        adminUserMapper.updateAdminUser(adminUserId, displayName, role, status);
        
        // Return the updated admin user
        return getAdminUserById(adminUserId);
    }

    /**
     * Validate admin user input
     */
    private void validateAdminUserInput(String email, String displayName, String role) {
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        validateDisplayName(displayName);
        validateRole(role);
    }

    /**
     * Validate display name
     */
    private void validateDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty() || displayName.length() > 120) {
            throw new IllegalArgumentException("Display name must be 1-120 characters");
        }
    }

    /**
     * Validate role
     */
    private void validateRole(String role) {
        if (role == null || (!role.equals("ADMIN") && !role.equals("ANALYST"))) {
            throw new IllegalArgumentException("Role must be ADMIN or ANALYST");
        }
    }

    /**
     * Validate status
     */
    private void validateStatus(String status) {
        if (status == null || (!status.equals("ACTIVE") && !status.equals("SUSPENDED"))) {
            throw new IllegalArgumentException("Status must be ACTIVE or SUSPENDED");
        }
    }
}
