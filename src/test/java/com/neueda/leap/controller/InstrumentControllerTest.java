package com.neueda.leap.controller;

import com.neueda.leap.Instrument;
import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.InstrumentStatus;
import com.neueda.leap.service.InstrumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Instrument Controller Tests")
class InstrumentControllerTest {

    @Mock
    private InstrumentService instrumentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.buildMockMvc(new InstrumentController(instrumentService));
    }

    @Test
    @DisplayName("GET /api/instruments/{instrumentId} returns an instrument matching the YAML schema")
    void getInstrumentReturnsInstrumentPayload() throws Exception {
        UUID instrumentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instrument instrument = new Instrument(instrumentId, "AAPL", "Apple Inc.", AssetClass.EQUITY, InstrumentStatus.TRADABLE);

        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(instrument);

        mockMvc.perform(get("/api/instruments/{instrumentId}", instrumentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instrumentId").value(instrumentId.toString()))
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.name").value("Apple Inc."))
                .andExpect(jsonPath("$.assetClass").value("EQUITY"))
                .andExpect(jsonPath("$.status").value("TRADABLE"));

        verify(instrumentService).getInstrumentById(instrumentId);
    }

    @Test
    @DisplayName("GET /api/instruments applies the default TRADABLE status filter and pagination")
    void listInstrumentsReturnsPaginatedItems() throws Exception {
        Instrument first = new Instrument(UUID.fromString("22222222-2222-2222-2222-222222222222"), "AAPL", "Apple Inc.", AssetClass.EQUITY, InstrumentStatus.TRADABLE);
        Instrument second = new Instrument(UUID.fromString("33333333-3333-3333-3333-333333333333"), "MSFT", "Microsoft Corporation", AssetClass.EQUITY, InstrumentStatus.TRADABLE);

        when(instrumentService.listInstrumentsByStatus("TRADABLE", 2, 1)).thenReturn(List.of(first, second));
        when(instrumentService.listInstrumentsByStatus("TRADABLE")).thenReturn(List.of(first, second, first));

        mockMvc.perform(get("/api/instruments")
                        .param("limit", "2")
                        .param("offset", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.items[1].symbol").value("MSFT"))
                .andExpect(jsonPath("$.pagination.limit").value(2))
                .andExpect(jsonPath("$.pagination.offset").value(1))
                .andExpect(jsonPath("$.pagination.totalCount").value(3))
                .andExpect(jsonPath("$.pagination.hasMore").value(false));

        verify(instrumentService).listInstrumentsByStatus("TRADABLE", 2, 1);
        verify(instrumentService).listInstrumentsByStatus("TRADABLE");
    }

    @Test
    @DisplayName("POST /api/instruments creates an instrument with the YAML response fields")
    void createInstrumentReturnsCreatedPayload() throws Exception {
        UUID instrumentId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Instrument instrument = new Instrument(instrumentId, "BTC-USD", "Bitcoin", AssetClass.CRYPTO, InstrumentStatus.TRADABLE);

        when(instrumentService.createInstrument("BTC-USD", "Bitcoin", "CRYPTO")).thenReturn(instrument);

        mockMvc.perform(post("/api/instruments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"BTC-USD\",\"name\":\"Bitcoin\",\"assetClass\":\"CRYPTO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instrumentId").value(instrumentId.toString()))
                .andExpect(jsonPath("$.symbol").value("BTC-USD"))
                .andExpect(jsonPath("$.assetClass").value("CRYPTO"))
                .andExpect(jsonPath("$.status").value("TRADABLE"));

        verify(instrumentService).createInstrument("BTC-USD", "Bitcoin", "CRYPTO");
    }

    @Test
    @DisplayName("PATCH /api/instruments/{instrumentId}/status returns the updated status")
    void updateInstrumentStatusReturnsUpdatedInstrument() throws Exception {
        UUID instrumentId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        Instrument existing = new Instrument(instrumentId, "ETH-USD", "Ethereum", AssetClass.CRYPTO, InstrumentStatus.TRADABLE);
        Instrument updated = new Instrument(instrumentId, "ETH-USD", "Ethereum", AssetClass.CRYPTO, InstrumentStatus.HALTED);

        when(instrumentService.getInstrumentById(instrumentId)).thenReturn(existing, updated);

        mockMvc.perform(patch("/api/instruments/{instrumentId}/status", instrumentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HALTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instrumentId").value(instrumentId.toString()))
                .andExpect(jsonPath("$.status").value("HALTED"));

        verify(instrumentService).updateInstrumentStatus(instrumentId, "HALTED");
    }
}
