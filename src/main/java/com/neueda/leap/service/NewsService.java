package com.neueda.leap.service;

import com.neueda.leap.Instrument;
import com.neueda.leap.News;
import com.neueda.leap.dto.FinnhubNewsResponse;
import com.neueda.leap.repository.NewsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewsService {
    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);
    private final FinnhubNewsService finnhubNewsService;
    private final NewsRepository newsRepository;
    private final InstrumentService instrumentService;
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_NEWS_PER_SYMBOL = 5;
    private static final int MAX_GENERAL_NEWS = 5;

    public NewsService(FinnhubNewsService finnhubNewsService, NewsRepository newsRepository, InstrumentService instrumentService) {
        this.finnhubNewsService = finnhubNewsService;
        this.newsRepository = newsRepository;
        this.instrumentService = instrumentService;
    }

    public void refreshNewsForAllAccounts() {
        try {
            logger.info("Starting news refresh job");
            String batchId = String.valueOf(System.currentTimeMillis());
            List<News> allNews = new ArrayList<>();

            // Get all tradable instruments from the trade side
            List<Instrument> tradableInstruments = instrumentService.listAllInstruments();
            List<String> symbols = tradableInstruments.stream()
                    .map(Instrument::getSymbol)
                    .collect(Collectors.toList());

            logger.info("Found {} tradable instruments for news fetching", symbols.size());

            // Fetch company news for each symbol (max 5 per symbol)
            LocalDate today = LocalDate.now();
            LocalDate fromDate = today.minusDays(7);
            String from = fromDate.format(dateFormatter);
            String to = today.format(dateFormatter);

            for (String symbol : symbols) {
                List<FinnhubNewsResponse> companyNews = finnhubNewsService.fetchCompanyNews(symbol, from, to);

                // Limit to MAX_NEWS_PER_SYMBOL articles
                List<FinnhubNewsResponse> limitedNews = companyNews.stream()
                        .limit(MAX_NEWS_PER_SYMBOL)
                        .collect(Collectors.toList());

                logger.info("Fetched {} news articles for symbol {} (limited to {})", companyNews.size(), symbol, limitedNews.size());

                for (FinnhubNewsResponse finnhubNews : limitedNews) {
                    News news = new News(
                        symbol,
                        "company",
                        finnhubNews.getHeadline(),
                        finnhubNews.getSummary(),
                        finnhubNews.getSource(),
                        finnhubNews.getDatetime(),
                        finnhubNews.getImage(),
                        finnhubNews.getUrl(),
                        batchId
                    );
                    allNews.add(news);
                }
            }

            // Fetch general market news (max 5)
            List<FinnhubNewsResponse> generalNews = finnhubNewsService.fetchGeneralNews("general");
            List<FinnhubNewsResponse> limitedGeneralNews = generalNews.stream()
                    .limit(MAX_GENERAL_NEWS)
                    .collect(Collectors.toList());

            logger.info("Fetched {} general news articles (limited to {})", generalNews.size(), limitedGeneralNews.size());

            for (FinnhubNewsResponse finnhubNews : limitedGeneralNews) {
                News news = new News(
                    "MARKET",
                    "market",
                    finnhubNews.getHeadline(),
                    finnhubNews.getSummary(),
                    finnhubNews.getSource(),
                    finnhubNews.getDatetime(),
                    finnhubNews.getImage(),
                    finnhubNews.getUrl(),
                    batchId
                );
                allNews.add(news);
            }

            // Deduplicate articles by headline to avoid storing same news multiple times
            Set<String> seenHeadlines = new HashSet<>();
            List<News> deduplicatedNews = new ArrayList<>();
            for (News news : allNews) {
                if (seenHeadlines.add(news.getHeadline())) {
                    deduplicatedNews.add(news);
                }
            }
            logger.info("Deduplicated {} articles down to {}", allNews.size(), deduplicatedNews.size());

            // Delete old news and insert new batch
            if (!deduplicatedNews.isEmpty()) {
                logger.info("Deleting old news batch");
                newsRepository.deleteNewsByBatchId(batchId);

                logger.info("Inserting {} new articles", deduplicatedNews.size());
                newsRepository.insertNews(deduplicatedNews);

                logger.info("News refresh completed successfully. Total articles: {}", deduplicatedNews.size());
            } else {
                logger.warn("No news articles fetched in this refresh cycle");
            }

        } catch (Exception e) {
            logger.error("Error during news refresh job: {}", e.getMessage(), e);
        }
    }

    public List<News> getLatestNewsBySymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }
        return newsRepository.getLatestNewsBySymbols(symbols, 100);
    }

    public List<News> getGeneralMarketNews() {
        return newsRepository.getLatestGeneralNews(MAX_GENERAL_NEWS);
    }

    public List<News> getLatestNews(List<String> symbols) {
        if (symbols != null && !symbols.isEmpty()) {
            return newsRepository.getLatestNewsBySymbols(symbols, 100);
        }
        List<News> companyNews = newsRepository.getLatestGeneralNews(50);
        List<News> marketNews = newsRepository.getLatestGeneralNews(MAX_GENERAL_NEWS);
        companyNews.addAll(marketNews);
        return companyNews;
    }
}

