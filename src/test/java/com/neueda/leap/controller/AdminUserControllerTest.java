package com.neueda.leap.controller;

import com.neueda.leap.AdminUser;
import com.neueda.leap.enums.AdminRole;
import com.neueda.leap.enums.AdminUserStatus;
import com.neueda.leap.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin User Controller Tests")
class AdminUserControllerTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-22T14:30:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-09-23T09:15:00Z");

    @Mock
    private AdminUserService adminUserService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.buildMockMvc(new AdminUserController(adminUserService));
    }

    @Test
    @DisplayName("POST /api/admin/users creates an admin user with the YAML response fields")
    void createAdminUserReturnsCreatedPayload() throws Exception {
        UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AdminUser adminUser = new AdminUser(adminUserId, "admin@example.com", "Admin One", AdminRole.ADMIN, AdminUserStatus.ACTIVE, CREATED_AT, UPDATED_AT);

        when(adminUserService.createAdminUser("admin@example.com", "Admin One", "ADMIN")).thenReturn(adminUser);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"displayName\":\"Admin One\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adminUserId").value(adminUserId.toString()))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.displayName").value("Admin One"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value(ControllerTestSupport.isoTimestamp(CREATED_AT)));
    }

    @Test
    @DisplayName("GET /api/admin/users supports role filtering and pagination")
    void listAdminUsersReturnsPaginatedItems() throws Exception {
        UUID adminUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        AdminUser analyst = new AdminUser(adminUserId, "analyst@example.com", "Analyst", AdminRole.ANALYST, AdminUserStatus.ACTIVE, CREATED_AT, UPDATED_AT);

        when(adminUserService.listAdminUsersByRole("ANALYST", 10, 20)).thenReturn(List.of(analyst));
        when(adminUserService.countAdminUsersByRole("ANALYST")).thenReturn(21);

        mockMvc.perform(get("/api/admin/users")
                        .param("role", "ANALYST")
                        .param("limit", "10")
                        .param("offset", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].adminUserId").value(adminUserId.toString()))
                .andExpect(jsonPath("$.items[0].role").value("ANALYST"))
                .andExpect(jsonPath("$.pagination.limit").value(10))
                .andExpect(jsonPath("$.pagination.offset").value(20))
                .andExpect(jsonPath("$.pagination.totalCount").value(21))
                .andExpect(jsonPath("$.pagination.hasMore").value(false));

        verify(adminUserService).listAdminUsersByRole("ANALYST", 10, 20);
        verify(adminUserService).countAdminUsersByRole("ANALYST");
    }

    @Test
    @DisplayName("PUT /api/admin/users/{adminUserId} updates an admin user from the request body")
    void updateAdminUserReturnsUpdatedPayload() throws Exception {
        UUID adminUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        AdminUser existing = new AdminUser(adminUserId, "user@example.com", "Before", AdminRole.ADMIN, AdminUserStatus.ACTIVE, CREATED_AT, UPDATED_AT);
        AdminUser updated = new AdminUser(adminUserId, "user@example.com", "After", AdminRole.ADMIN, AdminUserStatus.SUSPENDED, CREATED_AT, UPDATED_AT.plusDays(1));

        when(adminUserService.getAdminUserById(adminUserId)).thenReturn(existing);
        when(adminUserService.updateAdminUser(adminUserId, "After", "ADMIN", "SUSPENDED")).thenReturn(updated);

        mockMvc.perform(put("/api/admin/users/{adminUserId}", adminUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"After\",\"role\":\"ADMIN\",\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminUserId").value(adminUserId.toString()))
                .andExpect(jsonPath("$.displayName").value("After"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.updatedAt").value(ControllerTestSupport.isoTimestamp(UPDATED_AT.plusDays(1))));
    }
}
