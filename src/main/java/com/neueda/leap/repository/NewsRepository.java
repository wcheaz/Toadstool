package com.neueda.leap.repository;

import com.neueda.leap.News;
import java.util.List;
import java.util.UUID;

public interface NewsRepository {
    void insertNews(List<News> newsList);
    void deleteNewsByBatchId(String batchId);
    List<News> getLatestNewsBySymbol(String symbol, int limit);
    List<News> getLatestGeneralNews(int limit);
    List<News> getLatestNewsBySymbols(List<String> symbols, int limit);
    List<News> getLatestNewsBatch(String batchId);
}
