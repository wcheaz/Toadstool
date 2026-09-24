package com.neueda.leap.controller;

import com.neueda.leap.Instrument;
import com.neueda.leap.InstrumentCreateRequest;
import com.neueda.leap.InstrumentStatusUpdateRequest;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.InstrumentStatus;
import com.neueda.leap.service.InstrumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Instrument endpoints
 */
@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
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
}
