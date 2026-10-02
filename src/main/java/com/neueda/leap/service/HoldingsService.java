package com.neueda.leap.service;

import com.neueda.leap.Holdings;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.repository.HoldingsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for Holdings operations.
 * 
 * Holdings represent the current cached position (quantity) of an instrument
 * held by an account. This is a denormalized, mutable cache of position data
 * derived from filled trades. While the data can be reconstructed from trade_events,
 * caching it here avoids expensive computation for every portfolio read.
 * 
 * Key responsibilities:
 * - Update holdings when fills complete (BUY adds to quantity, SELL subtracts)
 * - Retrieve portfolio positions for accounts
 * - Query holdings by instrument (market surveillance)
 * - Maintain the UNIQUE constraint on (account_id, instrument_id)
 */
@Service
public class HoldingsService {

    private final HoldingsRepository holdingsRepository;

    public HoldingsService(HoldingsRepository holdingsRepository) {
        this.holdingsRepository = holdingsRepository;
    }

    /**
     * Retrieve a specific holding by account and instrument.
     * 
     * @param accountId the account owner
     * @param instrumentId the instrument held
     * @return Optional containing the Holdings if found, empty otherwise
     */
    public Optional<Holdings> getHolding(UUID accountId, UUID instrumentId) {
        return holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId);
    }

    /**
     * Get all holdings for an account (portfolio view).
     * 
     * @param accountId the account owner
     * @param limit maximum number of results
     * @param offset pagination offset
     * @return list of Holdings records for this account
     */
    public List<Holdings> getAccountHoldings(UUID accountId, int limit, int offset) {
        return holdingsRepository.findByAccountId(accountId, limit, offset);
    }

    /**
     * Count all holdings for an account.
     * 
     * @param accountId the account owner
     * @return number of holdings (non-zero positions)
     */
    public int countAccountHoldings(UUID accountId) {
        return holdingsRepository.countByAccountId(accountId);
    }

    /**
     * Get all accounts holding a particular instrument (market surveillance).
     * 
     * @param instrumentId the instrument
     * @param limit maximum number of results
     * @param offset pagination offset
     * @return list of Holdings records for this instrument
     */
    public List<Holdings> getInstrumentHolders(UUID instrumentId, int limit, int offset) {
        return holdingsRepository.findByInstrumentId(instrumentId, limit, offset);
    }

    /**
     * Count all accounts holding a particular instrument.
     * 
     * @param instrumentId the instrument
     * @return number of accounts holding this instrument
     */
    public int countInstrumentHolders(UUID instrumentId) {
        return holdingsRepository.countByInstrumentId(instrumentId);
    }

    /**
     * Update holdings when a fill completes.
     * 
     * This is the primary mutation path for holdings:
     * - If a holding does not exist, create one with the fill quantity
     * - If a holding exists:
     *   - BUY: add fill quantity to existing quantity
     *   - SELL: subtract fill quantity from existing quantity
     * - Update the timestamp to reflect the most recent fill
     * 
     * @param accountId the account owner
     * @param instrumentId the instrument being traded
     * @param side the order side (BUY or SELL)
     * @param fillQuantity the quantity filled (not the entire order, may be partial)
     * @return the updated Holdings record
     */
    public Holdings updateHoldingFromFill(UUID accountId, UUID instrumentId, OrderSide side, BigDecimal fillQuantity) {
        Optional<Holdings> existingHolding = holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId);

        Holdings holding;
        if (existingHolding.isPresent()) {
            // Update existing holding
            holding = existingHolding.get();
            BigDecimal currentQuantity = holding.getQuantity();
            
            // Apply the fill: BUY adds, SELL subtracts
            BigDecimal newQuantity;
            if (OrderSide.BUY.equals(side)) {
                newQuantity = currentQuantity.add(fillQuantity);
            } else if (OrderSide.SELL.equals(side)) {
                newQuantity = currentQuantity.subtract(fillQuantity);
            } else {
                throw new IllegalArgumentException("Unknown order side: " + side);
            }
            
            holding.setQuantity(newQuantity);
        } else {
            // Create new holding: for BUY, quantity is positive; for SELL, this would be short selling
            BigDecimal newQuantity;
            if (OrderSide.BUY.equals(side)) {
                newQuantity = fillQuantity;
            } else if (OrderSide.SELL.equals(side)) {
                // Selling without a prior holding represents a short position (negative quantity)
                newQuantity = fillQuantity.negate();
            } else {
                throw new IllegalArgumentException("Unknown order side: " + side);
            }
            
            holding = new Holdings(accountId, instrumentId, newQuantity);
        }

        // Update the timestamp to reflect this mutation
        holding.setUpdatedAt(OffsetDateTime.now());

        return holdingsRepository.save(holding);
    }

    /**
     * Manually set a holding's quantity (for corrections, adjustments, or mass liquidation).
     * 
     * @param accountId the account owner
     * @param instrumentId the instrument held
     * @param newQuantity the new quantity
     * @return the updated Holdings record
     */
    public Holdings setHoldingQuantity(UUID accountId, UUID instrumentId, BigDecimal newQuantity) {
        Optional<Holdings> existingHolding = holdingsRepository.findByAccountIdAndInstrumentId(accountId, instrumentId);

        Holdings holding;
        if (existingHolding.isPresent()) {
            holding = existingHolding.get();
            holding.setQuantity(newQuantity);
        } else {
            holding = new Holdings(accountId, instrumentId, newQuantity);
        }

        holding.setUpdatedAt(OffsetDateTime.now());
        return holdingsRepository.save(holding);
    }

    /**
     * Delete a holding (typically called when a position is liquidated to 0,
     * though we may keep zero holdings for audit purposes).
     * 
     * @param holdingId the unique holding identifier
     * @return true if the holding was deleted, false if not found
     */
    public boolean deleteHolding(UUID holdingId) {
        return holdingsRepository.deleteById(holdingId);
    }
}
