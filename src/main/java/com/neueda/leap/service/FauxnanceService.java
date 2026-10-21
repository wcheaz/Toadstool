package com.neueda.leap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.leap.config.FauxnanceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FauxnanceService {
    private static final Logger logger = LoggerFactory.getLogger(FauxnanceService.class);
    private final RestTemplate restTemplate;
    private final FauxnanceProperties properties;
    private final ObjectMapper objectMapper;

    private static class CacheEntry<T> {
        T value;
        long timestamp;
        CacheEntry(T value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    private final ConcurrentHashMap<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60000; // 1 minute

    public FauxnanceService(RestTemplate restTemplate, FauxnanceProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Latest quote in USD, the platform's account currency. Quotes in other currencies (e.g. INR for
     * Indian equities, pence for UK equities, the quote currency of an FX pair) are converted with the
     * live FX rate; the local price and rate are kept on the quote. Null when no quote or rate is available.
     */
    public QuoteResponse getQuote(String symbol) {
        return toUsd(getRawQuote(symbol));
    }

    /** Provider quote in its own currency, cached for CACHE_TTL_MS. */
    private QuoteResponse getRawQuote(String symbol) {
        String cacheKey = quoteCacheKey(symbol);
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(CACHE_TTL_MS)) {
            return (QuoteResponse) cached.value;
        }

        QuoteResponse quote = fetchQuote(symbol);
        if (quote != null) {
            cacheQuote(symbol, quote);
        }
        return quote;
    }

    public List<QuoteResponse> getQuotes(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> requestedSymbols = symbols.stream()
                .filter(Objects::nonNull)
                .map(this::extractBaseSymbol)
                .distinct()
                .collect(Collectors.toList());
        Map<String, QuoteResponse> quotesBySymbol = new HashMap<>();
        List<String> missingSymbols = new ArrayList<>();

        for (String symbol : requestedSymbols) {
            CacheEntry<?> cached = cache.get(quoteCacheKey(symbol));
            if (cached != null && !cached.isExpired(CACHE_TTL_MS)) {
                QuoteResponse quote = (QuoteResponse) cached.value;
                quotesBySymbol.put(symbol, quote);
            } else {
                missingSymbols.add(symbol);
            }
        }

        if (!missingSymbols.isEmpty()) {
            List<QuoteResponse> fetchedQuotes = fetchQuotes(missingSymbols);
            for (QuoteResponse quote : fetchedQuotes) {
                cacheQuote(quote.getSymbol(), quote);
                quotesBySymbol.put(extractBaseSymbol(quote.getSymbol()), quote);
            }
        }

        return requestedSymbols.stream()
                .map(quotesBySymbol::get)
                .map(this::toUsd)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void cacheQuote(String requestedSymbol, QuoteResponse quote) {
        cache.put(quoteCacheKey(requestedSymbol), new CacheEntry<>(quote));
        cache.put(quoteCacheKey(quote.getSymbol()), new CacheEntry<>(quote));
    }

    private String quoteCacheKey(String symbol) {
        return "quote:" + extractBaseSymbol(symbol);
    }

    /**
     * Daily candles in USD. Candles in another currency are converted with that day's FX close
     * (falling back to the latest rate for days without one); empty when no rate is available.
     */
    @SuppressWarnings("unchecked")
    public List<CandleResponse> getCandles(String symbol, String fromDate, String toDate) {
        String cacheKey = "candles:" + symbol + ":" + fromDate + ":" + toDate;
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(CACHE_TTL_MS)) {
            return (List<CandleResponse>) cached.value;
        }

        List<CandleResponse> candles = toUsd(getRawCandles(symbol, fromDate, toDate), fromDate, toDate);
        cache.put(cacheKey, new CacheEntry<>(candles));
        return candles;
    }

    /** Provider candles in their own currency, cached for CACHE_TTL_MS. */
    private CandleSeries getRawCandles(String symbol, String fromDate, String toDate) {
        String cacheKey = "rawcandles:" + extractBaseSymbol(symbol) + ":" + fromDate + ":" + toDate;
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(CACHE_TTL_MS)) {
            return (CandleSeries) cached.value;
        }
        CandleSeries series = fetchCandles(symbol, fromDate, toDate);
        if (!series.candles.isEmpty()) {
            cache.put(cacheKey, new CacheEntry<>(series));
        }
        return series;
    }

    // ---- Currency conversion to USD ----

    /** Currencies Fauxnance quotes as FX:{CCY}USD (USD per unit); others are quoted as FX:USD{CCY}. */
    private static final Set<String> QUOTED_AGAINST_USD = Set.of("EUR", "GBP", "AUD", "NZD");
    /** UK equities are priced in pence (GBp/GBX). */
    private static final Set<String> PENCE_STERLING = Set.of("GBp", "GBX", "GBx");

    private static boolean isUsd(String currency) {
        return currency == null || currency.isBlank() || "USD".equals(currency);
    }

    /** FX pair whose price gives the USD value of one unit of the currency. */
    private static String usdRatePair(String currency) {
        return QUOTED_AGAINST_USD.contains(currency) ? "FX:" + currency + "USD" : "FX:USD" + currency;
    }

    private static double usdPerUnitFromPair(String currency, double pairPrice) {
        return QUOTED_AGAINST_USD.contains(currency) ? pairPrice : 1.0 / pairPrice;
    }

    /** Current USD value of one unit of the currency, or null if no rate is available. */
    private Double usdPerUnit(String currency) {
        if (isUsd(currency)) {
            return 1.0;
        }
        if (PENCE_STERLING.contains(currency)) {
            Double perPound = usdPerUnit("GBP");
            return perPound != null ? perPound / 100.0 : null;
        }
        QuoteResponse pair = getRawQuote(usdRatePair(currency));
        if (pair == null || !(pair.getPrice() > 0)) {
            logger.warn("No FX rate available to convert {} to USD", currency);
            return null;
        }
        return usdPerUnitFromPair(currency, pair.getPrice());
    }

    private QuoteResponse toUsd(QuoteResponse quote) {
        if (quote == null || isUsd(quote.getCurrency())) {
            return quote;
        }
        Double rate = usdPerUnit(quote.getCurrency());
        if (rate == null) {
            return null; // never price in the wrong currency
        }
        QuoteResponse converted = new QuoteResponse(quote.getSymbol(), quote.getPrice() * rate, quote.getBid() * rate,
                quote.getAsk() * rate, quote.getChange() * rate, quote.getChangePercent(), quote.getAsOf());
        converted.localCurrency = quote.getCurrency();
        converted.localPrice = quote.getPrice();
        converted.fxRate = rate;
        return converted;
    }

    private List<CandleResponse> toUsd(CandleSeries series, String fromDate, String toDate) {
        if (isUsd(series.currency) || series.candles.isEmpty()) {
            return series.candles;
        }
        Double latestRate = usdPerUnit(series.currency);
        if (latestRate == null) {
            return Collections.emptyList();
        }
        Map<String, Double> dailyRates = dailyUsdRates(series.currency, fromDate, toDate);
        List<CandleResponse> converted = new ArrayList<>();
        for (CandleResponse candle : series.candles) {
            double rate = dailyRates.getOrDefault(candle.getDate(), latestRate);
            converted.add(new CandleResponse(candle.getDate(), candle.getOpen() * rate, candle.getHigh() * rate,
                    candle.getLow() * rate, candle.getClose() * rate, candle.getVolume()));
        }
        return converted;
    }

    /** USD per unit of the currency for each day, from the FX pair's daily closes. */
    private Map<String, Double> dailyUsdRates(String currency, String fromDate, String toDate) {
        double scale = 1.0;
        String rateCurrency = currency;
        if (PENCE_STERLING.contains(currency)) {
            rateCurrency = "GBP";
            scale = 0.01;
        }
        Map<String, Double> rates = new HashMap<>();
        for (CandleResponse candle : getRawCandles(usdRatePair(rateCurrency), fromDate, toDate).candles) {
            if (candle.getClose() > 0) {
                rates.put(candle.getDate(), usdPerUnitFromPair(rateCurrency, candle.getClose()) * scale);
            }
        }
        return rates;
    }

    /** Candles plus the currency they are priced in. */
    private static class CandleSeries {
        final String currency;
        final List<CandleResponse> candles;

        CandleSeries(String currency, List<CandleResponse> candles) {
            this.currency = currency;
            this.candles = candles;
        }
    }

    public SymbolResponse getSymbol(String symbol) {
        String cacheKey = "symbol:" + symbol;
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(CACHE_TTL_MS)) {
            return (SymbolResponse) cached.value;
        }

        SymbolResponse symbolData = fetchSymbol(symbol);
        if (symbolData != null) {
            cache.put(cacheKey, new CacheEntry<>(symbolData));
        }
        return symbolData;
    }

    private QuoteResponse fetchQuote(String symbol) {
        try {
            String baseSymbol = extractBaseSymbol(symbol);
            String url = properties.getBaseUrl() + "/quotes/" + baseSymbol;
            ResponseEntity<String> response = makeRequest(url);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Fauxnance API returned non-success status {} for symbol {}", response.getStatusCode(), symbol);
                return null;
            }
            
            if (response.getBody() == null) {
                logger.warn("Fauxnance API returned empty body for symbol {}", symbol);
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            if (data != null) {
                QuoteResponse quote = new QuoteResponse(
                        data.get("symbol").asText(),
                        data.get("price").asDouble(),
                        data.get("bid").asDouble(),
                        data.get("ask").asDouble(),
                        data.get("change").asDouble(),
                        data.get("changePercent").asDouble(),
                        data.get("asOf").asText()
                );
                quote.currency = data.path("currency").asText("USD");
                return quote;
            }
            logger.warn("Fauxnance API response missing 'data' field for symbol {}", symbol);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching quote for symbol {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    private List<QuoteResponse> fetchQuotes(List<String> symbols) {
        try {
            List<String> baseSymbols = symbols.stream()
                    .map(this::extractBaseSymbol)
                    .collect(Collectors.toList());
            String url = properties.getBaseUrl() + "/quotes?symbols=" + String.join(",", baseSymbols);
            ResponseEntity<String> response = makeRequest(url);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Fauxnance API returned non-success status {} for batch quotes", response.getStatusCode());
                return Collections.emptyList();
            }

            if (response.getBody() == null) {
                logger.warn("Fauxnance API returned empty body for batch quotes");
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            List<QuoteResponse> quotes = new ArrayList<>();
            if (data != null && data.has("quotes")) {
                JsonNode quotesArray = data.get("quotes");
                quotesArray.forEach(entry -> {
                    if (entry.has("quote") && !entry.has("error")) {
                        JsonNode quote = entry.get("quote");
                        QuoteResponse parsed = new QuoteResponse(
                                quote.get("symbol").asText(),
                                quote.get("price").asDouble(),
                                quote.get("bid").asDouble(),
                                quote.get("ask").asDouble(),
                                quote.get("change").asDouble(),
                                quote.get("changePercent").asDouble(),
                                quote.get("asOf").asText()
                        );
                        parsed.currency = quote.path("currency").asText("USD");
                        quotes.add(parsed);
                    } else if (entry.has("error")) {
                        logger.warn("Fauxnance batch quote error for symbol {}: {}",
                                entry.get("symbol").asText(),
                                entry.get("error").get("message").asText());
                    }
                });
            }
            return quotes;
        } catch (Exception e) {
            logger.error("Error fetching batch quotes: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private CandleSeries fetchCandles(String symbol, String fromDate, String toDate) {
        try {
            String baseSymbol = extractBaseSymbol(symbol);
            String url = properties.getBaseUrl() + "/candles/" + baseSymbol + "?from=" + fromDate + "&to=" + toDate;
            ResponseEntity<String> response = makeRequest(url);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Fauxnance API returned non-success status {} for candles {}", response.getStatusCode(), symbol);
                return new CandleSeries("USD", Collections.emptyList());
            }
            
            if (response.getBody() == null) {
                logger.warn("Fauxnance API returned empty body for candles {}", symbol);
                return new CandleSeries("USD", Collections.emptyList());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            List<CandleResponse> candles = new ArrayList<>();
            if (data != null && data.has("candles")) {
                JsonNode candlesArray = data.get("candles");
                candlesArray.forEach(candle -> candles.add(new CandleResponse(
                        candle.get("date").asText(),
                        candle.get("open").asDouble(),
                        candle.get("high").asDouble(),
                        candle.get("low").asDouble(),
                        candle.get("close").asDouble(),
                        candle.get("volume").asLong()
                )));
            }
            String currency = data != null ? data.path("currency").asText("USD") : "USD";
            return new CandleSeries(currency, candles);
        } catch (Exception e) {
            logger.error("Error fetching candles for symbol {}: {}", symbol, e.getMessage(), e);
            return new CandleSeries("USD", Collections.emptyList());
        }
    }

    private SymbolResponse fetchSymbol(String symbol) {
        try {
            String baseSymbol = extractBaseSymbol(symbol);
            String url = properties.getBaseUrl() + "/symbols/" + baseSymbol;
            ResponseEntity<String> response = makeRequest(url);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Fauxnance API returned non-success status {} for symbol {}", response.getStatusCode(), symbol);
                return null;
            }
            
            if (response.getBody() == null) {
                logger.warn("Fauxnance API returned empty body for symbol {}", symbol);
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            if (data != null) {
                return new SymbolResponse(
                        data.get("symbol").asText(),
                        data.get("name").asText(),
                        data.get("currency").asText()
                );
            }
            logger.warn("Fauxnance API response missing 'data' field for symbol {}", symbol);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching symbol {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    private ResponseEntity<String> makeRequest(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", properties.getApiKey());
        headers.set("Accept", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    private String extractBaseSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return symbol;
        }
        int slashIndex = symbol.indexOf('/');
        if (slashIndex > 0) {
            return symbol.substring(0, slashIndex);
        }
        return symbol;
    }

    public static class QuoteResponse {
        public String symbol;
        public double price;
        public double bid;
        public double ask;
        public double change;
        public double changePercent;
        public String asOf;
        /** Currency of price/bid/ask/change: USD once converted by FauxnanceService.getQuote. */
        public String currency = "USD";
        /** Provider currency and price before conversion; null for USD-priced instruments. */
        public String localCurrency;
        public Double localPrice;
        /** USD per unit of localCurrency used for the conversion. */
        public Double fxRate;

        public QuoteResponse(String symbol, double price, double bid, double ask, double change, double changePercent, String asOf) {
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
        public String getCurrency() { return currency; }
        public String getLocalCurrency() { return localCurrency; }
        public Double getLocalPrice() { return localPrice; }
        public Double getFxRate() { return fxRate; }
    }

    public static class CandleResponse {
        public String date;
        public double open;
        public double high;
        public double low;
        public double close;
        public long volume;

        public CandleResponse(String date, double open, double high, double low, double close, long volume) {
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

    public static class SymbolResponse {
        public String symbol;
        public String name;
        public String currency;

        public SymbolResponse(String symbol, String name, String currency) {
            this.symbol = symbol;
            this.name = name;
            this.currency = currency;
        }

        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public String getCurrency() { return currency; }
    }
}
