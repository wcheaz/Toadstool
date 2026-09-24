package com.neueda.leap.controller;

import com.neueda.leap.Client;
import com.neueda.leap.enums.ClientStatus;
import com.neueda.leap.service.ClientService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Client Controller Tests")
class ClientControllerTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-22T14:30:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-09-23T09:15:00Z");

    @Mock
    private ClientService clientService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.buildMockMvc(new ClientController(clientService));
    }

    @Test
    @DisplayName("GET /api/clients/{clientId} returns the client payload from the YAML contract")
    void getClientReturnsClientPayload() throws Exception {
        UUID clientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Client client = new Client(clientId, "client@example.com", "Alice Trader", ClientStatus.ACTIVE, CREATED_AT, UPDATED_AT);

        when(clientService.getClientById(clientId)).thenReturn(client);

        mockMvc.perform(get("/api/clients/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.email").value("client@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice Trader"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value(ControllerTestSupport.isoTimestamp(CREATED_AT)))
                .andExpect(jsonPath("$.updatedAt").value(ControllerTestSupport.isoTimestamp(UPDATED_AT)));

        verify(clientService).getClientById(clientId);
    }

    @Test
    @DisplayName("GET /api/clients supports pagination and status filtering")
    void listClientsReturnsPaginatedItems() throws Exception {
        UUID firstClientId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Client client = new Client(firstClientId, "active@example.com", "Active Client", ClientStatus.ACTIVE, CREATED_AT, UPDATED_AT);

        when(clientService.listClientsByStatus("ACTIVE", 2, 0)).thenReturn(List.of(client));
        when(clientService.countClientsByStatus("ACTIVE")).thenReturn(1);

        mockMvc.perform(get("/api/clients")
                        .param("status", "ACTIVE")
                        .param("limit", "2")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].clientId").value(firstClientId.toString()))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.pagination.limit").value(2))
                .andExpect(jsonPath("$.pagination.offset").value(0))
                .andExpect(jsonPath("$.pagination.totalCount").value(1))
                .andExpect(jsonPath("$.pagination.hasMore").value(false));

        verify(clientService).listClientsByStatus("ACTIVE", 2, 0);
        verify(clientService).countClientsByStatus("ACTIVE");
    }

    @Test
    @DisplayName("PUT /api/clients/{clientId} accepts a request body and returns the updated client")
    void updateClientUsesRequestBodyContract() throws Exception {
        UUID clientId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Client existing = new Client(clientId, "client@example.com", "Before", ClientStatus.ACTIVE, CREATED_AT, UPDATED_AT);
        Client updated = new Client(clientId, "client@example.com", "After", ClientStatus.ACTIVE, CREATED_AT, UPDATED_AT.plusDays(1));

        when(clientService.getClientById(clientId)).thenReturn(existing, updated);

        mockMvc.perform(put("/api/clients/{clientId}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"After\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.displayName").value("After"))
                .andExpect(jsonPath("$.updatedAt").value(ControllerTestSupport.isoTimestamp(UPDATED_AT.plusDays(1))));

        verify(clientService).updateClientProfile(clientId, "After");
    }
}
