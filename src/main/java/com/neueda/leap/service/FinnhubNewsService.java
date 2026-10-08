package com.neueda.leap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.leap.config.FinnhubProperties;
import com.neueda.leap.dto.FinnhubNewsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class FinnhubNewsService {
    private static final Logger logger = LoggerFactory.getLogger(FinnhubNewsService.class);
    private final RestTemplate restTemplate;
    private final FinnhubProperties properties;
    private final ObjectMapper objectMapper;
    private static final int RATE_LIMIT_DELAY_MS = 100;

    public FinnhubNewsService(RestTemplate restTemplate, FinnhubProperties properties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<FinnhubNewsResponse> fetchCompanyNews(String symbol, String fromDate, String toDate) {
        try {
            applyRateLimit();
            // Use the /company-news endpoint for company-specific news
            String url = properties.getBaseUrl() + "/company-news?symbol=" + symbol.toUpperCase() +
                        "&from=" + fromDate + "&to=" + toDate + "&token=" + properties.getApiKey();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.warn("Finnhub API returned non-success status for symbol {}", symbol);
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            List<FinnhubNewsResponse> newsList = new ArrayList<>();

            if (root.isArray()) {
                root.forEach(newsItem -> {
                    try {
                        FinnhubNewsResponse news = new FinnhubNewsResponse(
                            newsItem.get("headline").asText(""),
                            newsItem.get("summary").asText(""),
                            newsItem.get("source").asText(""),
                            newsItem.get("datetime").asLong(0),
                            newsItem.get("image").asText(""),
                            newsItem.get("url").asText(""),
                            "company",
                            symbol,
                            newsItem.get("related").asText("")
                        );
                        newsList.add(news);
                    } catch (Exception e) {
                        logger.error("Error parsing company news item for {}: {}", symbol, e.getMessage());
                    }
                });
            }
            return newsList;
        } catch (Exception e) {
            logger.error("Error fetching company news for symbol {}: {}", symbol, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<FinnhubNewsResponse> fetchGeneralNews(String category) {
        try {
            List<FinnhubNewsResponse> allNews = new ArrayList<>();

            // Fetch from multiple categories for variety
            String[] categories = {"general", "forex", "crypto"};
            for (String cat : categories) {
                applyRateLimit();
                String url = properties.getBaseUrl() + "/news?category=" + cat + "&token=" + properties.getApiKey();
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    logger.warn("Finnhub API returned non-success status for category {}", cat);
                    continue;
                }

                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.isArray()) {
                    root.forEach(newsItem -> {
                        try {
                            FinnhubNewsResponse news = new FinnhubNewsResponse(
                                newsItem.get("headline").asText(""),
                                newsItem.get("summary").asText(""),
                                newsItem.get("source").asText(""),
                                newsItem.get("datetime").asLong(0),
                                newsItem.get("image").asText(""),
                                newsItem.get("url").asText(""),
                                cat,
                                newsItem.get("related").asText(""),
                                newsItem.get("related").asText("")
                            );
                            allNews.add(news);
                        } catch (Exception e) {
                            logger.error("Error parsing news item from category {}: {}", cat, e.getMessage());
                        }
                    });
                }
            }
            return allNews;
        } catch (Exception e) {
            logger.error("Error fetching general news: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private void applyRateLimit() {
        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Rate limit sleep interrupted");
        }
    }
}
