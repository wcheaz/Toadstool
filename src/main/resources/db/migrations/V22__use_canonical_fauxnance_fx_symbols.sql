UPDATE trading.instruments
SET symbol = 'FX:EURUSD',
    name = 'Euro / US Dollar'
WHERE symbol IN ('EURUSD=X', 'EUR-USD', 'EUR/USD');
