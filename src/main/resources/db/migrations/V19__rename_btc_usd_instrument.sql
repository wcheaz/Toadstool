UPDATE trading.instruments
SET symbol = REPLACE(symbol, '/', '-')
WHERE symbol IN ('BTC/USD', 'ETH/USD', 'XRP/USD', 'EUR/USD', 'GBP/USD', 'JPY/USD');
