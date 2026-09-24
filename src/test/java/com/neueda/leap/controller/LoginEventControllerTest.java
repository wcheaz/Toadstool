package com.neueda.leap.controller;

import com.neueda.leap.Client;
import com.neueda.leap.LoginEvent;
import com.neueda.leap.enums.ClientStatus;
import com.neueda.leap.enums.LoginOutcome;
import com.neueda.leap.service.ClientService;
import com.neueda.leap.service.LoginEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Login Event Controller Tests")
class LoginEventControllerTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-22T14:30:00Z");

    @Mock
    private LoginEventService loginEventService;

    @Mock
    private ClientService clientService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.buildMockMvc(new LoginEventController(loginEventService, clientService));
    }

    @Test
    @DisplayName("GET /api/clients/{clientId}/login-events returns paginated login events")
    void listLoginEventsByClientReturnsPaginatedItems() throws Exception {
        UUID clientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LoginEvent event = new LoginEvent(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                clientId,
                "client@example.com",
                LoginOutcome.SUCCESS,
                OCCURRED_AT,
                "{\"ip\":\"127.0.0.1\"}"
        );

        when(clientService.getClientById(clientId))
                .thenReturn(new Client(clientId, "client@example.com", "Alice", ClientStatus.ACTIVE, OCCURRED_AT, OCCURRED_AT));
        when(loginEventService.listLoginEventsByClient(clientId, 2, 0)).thenReturn(List.of(event));
        when(loginEventService.countLoginEventsByClient(clientId)).thenReturn(1);

        mockMvc.perform(get("/api/clients/{clientId}/login-events", clientId)
                        .param("outcome", "SUCCESS")
                        .param("limit", "2")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].loginEventId").value(event.getLoginEventId().toString()))
                .andExpect(jsonPath("$.items[0].clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.items[0].emailAttempted").value("client@example.com"))
                .andExpect(jsonPath("$.items[0].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.items[0].occurredAt").value(ControllerTestSupport.isoTimestamp(OCCURRED_AT)))
                .andExpect(jsonPath("$.items[0].details").value("{\"ip\":\"127.0.0.1\"}"))
                .andExpect(jsonPath("$.pagination.totalCount").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/login-events returns the admin login event listing schema")
    void listLoginEventsAdminReturnsPaginatedItems() throws Exception {
        UUID clientId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        LoginEvent event = new LoginEvent(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                clientId,
                "admin-view@example.com",
                LoginOutcome.FAILURE,
                OCCURRED_AT,
                "{\"reason\":\"wrong password\"}"
        );

        when(loginEventService.listAllLoginEvents(1, 0)).thenReturn(List.of(event));
        when(loginEventService.countAllLoginEvents()).thenReturn(1);

        mockMvc.perform(get("/api/admin/login-events")
                        .param("limit", "1")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].loginEventId").value(event.getLoginEventId().toString()))
                .andExpect(jsonPath("$.items[0].outcome").value("FAILURE"))
                .andExpect(jsonPath("$.pagination.limit").value(1))
                .andExpect(jsonPath("$.pagination.offset").value(0))
                .andExpect(jsonPath("$.pagination.totalCount").value(1));
    }
}
