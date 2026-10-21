package com.neueda.leap.job;

import com.neueda.leap.mapper.AnalyticsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the analytics daily facts current, off the trading path (BR-16): a fill's transaction writes
 * only trading data, and this job projects new fills into the facts that reporting reads.
 * Reports trail live trading by up to one interval.
 */
@Component
public class AnalyticsProjectionJob {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsProjectionJob.class);
    private final AnalyticsMapper analyticsMapper;

    public AnalyticsProjectionJob(AnalyticsMapper analyticsMapper) {
        this.analyticsMapper = analyticsMapper;
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void projectNewFills() {
        try {
            int projected = analyticsMapper.projectNewFills();
            if (projected > 0) {
                logger.info("Projected {} new fill(s) into analytics facts", projected);
            }
        } catch (Exception e) {
            // Facts catch up on the next run; trading is unaffected
            logger.error("Analytics projection failed: {}", e.getMessage(), e);
        }
    }
}
