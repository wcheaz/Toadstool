package com.neueda.leap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.neueda.leap.enums.AdminRole;
import com.neueda.leap.enums.AdminUserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminUser Tests")
class AdminUserTest {

    private AdminUser adminUser;
    private UUID testAdminUserId;
    private OffsetDateTime testDateTime;
    private String testEmail = "admin@example.com";
    private String testDisplayName = "Admin User";

    @BeforeEach
    void setUp() {
        testAdminUserId = UUID.randomUUID();
        testDateTime = OffsetDateTime.now();
    }

    @Test
    @DisplayName("Default constructor creates empty AdminUser")
    void testDefaultConstructor() {
        adminUser = new AdminUser();
        assertNull(adminUser.getAdminUserId());
        assertNull(adminUser.getEmail());
        assertNull(adminUser.getDisplayName());
        assertNull(adminUser.getRole());
        assertNull(adminUser.getStatus());
        assertNull(adminUser.getCreatedAt());
        assertNull(adminUser.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with email, displayName, role, and status")
    void testConstructorWithBasicFields() {
        adminUser = new AdminUser(testEmail, testDisplayName, AdminRole.ADMIN, AdminUserStatus.ACTIVE);
        
        assertNull(adminUser.getAdminUserId());
        assertEquals(testEmail, adminUser.getEmail());
        assertEquals(testDisplayName, adminUser.getDisplayName());
        assertEquals(AdminRole.ADMIN, adminUser.getRole());
        assertEquals(AdminUserStatus.ACTIVE, adminUser.getStatus());
        assertNull(adminUser.getCreatedAt());
        assertNull(adminUser.getUpdatedAt());
    }

    @Test
    @DisplayName("Constructor with all fields including ID")
    void testConstructorWithAllFields() {
        adminUser = new AdminUser(testAdminUserId, testEmail, testDisplayName, AdminRole.ADMIN, AdminUserStatus.ACTIVE, testDateTime, testDateTime);
        
        assertEquals(testAdminUserId, adminUser.getAdminUserId());
        assertEquals(testEmail, adminUser.getEmail());
        assertEquals(testDisplayName, adminUser.getDisplayName());
        assertEquals(AdminRole.ADMIN, adminUser.getRole());
        assertEquals(AdminUserStatus.ACTIVE, adminUser.getStatus());
        assertEquals(testDateTime, adminUser.getCreatedAt());
        assertEquals(testDateTime, adminUser.getUpdatedAt());
    }

    @Test
    @DisplayName("setAdminUserId and getAdminUserId")
    void testSetAndGetAdminUserId() {
        adminUser = new AdminUser();
        adminUser.setAdminUserId(testAdminUserId);
        assertEquals(testAdminUserId, adminUser.getAdminUserId());
    }

    @Test
    @DisplayName("setEmail and getEmail")
    void testSetAndGetEmail() {
        adminUser = new AdminUser();
        adminUser.setEmail("newemail@admin.com");
        assertEquals("newemail@admin.com", adminUser.getEmail());
    }

    @Test
    @DisplayName("setDisplayName and getDisplayName")
    void testSetAndGetDisplayName() {
        adminUser = new AdminUser();
        adminUser.setDisplayName("New Admin Name");
        assertEquals("New Admin Name", adminUser.getDisplayName());
    }

    @Test
    @DisplayName("setRole and getRole")
    void testSetAndGetRole() {
        adminUser = new AdminUser();
        adminUser.setRole(AdminRole.ANALYST);
        assertEquals(AdminRole.ANALYST, adminUser.getRole());
    }

    @Test
    @DisplayName("setStatus and getStatus")
    void testSetAndGetStatus() {
        adminUser = new AdminUser();
        adminUser.setStatus(AdminUserStatus.SUSPENDED);
        assertEquals(AdminUserStatus.SUSPENDED, adminUser.getStatus());
    }

    @Test
    @DisplayName("setCreatedAt and getCreatedAt")
    void testSetAndGetCreatedAt() {
        adminUser = new AdminUser();
        adminUser.setCreatedAt(testDateTime);
        assertEquals(testDateTime, adminUser.getCreatedAt());
    }

    @Test
    @DisplayName("setUpdatedAt and getUpdatedAt")
    void testSetAndGetUpdatedAt() {
        adminUser = new AdminUser();
        adminUser.setUpdatedAt(testDateTime);
        assertEquals(testDateTime, adminUser.getUpdatedAt());
    }
}
