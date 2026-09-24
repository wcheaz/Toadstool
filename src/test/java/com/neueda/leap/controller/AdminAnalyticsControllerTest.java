package com.neueda.leap.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Admin Analytics Controller Tests")
class AdminAnalyticsControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.buildMockMvc(new AdminAnalyticsController());
    }

    @Test
    @DisplayName("GET /api/admin/analytics/platform-activity returns the stub paginated activity payload")
    void getPlatformActivityReturnsPaginatedStub() throws Exception {
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
    @DisplayName("GET /api/admin/analytics/platform-activity rejects invalid pagination")
    void getPlatformActivityRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/platform-activity")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/admin/analytics/insights returns the stub insight response schema")
    void getBusinessInsightsReturnsStubSchema() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights.length()").value(0))
                .andExpect(jsonPath("$.generatedAt").exists());
    }
}
