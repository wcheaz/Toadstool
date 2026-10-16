package com.neueda.leap.service;

import com.neueda.leap.Instrument;
import com.neueda.leap.News;
import com.neueda.leap.dto.EarningsEventResponse;
import com.neueda.leap.dto.FinnhubNewsResponse;
import com.neueda.leap.enums.AssetClass;
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
    private static final int EARNINGS_LOOKAHEAD_DAYS = 30;

    /** Upcoming earnings for tradable equities, refreshed hourly alongside the news. */
    private volatile List<EarningsEventResponse> earningsCalendar = Collections.emptyList();

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

            // Finnhub company news only covers equities; crypto/FX are covered by the general news categories
            List<String> symbols = getEquitySymbols();

            logger.info("Found {} equity instruments for company news fetching", symbols.size());

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

            // Insert new batch, then delete previous batches (keeps old news if this fetch came back empty)
            if (!deduplicatedNews.isEmpty()) {
                logger.info("Inserting {} new articles", deduplicatedNews.size());
                newsRepository.insertNews(deduplicatedNews);

                logger.info("Deleting previous news batches");
                newsRepository.deleteNewsExceptBatchId(batchId);

                logger.info("News refresh completed successfully. Total articles: {}", deduplicatedNews.size());
            } else {
                logger.warn("No news articles fetched in this refresh cycle");
            }

        } catch (Exception e) {
            logger.error("Error during news refresh job: {}", e.getMessage(), e);
        }
    }

    public void refreshEarningsCalendar() {
        try {
            LocalDate today = LocalDate.now();
            String from = today.format(dateFormatter);
            String to = today.plusDays(EARNINGS_LOOKAHEAD_DAYS).format(dateFormatter);

            List<EarningsEventResponse> events = new ArrayList<>();
            for (String symbol : getEquitySymbols()) {
                events.addAll(finnhubNewsService.fetchEarningsCalendar(symbol, from, to));
            }
            events.sort(Comparator.comparing(EarningsEventResponse::getDate));
            earningsCalendar = List.copyOf(events);
            logger.info("Earnings calendar refreshed with {} upcoming events", events.size());
        } catch (Exception e) {
            logger.error("Error during earnings calendar refresh: {}", e.getMessage(), e);
        }
    }

    public List<EarningsEventResponse> getEarningsCalendar() {
        return earningsCalendar;
    }

    private List<String> getEquitySymbols() {
        return instrumentService.listAllInstruments().stream()
                .filter(instrument -> instrument.getAssetClass() == AssetClass.EQUITY)
                .map(Instrument::getSymbol)
                .collect(Collectors.toList());
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

