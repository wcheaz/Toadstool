package com.neueda.leap.controller;

import com.neueda.leap.dto.AdminDtos.AdminOrderRow;
import com.neueda.leap.service.AdminOversightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private AdminOversightService adminOversightService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ControllerTestSupport.clearAuthentication();
        mockMvc = ControllerTestSupport.buildMockMvc(new AdminOrderController(adminOversightService));
    }

    @Test
    void adminCanListOrdersWithSubmittedAtRange() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-10-01T00:00:00Z");
        AdminOrderRow order = new AdminOrderRow();
        order.orderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        order.accountId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        order.instrumentId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        order.side = "BUY";
        order.quantity = new BigDecimal("2.0000000000");
        order.idempotencyKey = "idem-admin-1";
        order.status = "SUBMITTED";
        order.submittedAt = OffsetDateTime.parse("2026-09-15T12:00:00Z");
        ControllerTestSupport.authenticateAdmin();

        when(adminOversightService.listAllOrders(from, to, 25, 0)).thenReturn(List.of(order));
        when(adminOversightService.countAllOrders(from, to)).thenReturn(1);

        mockMvc.perform(get("/api/admin/orders")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderId").value(order.orderId.toString()))
                .andExpect(jsonPath("$.pagination.totalCount").value(1));

        verify(adminOversightService).listAllOrders(from, to, 25, 0);
        verify(adminOversightService).countAllOrders(from, to);
    }

    @Test
    void clientCannotListAllOrders() throws Exception {
        ControllerTestSupport.authenticateClient(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminOversightService);
    }
}