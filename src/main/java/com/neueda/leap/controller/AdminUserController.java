package com.neueda.leap.controller;

import com.neueda.leap.AdminUser;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for AdminUser endpoints (admin-only)
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * POST /api/admin/users
     * Create a new admin/analyst user (admin-only)
     */
    @PostMapping
    public ResponseEntity<AdminUserDto> createAdminUser(@RequestBody CreateAdminUserRequest request) {
        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getRole() == null || request.getRole().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            AdminUser adminUser = adminUserService.createAdminUser(
                    request.getEmail(),
                    request.getDisplayName(),
                    request.getRole()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(mapToAdminUserDto(adminUser));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/admin/users/{adminUserId}
     * Retrieve an admin user (admin-only)
     */
    @GetMapping("/{adminUserId}")
    public ResponseEntity<AdminUserDto> getAdminUser(@PathVariable UUID adminUserId) {
        AdminUser adminUser = adminUserService.getAdminUserById(adminUserId);
        if (adminUser == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToAdminUserDto(adminUser));
    }

    /**
     * GET /api/admin/users
     * List all admin users (admin-only)
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminUserDto>> listAdminUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            List<AdminUser> adminUsers;
            int totalCount;

            if (role != null && !role.isEmpty()) {
                adminUsers = adminUserService.listAdminUsersByRole(role);
                totalCount = adminUsers.size();
            } else {
                adminUsers = adminUserService.listAllAdminUsers();
                totalCount = adminUsers.size();
            }

            List<AdminUserDto> responses = adminUsers.stream()
                    .map(this::mapToAdminUserDto)
                    .collect(Collectors.toList());

            PaginatedResponse<AdminUserDto> pagedResponse = new PaginatedResponse<>(responses, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/admin/users/{adminUserId}
     * Update an admin user (admin-only)
     */
    @PutMapping("/{adminUserId}")
    public ResponseEntity<AdminUserDto> updateAdminUser(
            @PathVariable UUID adminUserId,
            @RequestBody UpdateAdminUserRequest request) {

        try {
            AdminUser adminUser = adminUserService.getAdminUserById(adminUserId);
            if (adminUser == null) {
                return ResponseEntity.notFound().build();
            }

            String displayName = request.getDisplayName() != null ? request.getDisplayName() : adminUser.getDisplayName();
            String role = request.getRole() != null ? request.getRole() : adminUser.getRole().toString();
            String status = request.getStatus() != null ? request.getStatus() : adminUser.getStatus().toString();

            adminUser = adminUserService.updateAdminUser(adminUserId, displayName, role, status);
            return ResponseEntity.ok(mapToAdminUserDto(adminUser));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Map AdminUser domain object to AdminUserDto
     */
    private AdminUserDto mapToAdminUserDto(AdminUser adminUser) {
        String roleStr = adminUser.getRole() != null ? adminUser.getRole().toString() : "ADMIN";
        String statusStr = adminUser.getStatus() != null ? adminUser.getStatus().toString() : "ACTIVE";
        
        return new AdminUserDto(
                adminUser.getAdminUserId(),
                adminUser.getEmail(),
                adminUser.getDisplayName(),
                roleStr,
                statusStr,
                adminUser.getCreatedAt(),
                adminUser.getUpdatedAt()
        );
    }

    /**
     * DTO for AdminUser response
     */
    public static class AdminUserDto {
        private UUID adminUserId;
        private String email;
        private String displayName;
        private String role;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public AdminUserDto(UUID adminUserId, String email, String displayName, String role,
                           String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
            this.adminUserId = adminUserId;
            this.email = email;
            this.displayName = displayName;
            this.role = role;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        // Getters
        public UUID getAdminUserId() { return adminUserId; }
        public String getEmail() { return email; }
        public String getDisplayName() { return displayName; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
    }

    /**
     * Request DTO for creating an admin user
     */
    public static class CreateAdminUserRequest {
        private String email;
        private String displayName;
        private String role;

        public CreateAdminUserRequest() {}
        public CreateAdminUserRequest(String email, String displayName, String role) {
            this.email = email;
            this.displayName = displayName;
            this.role = role;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    /**
     * Request DTO for updating an admin user
     */
    public static class UpdateAdminUserRequest {
        private String displayName;
        private String role;
        private String status;

        public UpdateAdminUserRequest() {}
        public UpdateAdminUserRequest(String displayName, String role, String status) {
            this.displayName = displayName;
            this.role = role;
            this.status = status;
        }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
