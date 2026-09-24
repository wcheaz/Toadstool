package com.neueda.leap.controller;

import com.neueda.leap.Account;
import com.neueda.leap.Fill;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.FillStatus;
import com.neueda.leap.service.AccountService;
import com.neueda.leap.service.FillService;
import com.neueda.leap.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fill Controller Tests")
class FillControllerTest {

    private static final OffsetDateTime EXECUTED_AT = OffsetDateTime.parse("2026-09-22T14:30:00Z");

    @Mock
    private FillService fillService;

    @Mock
    private OrderService orderService;

    @Mock
    private AccountService accountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.buildMockMvc(new FillController(fillService, orderService, accountService));
    }

    @Test
    @DisplayName("GET /api/fills/{fillId} returns the fill payload from the YAML contract")
    void getFillReturnsFillPayload() throws Exception {
        UUID fillId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Fill fill = new Fill(fillId, orderId, new BigDecimal("101.2500000000"), new BigDecimal("2.0000000000"), FillStatus.Filled, EXECUTED_AT);

        when(fillService.getFillById(fillId)).thenReturn(fill);

        mockMvc.perform(get("/api/fills/{fillId}", fillId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fillId").value(fillId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.price").value("101.2500000000"))
                .andExpect(jsonPath("$.quantity").value("2.0000000000"))
                .andExpect(jsonPath("$.status").value("Filled"))
                .andExpect(jsonPath("$.executedAt").value(ControllerTestSupport.isoTimestamp(EXECUTED_AT)));
    }

    @Test
    @DisplayName("GET /api/orders/{orderId}/fills returns 404 when the order does not exist")
    void listFillsByOrderReturnsNotFoundForMissingOrder() throws Exception {
        UUID orderId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(orderService.getOrderById(orderId)).thenReturn(null);

        mockMvc.perform(get("/api/orders/{orderId}/fills", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/accounts/{accountId}/fills returns paginated fills for the account")
    void listFillsByAccountReturnsPaginatedItems() throws Exception {
        UUID accountId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID clientId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID orderId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Fill fill = new Fill(UUID.fromString("77777777-7777-7777-7777-777777777777"), orderId, new BigDecimal("99.0000000000"), new BigDecimal("5.0000000000"), FillStatus.Pending, EXECUTED_AT);

        when(accountService.getAccountById(accountId)).thenReturn(new Account(accountId, clientId, AccountStatus.ACTIVE, EXECUTED_AT));
        when(fillService.listFillsByAccount(accountId, 2, 0)).thenReturn(List.of(fill));
        when(fillService.countFillsByAccount(accountId)).thenReturn(1);

        mockMvc.perform(get("/api/accounts/{accountId}/fills", accountId)
                        .param("limit", "2")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].fillId").value(fill.getFillId().toString()))
                .andExpect(jsonPath("$.items[0].status").value("Pending"))
                .andExpect(jsonPath("$.pagination.limit").value(2))
                .andExpect(jsonPath("$.pagination.offset").value(0))
                .andExpect(jsonPath("$.pagination.totalCount").value(1))
                .andExpect(jsonPath("$.pagination.hasMore").value(false));
    }
}
