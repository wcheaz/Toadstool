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
    private static final long CACHE_TTL_MS = 5000; // 5 seconds

    public FauxnanceService(RestTemplate restTemplate, FauxnanceProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public QuoteResponse getQuote(String symbol) {
        String cacheKey = "quote:" + symbol;
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(CACHE_TTL_MS)) {
            return (QuoteResponse) cached.value;
        }

        QuoteResponse quote = fetchQuote(symbol);
        if (quote != null) {
            cache.put(cacheKey, new CacheEntry<>(quote));
        }
        return quote;
    }

    public List<QuoteResponse> getQuotes(List<String> symbols) {
        String cacheKey = "quotes:" + String.join(",", symbols);
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(CACHE_TTL_MS)) {
            return (List<QuoteResponse>) cached.value;
        }

        List<QuoteResponse> quotes = fetchQuotes(symbols);
        if (quotes != null) {
            cache.put(cacheKey, new CacheEntry<>(quotes));
        }
        return quotes;
    }

    public List<CandleResponse> getCandles(String symbol, String fromDate, String toDate) {
        String cacheKey = "candles:" + symbol + ":" + fromDate + ":" + toDate;
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(CACHE_TTL_MS)) {
            return (List<CandleResponse>) cached.value;
        }

        List<CandleResponse> candles = fetchCandles(symbol, fromDate, toDate);
        if (candles != null) {
            cache.put(cacheKey, new CacheEntry<>(candles));
        }
        return candles;
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
            String url = properties.getBaseUrl() + "/quotes/" + symbol;
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
                return new QuoteResponse(
                        data.get("symbol").asText(),
                        data.get("price").asDouble(),
                        data.get("bid").asDouble(),
                        data.get("ask").asDouble(),
                        data.get("change").asDouble(),
                        data.get("changePercent").asDouble(),
                        data.get("asOf").asText()
                );
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
            String url = properties.getBaseUrl() + "/quotes?symbols=" + String.join(",", symbols);
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
            if (data != null && data.isObject()) {
                data.fields().forEachRemaining(entry -> {
                    JsonNode quote = entry.getValue();
                    quotes.add(new QuoteResponse(
                            quote.get("symbol").asText(),
                            quote.get("price").asDouble(),
                            quote.get("bid").asDouble(),
                            quote.get("ask").asDouble(),
                            quote.get("change").asDouble(),
                            quote.get("changePercent").asDouble(),
                            quote.get("asOf").asText()
                    ));
                });
            }
            return quotes;
        } catch (Exception e) {
            logger.error("Error fetching batch quotes: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<CandleResponse> fetchCandles(String symbol, String fromDate, String toDate) {
        try {
            String url = properties.getBaseUrl() + "/candles/" + symbol + "?from=" + fromDate + "&to=" + toDate;
            ResponseEntity<String> response = makeRequest(url);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Fauxnance API returned non-success status {} for candles {}", response.getStatusCode(), symbol);
                return Collections.emptyList();
            }
            
            if (response.getBody() == null) {
                logger.warn("Fauxnance API returned empty body for candles {}", symbol);
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            List<CandleResponse> candles = new ArrayList<>();
            if (data != null && data.isArray()) {
                data.forEach(candle -> candles.add(new CandleResponse(
                        candle.get("date").asText(),
                        candle.get("open").asDouble(),
                        candle.get("high").asDouble(),
                        candle.get("low").asDouble(),
                        candle.get("close").asDouble(),
                        candle.get("volume").asLong()
                )));
            }
            return candles;
        } catch (Exception e) {
            logger.error("Error fetching candles for symbol {}: {}", symbol, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private SymbolResponse fetchSymbol(String symbol) {
        try {
            String url = properties.getBaseUrl() + "/symbols/" + symbol;
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

    public static class QuoteResponse {
        public String symbol;
        public double price;
        public double bid;
        public double ask;
        public double change;
        public double changePercent;
        public String asOf;

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

