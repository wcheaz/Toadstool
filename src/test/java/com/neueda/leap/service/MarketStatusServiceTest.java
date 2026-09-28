package com.neueda.leap.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("MarketStatusService Tests")
class MarketStatusServiceTest {

    @Autowired
    private MarketStatusService marketStatusService;

    @Test
    @DisplayName("isMarketOpen returns true in test environment")
    void testMarketOpenInTestEnvironment() {
        boolean isOpen = marketStatusService.isMarketOpen();
        assertTrue(isOpen, "Market should be open in test environment for testing purposes");
    }

    @Test
    @DisplayName("service is properly autowired")
    void testServiceAutowiring() {
        assertTrue(marketStatusService != null, "MarketStatusService should be autowired");
    }
}
