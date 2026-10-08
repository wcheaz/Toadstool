package com.neueda.leap.job;

import com.neueda.leap.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsRefreshJob {
    private static final Logger logger = LoggerFactory.getLogger(NewsRefreshJob.class);
    private final NewsService newsService;

    public NewsRefreshJob(NewsService newsService) {
        this.newsService = newsService;
    }

    @Scheduled(fixedRate = 3600000)
    public void refreshNews() {
        logger.info("News refresh job triggered");
        newsService.refreshNewsForAllAccounts();
    }
}
