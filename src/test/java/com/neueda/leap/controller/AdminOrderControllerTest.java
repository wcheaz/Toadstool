package com.neueda.leap.controller;

import com.neueda.leap.Order;
import com.neueda.leap.dto.OrderResponse;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.service.OrderService;
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
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ControllerTestSupport.clearAuthentication();
        mockMvc = ControllerTestSupport.buildMockMvc(new AdminOrderController(orderService));
    }

    @Test
    void adminCanListOrdersWithSubmittedAtRange() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-10-01T00:00:00Z");
        Order order = new Order(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                OrderSide.BUY,
                new BigDecimal("2.0000000000"),
                "idem-admin-1",
                OffsetDateTime.parse("2026-09-15T12:00:00Z"));
        ControllerTestSupport.authenticateAdmin();

        when(orderService.listAllOrders(from, to, 25, 0)).thenReturn(List.of(order));
        when(orderService.countAllOrders(from, to)).thenReturn(1);

        mockMvc.perform(get("/api/admin/orders")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderId").value(order.getOrderId().toString()))
                .andExpect(jsonPath("$.pagination.totalCount").value(1));

        verify(orderService).listAllOrders(from, to, 25, 0);
        verify(orderService).countAllOrders(from, to);
    }

    @Test
    void clientCannotListAllOrders() throws Exception {
        ControllerTestSupport.authenticateClient(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }
}