package com.neueda.leap.dto;

public class FinnhubNewsResponse {
    public String headline;
    public String summary;
    public String source;
    public long datetime;
    public String image;
    public String url;
    public String category;
    public String symbol;
    public String related;

    public FinnhubNewsResponse() {}

    public FinnhubNewsResponse(String headline, String summary, String source, long datetime,
                              String image, String url, String category, String symbol, String related) {
        this.headline = headline;
        this.summary = summary;
        this.source = source;
        this.datetime = datetime;
        this.image = image;
        this.url = url;
        this.category = category;
        this.symbol = symbol;
        this.related = related;
    }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public long getDatetime() { return datetime; }
    public void setDatetime(long datetime) { this.datetime = datetime; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getRelated() { return related; }
    public void setRelated(String related) { this.related = related; }
}
