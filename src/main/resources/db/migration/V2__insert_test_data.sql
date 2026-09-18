-- Test data insertion script
-- Populates the database with realistic test data for development and testing
-- Includes: 20+ clients, trading accounts, instruments, orders, and fills

BEGIN;

-- Insert test instruments first (referenced by orders)
INSERT INTO trading.instruments (symbol, name, asset_class, status) VALUES
('AAPL', 'Apple Inc.', 'EQUITY', 'TRADABLE'),
('GOOGL', 'Alphabet Inc.', 'EQUITY', 'TRADABLE'),
('MSFT', 'Microsoft Corporation', 'EQUITY', 'TRADABLE'),
('AMZN', 'Amazon.com Inc.', 'EQUITY', 'TRADABLE'),
('TSLA', 'Tesla Inc.', 'EQUITY', 'TRADABLE'),
('META', 'Meta Platforms Inc.', 'EQUITY', 'TRADABLE'),
('NVDA', 'NVIDIA Corporation', 'EQUITY', 'TRADABLE'),
('JPM', 'JPMorgan Chase & Co.', 'EQUITY', 'TRADABLE'),
('GLD', 'SPDR Gold Shares', 'EQUITY', 'TRADABLE'),
('SPY', 'SPDR S&P 500 ETF Trust', 'EQUITY', 'TRADABLE'),
('BTC/USD', 'Bitcoin to US Dollar', 'CRYPTO', 'TRADABLE'),
('ETH/USD', 'Ethereum to US Dollar', 'CRYPTO', 'TRADABLE'),
('XRP/USD', 'XRP to US Dollar', 'CRYPTO', 'TRADABLE'),
('EUR/USD', 'Euro to US Dollar', 'FX', 'TRADABLE'),
('GBP/USD', 'British Pound to US Dollar', 'FX', 'TRADABLE'),
('JPY/USD', 'Japanese Yen to US Dollar', 'FX', 'TRADABLE');

-- Create 25 test clients with accounts
WITH client_data AS (
  INSERT INTO trading.clients (email, display_name, status) VALUES
  ('alice.johnson@example.com', 'Alice Johnson', 'ACTIVE'),
  ('bob.smith@example.com', 'Bob Smith', 'ACTIVE'),
  ('carol.white@example.com', 'Carol White', 'ACTIVE'),
  ('david.brown@example.com', 'David Brown', 'ACTIVE'),
  ('emma.davis@example.com', 'Emma Davis', 'ACTIVE'),
  ('frank.miller@example.com', 'Frank Miller', 'ACTIVE'),
  ('grace.wilson@example.com', 'Grace Wilson', 'ACTIVE'),
  ('henry.moore@example.com', 'Henry Moore', 'ACTIVE'),
  ('isabelle.taylor@example.com', 'Isabelle Taylor', 'ACTIVE'),
  ('jack.anderson@example.com', 'Jack Anderson', 'ACTIVE'),
  ('kate.thomas@example.com', 'Kate Thomas', 'ACTIVE'),
  ('leo.jackson@example.com', 'Leo Jackson', 'ACTIVE'),
  ('mia.martin@example.com', 'Mia Martin', 'ACTIVE'),
  ('noah.perez@example.com', 'Noah Perez', 'ACTIVE'),
  ('olivia.garcia@example.com', 'Olivia Garcia', 'ACTIVE'),
  ('paul.robinson@example.com', 'Paul Robinson', 'ACTIVE'),
  ('quinn.clark@example.com', 'Quinn Clark', 'ACTIVE'),
  ('rachel.rodriguez@example.com', 'Rachel Rodriguez', 'ACTIVE'),
  ('sam.lewis@example.com', 'Sam Lewis', 'ACTIVE'),
  ('tina.lee@example.com', 'Tina Lee', 'ACTIVE'),
  ('ursula.walker@example.com', 'Ursula Walker', 'ACTIVE'),
  ('victor.hall@example.com', 'Victor Hall', 'ACTIVE'),
  ('wendy.allen@example.com', 'Wendy Allen', 'ACTIVE'),
  ('xander.young@example.com', 'Xander Young', 'ACTIVE'),
  ('yara.king@example.com', 'Yara King', 'ACTIVE')
  RETURNING client_id
)
INSERT INTO trading.accounts (client_id, status)
SELECT client_id, 'ACTIVE' FROM client_data;

COMMIT;
