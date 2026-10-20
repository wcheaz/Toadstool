UPDATE trading.instruments
SET symbol = CASE symbol
    WHEN 'EUR-USD' THEN 'EURUSD=X'
    WHEN 'GBP-USD' THEN 'GBPUSD=X'
    WHEN 'JPY-USD' THEN 'JPY=X'
    ELSE symbol
END
WHERE symbol IN ('EUR-USD', 'GBP-USD', 'JPY-USD');
