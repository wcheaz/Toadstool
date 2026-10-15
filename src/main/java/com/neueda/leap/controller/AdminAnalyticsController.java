package com.neueda.leap.controller;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetDateTimeSerializer;
import com.neueda.leap.dto.AdminDtos.PlatformOverview;
import com.neueda.leap.dto.AdminDtos.TopInstrument;
import com.neueda.leap.dto.PaginatedResponse;
import com.neueda.leap.security.SecurityAccess;
import com.neueda.leap.service.AdminOversightService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Analytics endpoints (admin-only, reporting).
 * Daily figures come from the analytics fact tables, which a trigger on trading.fills keeps current.
 */
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private static final int TOP_INSTRUMENT_LIMIT = 5;

    private final AdminOversightService adminOversightService;

    public AdminAnalyticsController(AdminOversightService adminOversightService) {
        this.adminOversightService = adminOversightService;
    }

    /**
     * GET /api/admin/analytics/platform-activity
     * Platform-wide daily trading activity, newest day first
     */
    @GetMapping("/platform-activity")
    public ResponseEntity<PaginatedResponse<PlatformActivityDto>> getDailyPlatformActivity(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (!SecurityAccess.hasRole(SecurityAccess.ADMIN_ROLE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (limit < 1 || limit > 1000 || offset < 0) {
            return ResponseEntity.badRequest().build();
        }

        try {
            LocalDate from = parseDate(fromDate);
            LocalDate to = parseDate(toDate);
            List<PlatformActivityDto> activity = adminOversightService.listPlatformActivity(from, to, limit, offset).stream()
                    .map(row -> new PlatformActivityDto(row.activityDate.toString(), row.orderCount,
                            plain(row.filledQuantity), plain(row.grossAmount), row.updatedAt))
                    .collect(Collectors.toList());
            int totalCount = adminOversightService.countPlatformActivity(from, to);
            return ResponseEntity.ok(new PaginatedResponse<>(activity, limit, offset, totalCount));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/admin/analytics/instrument-activity
     * Per-instrument daily trading activity, newest day first
     */
    @GetMapping("/instrument-activity")
    public ResponseEntity<PaginatedResponse<InstrumentActivityDto>> getDailyInstrumentActivity(
            @RequestParam(required = false) UUID instrumentId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (!SecurityAccess.hasRole(SecurityAccess.ADMIN_ROLE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (limit < 1 || limit > 1000 || offset < 0) {
            return ResponseEntity.badRequest().build();
        }

        try {
            LocalDate from = parseDate(fromDate);
            LocalDate to = parseDate(toDate);
            List<InstrumentActivityDto> activity = adminOversightService
                    .listInstrumentActivity(instrumentId, from, to, limit, offset).stream()
                    .map(row -> new InstrumentActivityDto(row.activityDate.toString(), row.instrumentId, row.symbol,
                            row.orderCount, plain(row.filledQuantity), plain(row.grossAmount), row.updatedAt))
                    .collect(Collectors.toList());
            int totalCount = adminOversightService.countInstrumentActivity(instrumentId, from, to);
            return ResponseEntity.ok(new PaginatedResponse<>(activity, limit, offset, totalCount));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/admin/analytics/insights
     * Headline platform numbers plus named insights derived from live trading data.
     * Pass insight=<name> to return a single named insight.
     */
    @GetMapping("/insights")
    public ResponseEntity<InsightsResponse> getBusinessInsights(
            @RequestParam(required = false) String insight,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        if (!SecurityAccess.hasRole(SecurityAccess.ADMIN_ROLE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            PlatformOverview overview = adminOversightService.getPlatformOverview();
            List<TopInstrument> topInstruments = adminOversightService.listTopInstruments(TOP_INSTRUMENT_LIMIT);
            List<Object> insights = buildInsights(overview, topInstruments).stream()
                    .filter(item -> insight == null || insight.isBlank() || item.getName().equals(insight))
                    .collect(Collectors.<Object>toList());
            return ResponseEntity.ok(new InsightsResponse(insights, OffsetDateTime.now(), overview, topInstruments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<Insight> buildInsights(PlatformOverview overview, List<TopInstrument> topInstruments) {
        List<Insight> insights = new ArrayList<>();
        if (overview == null) {
            return insights;
        }
        if (!topInstruments.isEmpty()) {
            TopInstrument top = topInstruments.get(0);
            insights.add(new Insight("most-traded-instrument", "Most traded instrument (30 days)",
                    top.symbol, top.fillCount + " fills · $" + money(top.grossAmount) + " gross"));
        }
        insights.add(new Insight("gross-volume-30d", "Gross traded volume (30 days)",
                "$" + money(overview.grossVolumeLast30Days), overview.ordersLast30Days + " orders submitted"));
        insights.add(new Insight("fees-30d", "Fees collected (30 days)",
                "$" + money(overview.feesLast30Days), "Recorded on each fill"));
        insights.add(new Insight("open-orders", "Orders awaiting execution",
                String.valueOf(overview.openOrders), overview.filledOrders + " of " + overview.totalOrders + " orders filled"));
        insights.add(new Insight("failed-logins-24h", "Failed sign-ins (24 hours)",
                String.valueOf(overview.failedLoginsLast24Hours), "From the login audit log"));
        return insights;
    }

    private static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00" : String.format("%,.2f", value.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * DTO for platform activity response
     */
    public static class PlatformActivityDto {
        private String activityDate;
        private int orderCount;
        private String filledQuantity;
        private String grossAmount;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
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
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
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
     * A single named insight
     */
    public static class Insight {
        private final String name;
        private final String title;
        private final String value;
        private final String detail;

        public Insight(String name, String title, String value, String detail) {
            this.name = name;
            this.title = title;
            this.value = value;
            this.detail = detail;
        }

        public String getName() { return name; }
        public String getTitle() { return title; }
        public String getValue() { return value; }
        public String getDetail() { return detail; }
    }

    /**
     * Response wrapper for insights
     */
    public static class InsightsResponse {
        private List<Object> insights;
        @JsonSerialize(using = OffsetDateTimeSerializer.class)
        private OffsetDateTime generatedAt;
        private PlatformOverview overview;
        private List<TopInstrument> topInstruments;

        public InsightsResponse(List<Object> insights, OffsetDateTime generatedAt) {
            this(insights, generatedAt, null, List.of());
        }

        public InsightsResponse(List<Object> insights, OffsetDateTime generatedAt,
                                PlatformOverview overview, List<TopInstrument> topInstruments) {
            this.insights = insights;
            this.generatedAt = generatedAt;
            this.overview = overview;
            this.topInstruments = topInstruments;
        }

        public List<Object> getInsights() { return insights; }
        public OffsetDateTime getGeneratedAt() { return generatedAt; }
        public PlatformOverview getOverview() { return overview; }
        public List<TopInstrument> getTopInstruments() { return topInstruments; }
    }
}
