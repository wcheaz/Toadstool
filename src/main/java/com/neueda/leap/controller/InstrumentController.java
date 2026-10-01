package com.neueda.leap.controller;

import com.neueda.leap.Instrument;
import com.neueda.leap.InstrumentCreateRequest;
import com.neueda.leap.InstrumentStatusUpdateRequest;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.service.InstrumentService;
import com.neueda.leap.service.FauxnanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Instrument endpoints
 */
@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentController.class);
    private final InstrumentService instrumentService;
    private final FauxnanceService fauxnanceService;

    public InstrumentController(InstrumentService instrumentService, FauxnanceService fauxnanceService) {
        this.instrumentService = instrumentService;
        this.fauxnanceService = fauxnanceService;
    }

    /**
     * GET /api/instruments/{instrumentId}
     * Retrieve a single instrument
     */
    @GetMapping("/{instrumentId}")
    public ResponseEntity<InstrumentDto> getInstrument(@PathVariable UUID instrumentId) {
        Instrument instrument = instrumentService.getInstrumentById(instrumentId);
        if (instrument == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToInstrumentDto(instrument));
    }

    /**
     * GET /api/instruments
     * List all instruments with optional filters
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<InstrumentDto>> listInstruments(
            @RequestParam(required = false) String assetClass,
            @RequestParam(required = false, defaultValue = "TRADABLE") String status,
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            List<Instrument> instruments;
            int totalCount;

            if (status != null && !status.isEmpty()) {
                instruments = instrumentService.listInstrumentsByStatus(status, limit, offset);
                totalCount = instrumentService.listInstrumentsByStatus(status).size();
            } else {
                instruments = instrumentService.listAllInstruments(limit, offset);
                totalCount = instrumentService.countAllInstruments();
            }

            List<InstrumentDto> responses = instruments.stream()
                    .map(this::mapToInstrumentDto)
                    .collect(Collectors.toList());

            PaginatedResponse<InstrumentDto> pagedResponse = new PaginatedResponse<>(responses, limit, offset, totalCount);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/instruments
     * Create a new instrument (admin-only)
     */
    @PostMapping
    public ResponseEntity<InstrumentDto> createInstrument(@RequestBody InstrumentCreateRequest request) {
        try {
            // Validate input
            if (request.getSymbol() == null || request.getSymbol().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (request.getAssetClass() == null) {
                return ResponseEntity.badRequest().build();
            }

            Instrument instrument = instrumentService.createInstrument(
                    request.getSymbol(),
                    request.getName(),
                    request.getAssetClass().toString()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(mapToInstrumentDto(instrument));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PATCH /api/instruments/{instrumentId}/status
     * Update instrument trading status (admin-only)
     */
    @PatchMapping("/{instrumentId}/status")
    public ResponseEntity<InstrumentDto> updateInstrumentStatus(
            @PathVariable UUID instrumentId,
            @RequestBody InstrumentStatusUpdateRequest request) {

        try {
            Instrument instrument = instrumentService.getInstrumentById(instrumentId);
            if (instrument == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.getStatus() == null) {
                return ResponseEntity.badRequest().build();
            }

            instrumentService.updateInstrumentStatus(instrumentId, request.getStatus().toString());

            // Refresh from database
            instrument = instrumentService.getInstrumentById(instrumentId);
            return ResponseEntity.ok(mapToInstrumentDto(instrument));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/instruments/{instrumentId}/candles
     * Get historical OHLCV candle data for charts
     */
    @GetMapping("/{instrumentId}/candles")
    public ResponseEntity<List<CandleDto>> getCandles(
            @PathVariable UUID instrumentId,
            @RequestParam(defaultValue = "30") int days) {
        try {
            Instrument instrument = instrumentService.getInstrumentById(instrumentId);
            if (instrument == null) {
                return ResponseEntity.notFound().build();
            }

            LocalDate toDate = LocalDate.now();
            LocalDate fromDate = toDate.minusDays(days);

            List<FauxnanceService.CandleResponse> candles = fauxnanceService.getCandles(
                    instrument.getSymbol(),
                    fromDate.toString(),
                    toDate.toString()
            );

            List<CandleDto> response = candles.stream()
                    .map(c -> new CandleDto(c.getDate(), c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/instruments/{instrumentId}/quote
     * Get live quote for instrument
     */
    @GetMapping("/{instrumentId}/quote")
    public ResponseEntity<QuoteDto> getInstrumentQuote(@PathVariable UUID instrumentId) {
        try {
            Instrument instrument = instrumentService.getInstrumentById(instrumentId);
            if (instrument == null) {
                return ResponseEntity.notFound().build();
            }

            FauxnanceService.QuoteResponse quote = fauxnanceService.getQuote(instrument.getSymbol());
            if (quote == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }

            QuoteDto response = new QuoteDto(
                    quote.getSymbol(),
                    quote.getPrice(),
                    quote.getBid(),
                    quote.getAsk(),
                    quote.getChange(),
                    quote.getChangePercent(),
                    quote.getAsOf()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching quote for instrument {}: {}", instrumentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Map Instrument domain object to InstrumentDto
     */
    private InstrumentDto mapToInstrumentDto(Instrument instrument) {
        String statusStr = instrument.getStatus() != null ? instrument.getStatus().toString() : "TRADABLE";
        String assetClassStr = instrument.getAssetClass() != null ? instrument.getAssetClass().toString() : "EQUITY";
        
        return new InstrumentDto(
                instrument.getInstrumentId(),
                instrument.getSymbol(),
                instrument.getName(),
                assetClassStr,
                statusStr
        );
    }

    /**
     * DTO for Instrument response
     */
    public static class InstrumentDto {
        private UUID instrumentId;
        private String symbol;
        private String name;
        private String assetClass;
        private String status;

        public InstrumentDto(UUID instrumentId, String symbol, String name, String assetClass, String status) {
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.name = name;
            this.assetClass = assetClass;
            this.status = status;
        }

        // Getters
        public UUID getInstrumentId() { return instrumentId; }
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public String getAssetClass() { return assetClass; }
        public String getStatus() { return status; }
    }

    /**
     * DTO for Candle response
     */
    public static class CandleDto {
        private String date;
        private double open;
        private double high;
        private double low;
        private double close;
        private long volume;

        public CandleDto(String date, double open, double high, double low, double close, long volume) {
            this.date = date;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }

        public String getDate() { return date; }
        public double getOpen() { return open; }
        public double getHigh() { return high; }
        public double getLow() { return low; }
        public double getClose() { return close; }
        public long getVolume() { return volume; }
    }

    /**
     * DTO for Quote response
     */
    public static class QuoteDto {
        private String symbol;
        private double price;
        private double bid;
        private double ask;
        private double change;
        private double changePercent;
        private String asOf;

        public QuoteDto(String symbol, double price, double bid, double ask, double change, double changePercent, String asOf) {
            this.symbol = symbol;
            this.price = price;
            this.bid = bid;
            this.ask = ask;
            this.change = change;
            this.changePercent = changePercent;
            this.asOf = asOf;
        }

        public String getSymbol() { return symbol; }
        public double getPrice() { return price; }
        public double getBid() { return bid; }
        public double getAsk() { return ask; }
        public double getChange() { return change; }
        public double getChangePercent() { return changePercent; }
        public String getAsOf() { return asOf; }
    }
}
