package com.neueda.leap.controller;

import com.neueda.leap.Account;
import com.neueda.leap.Client;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.ClientStatus;
import com.neueda.leap.service.AccountService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Controller Tests")
class AccountControllerTest {

    private static final OffsetDateTime OPENED_AT = OffsetDateTime.parse("2026-09-22T14:30:00Z");

    @Mock
    private AccountService accountService;

    @Mock
    private ClientService clientService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.buildMockMvc(new AccountController(accountService, clientService));
    }

    @Test
    @DisplayName("POST /api/accounts accepts the YAML request body and returns a created account")
    void createAccountReturnsCreatedAccount() throws Exception {
        UUID clientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID accountId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        when(clientService.getClientById(clientId))
                .thenReturn(new Client(clientId, "client@example.com", "Alice", ClientStatus.ACTIVE, OPENED_AT, OPENED_AT));
        when(accountService.createAccount(clientId))
                .thenReturn(new Account(accountId, clientId, AccountStatus.ACTIVE, OPENED_AT));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + clientId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.openedAt").value(ControllerTestSupport.isoTimestamp(OPENED_AT)));

        verify(accountService).createAccount(clientId);
    }

    @Test
    @DisplayName("GET /api/clients/{clientId}/accounts returns paginated accounts for the YAML path")
    void listAccountsByClientUsesYamlPath() throws Exception {
        UUID clientId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID accountId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Account account = new Account(accountId, clientId, AccountStatus.ACTIVE, OPENED_AT);

        when(clientService.getClientById(clientId))
                .thenReturn(new Client(clientId, "client@example.com", "Alice", ClientStatus.ACTIVE, OPENED_AT, OPENED_AT));
        when(accountService.listAccountsByClient(clientId, 2, 0)).thenReturn(List.of(account));
        when(accountService.countAccountsByClient(clientId)).thenReturn(1);

        mockMvc.perform(get("/api/clients/{clientId}/accounts", clientId)
                        .param("limit", "2")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.items[0].clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.pagination.limit").value(2))
                .andExpect(jsonPath("$.pagination.offset").value(0))
                .andExpect(jsonPath("$.pagination.totalCount").value(1))
                .andExpect(jsonPath("$.pagination.hasMore").value(false));
    }

    @Test
    @DisplayName("GET /api/accounts/{accountId}/holdings returns the stub holdings schema from the YAML")
    void getHoldingsReturnsStubHoldingsPayload() throws Exception {
        UUID accountId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID clientId = UUID.fromString("66666666-6666-6666-6666-666666666666");

        when(accountService.getAccountById(accountId))
                .thenReturn(new Account(accountId, clientId, AccountStatus.ACTIVE, OPENED_AT));

        mockMvc.perform(get("/api/accounts/{accountId}/holdings", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.holdings.length()").value(0))
                .andExpect(jsonPath("$.cash.balance").value("0.0000000000"))
                .andExpect(jsonPath("$.cash.currency").value("USD"))
                .andExpect(jsonPath("$.asOfTimestamp").exists());
    }
}
