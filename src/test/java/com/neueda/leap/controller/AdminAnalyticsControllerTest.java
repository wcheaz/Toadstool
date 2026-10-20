package com.neueda.leap.controller;

import com.neueda.leap.dto.AdminDtos.PlatformActivityRow;
import com.neueda.leap.dto.AdminDtos.PlatformOverview;
import com.neueda.leap.dto.AdminDtos.TopInstrument;
import com.neueda.leap.service.AdminOversightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Analytics Controller Tests")
class AdminAnalyticsControllerTest {

    @Mock
    private AdminOversightService adminOversightService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ControllerTestSupport.authenticateAdmin();
        mockMvc = ControllerTestSupport.buildMockMvc(new AdminAnalyticsController(adminOversightService));
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    @DisplayName("GET /api/admin/analytics/platform-activity returns an empty page when there is no activity")
    void getPlatformActivityReturnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/platform-activity")
                        .param("limit", "5")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.pagination.limit").value(5))
                .andExpect(jsonPath("$.pagination.offset").value(0))
                .andExpect(jsonPath("$.pagination.totalCount").value(0))
                .andExpect(jsonPath("$.pagination.hasMore").value(false));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/platform-activity returns daily rows from the fact table")
    void getPlatformActivityReturnsFactRows() throws Exception {
        PlatformActivityRow row = new PlatformActivityRow();
        row.activityDate = LocalDate.parse("2026-10-14");
        row.orderCount = 3;
        row.filledQuantity = new BigDecimal("12.5000000000");
        row.grossAmount = new BigDecimal("1500.2500000000");
        row.updatedAt = OffsetDateTime.parse("2026-10-14T10:00:00Z");
        LocalDate from = LocalDate.parse("2026-10-01");
        when(adminOversightService.listPlatformActivity(from, null, 10, 0)).thenReturn(List.of(row));
        when(adminOversightService.countPlatformActivity(from, null)).thenReturn(1);

        mockMvc.perform(get("/api/admin/analytics/platform-activity")
                        .param("fromDate", "2026-10-01")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].activityDate").value("2026-10-14"))
                .andExpect(jsonPath("$.items[0].orderCount").value(3))
                .andExpect(jsonPath("$.items[0].filledQuantity").value("12.5"))
                .andExpect(jsonPath("$.items[0].grossAmount").value("1500.25"))
                .andExpect(jsonPath("$.pagination.totalCount").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/platform-activity rejects malformed dates")
    void getPlatformActivityRejectsMalformedDates() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/platform-activity")
                        .param("fromDate", "14/10/2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/admin/analytics/insights derives named insights from live data")
    void getBusinessInsightsDerivesInsights() throws Exception {
        PlatformOverview overview = new PlatformOverview();
        overview.totalClients = 4;
        overview.totalOrders = 10;
        overview.filledOrders = 9;
        overview.openOrders = 1;
        overview.grossVolumeLast30Days = new BigDecimal("2500");
        overview.feesLast30Days = new BigDecimal("12.5");
        TopInstrument top = new TopInstrument();
        top.instrumentId = UUID.randomUUID();
        top.symbol = "NEXS";
        top.fillCount = 6;
        top.grossAmount = new BigDecimal("1800");
        TopInstrument mostFilled = new TopInstrument();
        mostFilled.symbol = "FILL";
        mostFilled.fillCount = 10;
        mostFilled.grossAmount = new BigDecimal("1700");
        TopInstrument mostBought = new TopInstrument();
        mostBought.symbol = "BUY";
        mostBought.fillCount = 4;
        mostBought.grossAmount = new BigDecimal("1200");
        TopInstrument mostSold = new TopInstrument();
        mostSold.symbol = "SELL";
        mostSold.fillCount = 3;
        mostSold.grossAmount = new BigDecimal("900");
        when(adminOversightService.getPlatformOverview(30)).thenReturn(overview);
        when(adminOversightService.listTopInstruments(5, 30)).thenReturn(List.of(top));
        when(adminOversightService.getMostFilledInstrument(30)).thenReturn(mostFilled);
        when(adminOversightService.getTopInstrumentBySide("BUY", 30)).thenReturn(mostBought);
        when(adminOversightService.getTopInstrumentBySide("SELL", 30)).thenReturn(mostSold);

        mockMvc.perform(get("/api/admin/analytics/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.totalClients").value(4))
                .andExpect(jsonPath("$.topInstruments[0].symbol").value("NEXS"))
                .andExpect(jsonPath("$.insights[0].name").value("most-filled-instrument"))
                .andExpect(jsonPath("$.insights[0].value").value("FILL"))
                .andExpect(jsonPath("$.insights[1].name").value("most-volume-instrument"))
                .andExpect(jsonPath("$.insights[2].name").value("most-bought-instrument"))
                .andExpect(jsonPath("$.insights[3].name").value("most-sold-instrument"));

        mockMvc.perform(get("/api/admin/analytics/insights").param("insight", "open-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights.length()").value(1))
                .andExpect(jsonPath("$.insights[0].value").value("1"));
    }

    @Test
    @DisplayName("GET /api/admin/analytics/platform-activity rejects invalid pagination")
    void getPlatformActivityRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/platform-activity")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/admin/analytics/insights returns the insight response schema when there is no data")
    void getBusinessInsightsReturnsEmptySchema() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights.length()").value(0))
                .andExpect(jsonPath("$.generatedAt").exists());
    }
}
