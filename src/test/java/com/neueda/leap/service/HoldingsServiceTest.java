package com.neueda.leap.service;

import com.neueda.leap.Holdings;
import com.neueda.leap.Order;
import com.neueda.leap.Fill;
import com.neueda.leap.Account;
import com.neueda.leap.Instrument;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.enums.FillStatus;
import com.neueda.leap.repository.HoldingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * TDD tests for HoldingsService.
 * 
 * These tests define the expected behavior of holdings management:
 * - Creating or updating holdings when fills complete
 * - Calculating net position from buy/sell side
 * - Retrieving holdings for an account
 * - Handling liquidated positions (quantity = 0)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HoldingsService Tests")
class HoldingsServiceTest {

    private HoldingsService holdingsService;

    @Mock
    private HoldingsRepository holdingsRepository;

    private UUID accountId;
    private UUID instrumentId;
    private UUID holdingId;
    private Account testAccount;
    private Instrument testInstrument;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        instrumentId = UUID.randomUUID();
        holdingId = UUID.randomUUID();

        holdingsService = new HoldingsService(holdingsRepository);

        testAccount = new Account();
        testAccount.setAccountId(accountId);

        testInstrument = new Instrument();
        testInstrument.setInstrumentId(instrumentId);
    }

    @Test
    @DisplayName("Create new holding when account buys an instrument for first time")
    void testCreateNewHoldingOnFirstBuy() {
        BigDecimal buyQuantity = new BigDecimal("100");
        Fill fill = new Fill(UUID.randomUUID(), buyQuantity, buyQuantity, FillStatus.Filled);

        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.empty());
        when(holdingsRepository.save(any(Holdings.class)))
            .thenAnswer(invocation -> {
                Holdings holdings = invocation.getArgument(0);
                holdings.setHoldingId(holdingId);
                return holdings;
            });

        Holdings result = holdingsService.updateHoldingFromFill(accountId, instrumentId, OrderSide.BUY, buyQuantity);

        assertNotNull(result);
        assertEquals(accountId, result.getAccountId());
        assertEquals(instrumentId, result.getInstrumentId());
        assertEquals(buyQuantity, result.getQuantity());
        verify(holdingsRepository, times(1)).save(any(Holdings.class));
    }

    @Test
    @DisplayName("Increase holding quantity on subsequent buy")
    void testIncreaseHoldingOnSubsequentBuy() {
        BigDecimal existingQuantity = new BigDecimal("100");
        BigDecimal additionalBuy = new BigDecimal("50");
        BigDecimal expectedTotal = new BigDecimal("150");

        Holdings existingHolding = new Holdings(holdingId, accountId, instrumentId, existingQuantity, OffsetDateTime.now());

        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.of(existingHolding));
        when(holdingsRepository.save(any(Holdings.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Holdings result = holdingsService.updateHoldingFromFill(accountId, instrumentId, OrderSide.BUY, additionalBuy);

        assertEquals(expectedTotal, result.getQuantity());
        verify(holdingsRepository, times(1)).save(any(Holdings.class));
    }

    @Test
    @DisplayName("Decrease holding quantity on sell")
    void testDecreaseHoldingOnSell() {
        BigDecimal existingQuantity = new BigDecimal("100");
        BigDecimal sellQuantity = new BigDecimal("30");
        BigDecimal expectedRemaining = new BigDecimal("70");

        Holdings existingHolding = new Holdings(holdingId, accountId, instrumentId, existingQuantity, OffsetDateTime.now());

        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.of(existingHolding));
        when(holdingsRepository.save(any(Holdings.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Holdings result = holdingsService.updateHoldingFromFill(accountId, instrumentId, OrderSide.SELL, sellQuantity);

        assertEquals(expectedRemaining, result.getQuantity());
        verify(holdingsRepository, times(1)).save(any(Holdings.class));
    }

    @Test
    @DisplayName("Set holding to zero when account sells entire position")
    void testLiquidatePosition() {
        BigDecimal existingQuantity = new BigDecimal("100");
        BigDecimal sellQuantity = new BigDecimal("100");
        BigDecimal expectedZero = BigDecimal.ZERO;

        Holdings existingHolding = new Holdings(holdingId, accountId, instrumentId, existingQuantity, OffsetDateTime.now());

        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.of(existingHolding));
        when(holdingsRepository.save(any(Holdings.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Holdings result = holdingsService.updateHoldingFromFill(accountId, instrumentId, OrderSide.SELL, sellQuantity);

        assertEquals(expectedZero, result.getQuantity());
        verify(holdingsRepository, times(1)).save(any(Holdings.class));
    }

    @Test
    @DisplayName("Handle partial fills that don't match order quantity")
    void testHandlePartialFill() {
        BigDecimal existingQuantity = new BigDecimal("100");
        BigDecimal partialFillQuantity = new BigDecimal("25.5");
        BigDecimal expectedTotal = new BigDecimal("125.5");

        Holdings existingHolding = new Holdings(holdingId, accountId, instrumentId, existingQuantity, OffsetDateTime.now());

        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.of(existingHolding));
        when(holdingsRepository.save(any(Holdings.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Holdings result = holdingsService.updateHoldingFromFill(accountId, instrumentId, OrderSide.BUY, partialFillQuantity);

        assertEquals(expectedTotal, result.getQuantity());
    }

    @Test
    @DisplayName("Update timestamp when holding is modified")
    void testUpdateTimestampOnModification() {
        BigDecimal quantity = new BigDecimal("100");
        Holdings existingHolding = new Holdings(holdingId, accountId, instrumentId, quantity, OffsetDateTime.now().minusHours(1));

        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.of(existingHolding));
        when(holdingsRepository.save(any(Holdings.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime beforeUpdate = OffsetDateTime.now();
        Holdings result = holdingsService.updateHoldingFromFill(accountId, instrumentId, OrderSide.BUY, new BigDecimal("10"));
        OffsetDateTime afterUpdate = OffsetDateTime.now();

        assertNotNull(result.getUpdatedAt());
        assertTrue(result.getUpdatedAt().isAfter(beforeUpdate.minusSeconds(1)));
        assertTrue(result.getUpdatedAt().isBefore(afterUpdate.plusSeconds(1)));
    }

    @Test
    @DisplayName("Retrieve holding for account and instrument")
    void testGetHoldingByAccountAndInstrument() {
        Holdings expectedHolding = new Holdings(holdingId, accountId, instrumentId, new BigDecimal("150"), OffsetDateTime.now());

        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.of(expectedHolding));

        Optional<Holdings> result = holdingsService.getHolding(accountId, instrumentId);

        assertTrue(result.isPresent());
        assertEquals(expectedHolding, result.get());
        assertEquals(new BigDecimal("150"), result.get().getQuantity());
    }

    @Test
    @DisplayName("Return empty when holding does not exist")
    void testGetNonExistentHolding() {
        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.empty());

        Optional<Holdings> result = holdingsService.getHolding(accountId, instrumentId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Handle high precision quantities with 10 decimal places")
    void testHighPrecisionQuantityCalculation() {
        BigDecimal existingQuantity = new BigDecimal("100.1234567890");
        BigDecimal additionalBuy = new BigDecimal("50.9876543210");
        BigDecimal expectedTotal = new BigDecimal("151.1111111100");

        Holdings existingHolding = new Holdings(holdingId, accountId, instrumentId, existingQuantity, OffsetDateTime.now());

        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.of(existingHolding));
        when(holdingsRepository.save(any(Holdings.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Holdings result = holdingsService.updateHoldingFromFill(accountId, instrumentId, OrderSide.BUY, additionalBuy);

        assertEquals(expectedTotal, result.getQuantity());
    }

    @Test
    @DisplayName("Sell side calculation respects sign convention")
    void testSellSideNegatesQuantity() {
        BigDecimal existingQuantity = new BigDecimal("200");
        BigDecimal sellQuantity = new BigDecimal("50");
        BigDecimal expectedQuantity = new BigDecimal("150");

        Holdings existingHolding = new Holdings(holdingId, accountId, instrumentId, existingQuantity, OffsetDateTime.now());

        when(holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId))
            .thenReturn(Optional.of(existingHolding));
        when(holdingsRepository.save(any(Holdings.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // SELL side should subtract from quantity
        Holdings result = holdingsService.updateHoldingFromFill(accountId, instrumentId, OrderSide.SELL, sellQuantity);

        assertEquals(expectedQuantity, result.getQuantity());
    }
}
