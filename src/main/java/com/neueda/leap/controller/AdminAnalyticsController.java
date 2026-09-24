package com.neueda.leap.controller;

import com.neueda.leap.dto.PaginatedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Analytics endpoints (admin-only, reporting)
 */
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    /**
     * GET /api/admin/analytics/platform-activity
     * Platform-wide trading activity summary (stub for Week 1-2)
     */
    @GetMapping("/platform-activity")
    public ResponseEntity<PaginatedResponse<PlatformActivityDto>> getDailyPlatformActivity(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            // Stub: return empty activity data for Week 1-2
            List<PlatformActivityDto> activity = new ArrayList<>();

            PaginatedResponse<PlatformActivityDto> pagedResponse = new PaginatedResponse<>(activity, limit, offset, 0);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/admin/analytics/instrument-activity
     * Per-instrument trading activity summary (stub for Week 1-2)
     */
    @GetMapping("/instrument-activity")
    public ResponseEntity<PaginatedResponse<InstrumentActivityDto>> getDailyInstrumentActivity(
            @RequestParam(required = false) UUID instrumentId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        try {
            // Validate pagination
            if (limit < 1 || limit > 1000 || offset < 0) {
                return ResponseEntity.badRequest().build();
            }

            // Stub: return empty activity data for Week 1-2
            List<InstrumentActivityDto> activity = new ArrayList<>();

            PaginatedResponse<InstrumentActivityDto> pagedResponse = new PaginatedResponse<>(activity, limit, offset, 0);
            return ResponseEntity.ok(pagedResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/admin/analytics/insights
     * Named business insights (stub for Week 4)
     */
    @GetMapping("/insights")
    public ResponseEntity<InsightsResponse> getBusinessInsights(
            @RequestParam(required = false) String insight,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        try {
            // Stub: return empty insights for Week 1-3
            List<Object> insights = new ArrayList<>();

            InsightsResponse response = new InsightsResponse(insights, OffsetDateTime.now());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DTO for platform activity response
     */
    public static class PlatformActivityDto {
        private String activityDate;
        private int orderCount;
        private String filledQuantity;
        private String grossAmount;
        private OffsetDateTime updatedAt;

        public PlatformActivityDto(String activityDate, int orderCount, String filledQuantity,
                                   String grossAmount, OffsetDateTime updatedAt) {
            this.activityDate = activityDate;
            this.orderCount = orderCount;
            this.filledQuantity = filledQuantity;
            this.grossAmount = grossAmount;
            this.updatedAt = updatedAt;
        }

        // Getters
        public String getActivityDate() { return activityDate; }
        public int getOrderCount() { return orderCount; }
        public String getFilledQuantity() { return filledQuantity; }
        public String getGrossAmount() { return grossAmount; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
    }

    /**
     * DTO for instrument activity response
     */
    public static class InstrumentActivityDto {
        private String activityDate;
        private UUID instrumentId;
        private String symbol;
        private int orderCount;
        private String filledQuantity;
        private String grossAmount;
        private OffsetDateTime updatedAt;

        public InstrumentActivityDto(String activityDate, UUID instrumentId, String symbol,
                                    int orderCount, String filledQuantity, String grossAmount,
                                    OffsetDateTime updatedAt) {
            this.activityDate = activityDate;
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.orderCount = orderCount;
            this.filledQuantity = filledQuantity;
            this.grossAmount = grossAmount;
            this.updatedAt = updatedAt;
        }

        // Getters
        public String getActivityDate() { return activityDate; }
        public UUID getInstrumentId() { return instrumentId; }
        public String getSymbol() { return symbol; }
        public int getOrderCount() { return orderCount; }
        public String getFilledQuantity() { return filledQuantity; }
        public String getGrossAmount() { return grossAmount; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
    }

    /**
     * Response wrapper for insights
     */
    public static class InsightsResponse {
        private List<Object> insights;
        private OffsetDateTime generatedAt;

        public InsightsResponse(List<Object> insights, OffsetDateTime generatedAt) {
            this.insights = insights;
            this.generatedAt = generatedAt;
        }

        public List<Object> getInsights() { return insights; }
        public OffsetDateTime getGeneratedAt() { return generatedAt; }
    }
}
