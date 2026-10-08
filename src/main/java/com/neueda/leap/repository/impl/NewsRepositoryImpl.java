package com.neueda.leap.repository.impl;

import com.neueda.leap.News;
import com.neueda.leap.mapper.NewsMapper;
import com.neueda.leap.repository.NewsRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class NewsRepositoryImpl implements NewsRepository {
    private final NewsMapper newsMapper;

    public NewsRepositoryImpl(NewsMapper newsMapper) {
        this.newsMapper = newsMapper;
    }

    @Override
    public void insertNews(List<News> newsList) {
        for (News news : newsList) {
            newsMapper.insertNews(news);
        }
    }

    @Override
    public void deleteNewsByBatchId(String batchId) {
        newsMapper.deleteNewsByBatchId(batchId);
    }

    @Override
    public List<News> getLatestNewsBySymbol(String symbol, int limit) {
        return newsMapper.selectLatestNewsBySymbol(symbol, limit);
    }

    @Override
    public List<News> getLatestGeneralNews(int limit) {
        return newsMapper.selectLatestGeneralNews(limit);
    }

    @Override
    public List<News> getLatestNewsBySymbols(List<String> symbols, int limit) {
        return newsMapper.selectLatestNewsBySymbols(symbols, limit);
    }

    @Override
    public List<News> getLatestNewsBatch(String batchId) {
        return newsMapper.selectNewsByBatchId(batchId);
    }
}
