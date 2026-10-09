package com.neueda.leap.controller;

import com.neueda.leap.Instrument;
import com.neueda.leap.News;
import com.neueda.leap.service.InstrumentService;
import com.neueda.leap.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class NewsController {
    private static final Logger logger = LoggerFactory.getLogger(NewsController.class);
    private final NewsService newsService;
    private final InstrumentService instrumentService;

    public NewsController(NewsService newsService, InstrumentService instrumentService) {
        this.newsService = newsService;
        this.instrumentService = instrumentService;
    }

    @GetMapping("/latest")
    public ResponseEntity<List<NewsResponse>> getLatestNews(
            @RequestParam(required = false) String symbols) {
        try {
            List<News> news;
            if (symbols != null && !symbols.isEmpty()) {
                List<String> symbolList = Arrays.asList(symbols.split(","));
                news = newsService.getLatestNewsBySymbols(symbolList);
            } else {
                // Dynamically fetch news for all available market assets + market news
                List<Instrument> allInstruments = instrumentService.listAllInstruments();
                List<String> marketAssetSymbols = allInstruments.stream()
                        .map(Instrument::getSymbol)
                        .collect(Collectors.toList());

                List<News> companyNews = newsService.getLatestNewsBySymbols(marketAssetSymbols);
                List<News> marketNews = newsService.getGeneralMarketNews();

                news = new ArrayList<>(companyNews);
                news.addAll(marketNews);
            }

            // Deduplicate by headline
            Set<String> seenHeadlines = new HashSet<>();
            List<News> deduplicatedNews = news.stream()
                    .filter(n -> seenHeadlines.add(n.getHeadline()))
                    .collect(Collectors.toList());

            List<NewsResponse> responses = deduplicatedNews.stream()
                    .map(this::convertToResponse)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching latest news: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/market")
    public ResponseEntity<List<NewsResponse>> getMarketNews() {
        try {
            List<News> news = newsService.getGeneralMarketNews();
            List<NewsResponse> responses = news.stream()
                    .map(this::convertToResponse)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching market news: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshNews() {
        try {
            newsService.refreshNewsForAllAccounts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "News refreshed successfully");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error refreshing news: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error refreshing news: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private NewsResponse convertToResponse(News news) {
        return new NewsResponse(
                news.getNewsId().toString(),
                news.getSymbol(),
                news.getCategory(),
                news.getHeadline(),
                news.getSummary(),
                news.getSource(),
                news.getPublishedTimestamp(),
                news.getImageUrl(),
                news.getSourceUrl(),
                news.getCreatedAt().toString(),
                news.getBatchId()
        );
    }

    public static class NewsResponse {
        public String newsId;
        public String symbol;
        public String category;
        public String headline;
        public String summary;
        public String source;
        public Long publishedTimestamp;
        public String imageUrl;
        public String sourceUrl;
        public String createdAt;
        public String batchId;

        public NewsResponse(String newsId, String symbol, String category, String headline,
                          String summary, String source, Long publishedTimestamp,
                          String imageUrl, String sourceUrl, String createdAt, String batchId) {
            this.newsId = newsId;
            this.symbol = symbol;
            this.category = category;
            this.headline = headline;
            this.summary = summary;
            this.source = source;
            this.publishedTimestamp = publishedTimestamp;
            this.imageUrl = imageUrl;
            this.sourceUrl = sourceUrl;
            this.createdAt = createdAt;
            this.batchId = batchId;
        }
    }
}
