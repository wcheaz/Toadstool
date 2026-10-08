package com.neueda.leap;

import java.time.OffsetDateTime;
import java.util.UUID;

public class News {
    private UUID newsId;
    private String symbol;
    private String category;
    private String headline;
    private String summary;
    private String source;
    private Long publishedTimestamp;
    private String imageUrl;
    private String sourceUrl;
    private OffsetDateTime createdAt;
    private String batchId;

    public News() {}

    public News(String symbol, String category, String headline, String summary, String source,
                Long publishedTimestamp, String imageUrl, String sourceUrl, String batchId) {
        this.symbol = symbol;
        this.category = category;
        this.headline = headline;
        this.summary = summary;
        this.source = source;
        this.publishedTimestamp = publishedTimestamp;
        this.imageUrl = imageUrl;
        this.sourceUrl = sourceUrl;
        this.batchId = batchId;
    }

    public News(UUID newsId, String symbol, String category, String headline, String summary,
                String source, Long publishedTimestamp, String imageUrl, String sourceUrl,
                OffsetDateTime createdAt, String batchId) {
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

    public UUID getNewsId() { return newsId; }
    public void setNewsId(UUID newsId) { this.newsId = newsId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getPublishedTimestamp() { return publishedTimestamp; }
    public void setPublishedTimestamp(Long publishedTimestamp) { this.publishedTimestamp = publishedTimestamp; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
}
