package com.neueda.leap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "finnhub")
public class FinnhubProperties {
    private String apiKey;
    private String baseUrl = "https://finnhub.io/api/v1";
    private int newsRateLimitDelayMs = 100;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getNewsRateLimitDelayMs() {
        return newsRateLimitDelayMs;
    }

    public void setNewsRateLimitDelayMs(int newsRateLimitDelayMs) {
        this.newsRateLimitDelayMs = newsRateLimitDelayMs;
    }
}
