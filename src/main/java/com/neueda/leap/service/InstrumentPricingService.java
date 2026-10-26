package com.neueda.leap.service;

import com.neueda.leap.Instrument;
import com.neueda.leap.enums.AssetClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Service for fetching instrument prices via the Fauxnance API
 * 
 * Replaces random price generation with live data from FauxnanceService.
 * Falls back to a default fallback price if the API is unavailable.
 */
@Service
public class InstrumentPricingService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentPricingService.class);
    private final FauxnanceService fauxnanceService;
    private final InstrumentService instrumentService;
    private static final BigDecimal FALLBACK_PRICE = new BigDecimal("100.00");

    public InstrumentPricingService(FauxnanceService fauxnanceService, InstrumentService instrumentService) {
        this.fauxnanceService = fauxnanceService;
        this.instrumentService = instrumentService;
    }

    /**
     * Gets the current price for an instrument from the Fauxnance API
     * 
     * @param assetClass The asset class of the instrument (used for fallback/logging)
     * @param instrumentId The ID of the instrument
     * @return The price as BigDecimal with at most 5 decimal places, or fallback price if API unavailable
     * @throws IllegalArgumentException if assetClass is null
     */
    public BigDecimal getPrice(AssetClass assetClass, UUID instrumentId) {
        if (assetClass == null) {
            throw new IllegalArgumentException("Asset class cannot be null");
        }

        try {
            // Get the instrument to retrieve its symbol
            Instrument instrument = instrumentService.getInstrumentById(instrumentId);
            if (instrument == null) {
                logger.warn("Instrument not found for ID: {}", instrumentId);
                return FALLBACK_PRICE.setScale(5, RoundingMode.HALF_UP);
            }

            // Fetch the price from Fauxnance API using the instrument symbol
            FauxnanceService.QuoteResponse quote = fauxnanceService.getQuote(instrument.getSymbol());
            if (quote == null) {
                logger.warn("Failed to fetch price from Fauxnance for symbol: {}", instrument.getSymbol());
                return FALLBACK_PRICE.setScale(5, RoundingMode.HALF_UP);
            }

            // Convert the double price to BigDecimal and return with proper scale
            BigDecimal price = new BigDecimal(quote.getPrice());
            return price.setScale(5, RoundingMode.HALF_UP);

        } catch (Exception e) {
            logger.error("Error fetching price for instrument {}: {}", instrumentId, e.getMessage(), e);
            return FALLBACK_PRICE.setScale(5, RoundingMode.HALF_UP);
        }
    }
}
