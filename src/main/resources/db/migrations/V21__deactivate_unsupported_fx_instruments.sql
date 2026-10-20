UPDATE trading.instruments
SET status = 'INACTIVE'
WHERE symbol IN (
    'GBPUSD=X',
    'JPY=X',
    'GBP-USD',
    'JPY-USD',
    'GBP/USD',
    'JPY/USD'
);
