package com.neueda.leap.repository;

import com.neueda.leap.Holdings;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Holdings persistence operations.
 * 
 * Holdings represent the current cached position (quantity) of an instrument
 * held by an account. Unlike append-only tables, holdings are mutable.
 */
public interface HoldingsRepository {
    
    /**
     * Find a holding by its unique ID.
     * 
     * @param holdingId the unique holding identifier
     * @return the Holdings record, or empty if not found
     */
    Optional<Holdings> findById(UUID holdingId);
    
    /**
     * Find a holding by account and instrument combination (primary lookup).
     * 
     * @param accountId the account owner
     * @param instrumentId the instrument held
     * @return the Holdings record if it exists, or empty if not found
     */
    Optional<Holdings> findByAccountIdAndInstrumentId(UUID accountId, UUID instrumentId);
    
    /**
     * Find all holdings for an account (e.g., portfolio view).
     * 
     * @param accountId the account owner
     * @param limit maximum number of results
     * @param offset pagination offset
     * @return list of Holdings records for this account
     */
    List<Holdings> findByAccountId(UUID accountId, int limit, int offset);
    
    /**
     * Count all holdings for an account.
     * 
     * @param accountId the account owner
     * @return number of holdings (non-zero positions)
     */
    int countByAccountId(UUID accountId);
    
    /**
     * Find all accounts holding a particular instrument (e.g., market surveillance).
     * 
     * @param instrumentId the instrument
     * @param limit maximum number of results
     * @param offset pagination offset
     * @return list of Holdings records for this instrument
     */
    List<Holdings> findByInstrumentId(UUID instrumentId, int limit, int offset);
    
    /**
     * Count all holdings for an instrument.
     * 
     * @param instrumentId the instrument
     * @return number of accounts holding this instrument
     */
    int countByInstrumentId(UUID instrumentId);
    
    /**
     * Save or update a holding.
     * 
     * @param holdings the Holdings record to persist
     * @return the persisted Holdings record with ID if newly created
     */
    Holdings save(Holdings holdings);
    
    /**
     * Delete a holding by ID.
     * 
     * @param holdingId the unique holding identifier
     * @return true if the holding was deleted, false if not found
     */
    boolean deleteById(UUID holdingId);
}
