-- Create news table for storing Finnhub news articles
CREATE TABLE IF NOT EXISTS trading.news (
  news_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  symbol VARCHAR(20) NOT NULL,
  category VARCHAR(50),
  headline VARCHAR(500) NOT NULL,
  summary TEXT,
  source VARCHAR(100),
  published_timestamp BIGINT,
  image_url VARCHAR(500),
  source_url VARCHAR(500),
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  batch_id VARCHAR(50) NOT NULL
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_news_symbol ON trading.news(symbol);
CREATE INDEX IF NOT EXISTS idx_news_batch_id ON trading.news(batch_id);
CREATE INDEX IF NOT EXISTS idx_news_created_at ON trading.news(created_at);
CREATE INDEX IF NOT EXISTS idx_news_symbol_category ON trading.news(symbol, category);
