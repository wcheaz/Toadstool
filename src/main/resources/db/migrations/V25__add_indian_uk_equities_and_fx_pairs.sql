-- BR-12: equities in UK/US and Indian markets, foreign exchange and crypto.
-- Prices for non-USD instruments are converted to USD (the account currency) by FauxnanceService.

-- Indian equities (NSE, priced in INR): NIFTY 50 constituents verified against Fauxnance quotes
INSERT INTO trading.instruments (symbol, name, asset_class, status) VALUES
('RELIANCE.NS', 'Reliance Industries (NSE)', 'EQUITY', 'TRADABLE'),
('TCS.NS', 'Tata Consultancy Services (NSE)', 'EQUITY', 'TRADABLE'),
('HDFCBANK.NS', 'HDFC Bank (NSE)', 'EQUITY', 'TRADABLE'),
('ICICIBANK.NS', 'ICICI Bank (NSE)', 'EQUITY', 'TRADABLE'),
('INFY.NS', 'Infosys (NSE)', 'EQUITY', 'TRADABLE'),
('BHARTIARTL.NS', 'Bharti Airtel (NSE)', 'EQUITY', 'TRADABLE'),
('SBIN.NS', 'State Bank of India (NSE)', 'EQUITY', 'TRADABLE'),
('ITC.NS', 'ITC (NSE)', 'EQUITY', 'TRADABLE'),
('HINDUNILVR.NS', 'Hindustan Unilever (NSE)', 'EQUITY', 'TRADABLE'),
('LT.NS', 'Larsen & Toubro (NSE)', 'EQUITY', 'TRADABLE'),
('BAJFINANCE.NS', 'Bajaj Finance (NSE)', 'EQUITY', 'TRADABLE'),
('KOTAKBANK.NS', 'Kotak Mahindra Bank (NSE)', 'EQUITY', 'TRADABLE'),
('AXISBANK.NS', 'Axis Bank (NSE)', 'EQUITY', 'TRADABLE'),
('MARUTI.NS', 'Maruti Suzuki India (NSE)', 'EQUITY', 'TRADABLE'),
('SUNPHARMA.NS', 'Sun Pharmaceutical Industries (NSE)', 'EQUITY', 'TRADABLE'),
('ASIANPAINT.NS', 'Asian Paints (NSE)', 'EQUITY', 'TRADABLE'),
('WIPRO.NS', 'Wipro (NSE)', 'EQUITY', 'TRADABLE'),
('HCLTECH.NS', 'HCLTech (NSE)', 'EQUITY', 'TRADABLE'),
('TITAN.NS', 'Titan Company (NSE)', 'EQUITY', 'TRADABLE'),
('ULTRACEMCO.NS', 'UltraTech Cement (NSE)', 'EQUITY', 'TRADABLE')
ON CONFLICT (symbol) DO NOTHING;

-- UK equities (LSE, priced in pence): Fauxnance symbol discovery for .L symbols was unavailable when
-- this was written, so they start INACTIVE. An admin can make them TRADABLE once quotes are returned
-- (PATCH /api/instruments/{id}/status); until then no order can be placed without a price.
INSERT INTO trading.instruments (symbol, name, asset_class, status) VALUES
('SHEL.L', 'Shell (LSE)', 'EQUITY', 'INACTIVE'),
('AZN.L', 'AstraZeneca (LSE)', 'EQUITY', 'INACTIVE'),
('HSBA.L', 'HSBC Holdings (LSE)', 'EQUITY', 'INACTIVE'),
('ULVR.L', 'Unilever (LSE)', 'EQUITY', 'INACTIVE'),
('BP.L', 'BP (LSE)', 'EQUITY', 'INACTIVE'),
('GSK.L', 'GSK (LSE)', 'EQUITY', 'INACTIVE'),
('RIO.L', 'Rio Tinto (LSE)', 'EQUITY', 'INACTIVE'),
('BARC.L', 'Barclays (LSE)', 'EQUITY', 'INACTIVE'),
('LLOY.L', 'Lloyds Banking Group (LSE)', 'EQUITY', 'INACTIVE'),
('VOD.L', 'Vodafone Group (LSE)', 'EQUITY', 'INACTIVE')
ON CONFLICT (symbol) DO NOTHING;

-- FX: revive GBP/USD under the canonical Fauxnance symbol (as V22 did for EUR/USD)
UPDATE trading.instruments
SET symbol = 'FX:GBPUSD',
    name = 'British Pound / US Dollar',
    status = 'TRADABLE'
WHERE symbol IN ('GBPUSD=X', 'GBP-USD', 'GBP/USD')
  AND NOT EXISTS (SELECT 1 FROM trading.instruments WHERE symbol = 'FX:GBPUSD');

-- FX pairs with a non-USD base, so their USD value moves (USD-base pairs are always worth $1 per unit)
INSERT INTO trading.instruments (symbol, name, asset_class, status) VALUES
('FX:GBPUSD', 'British Pound / US Dollar', 'FX', 'TRADABLE'),
('FX:AUDUSD', 'Australian Dollar / US Dollar', 'FX', 'TRADABLE'),
('FX:NZDUSD', 'New Zealand Dollar / US Dollar', 'FX', 'TRADABLE'),
('FX:EURGBP', 'Euro / British Pound', 'FX', 'TRADABLE'),
('FX:EURJPY', 'Euro / Japanese Yen', 'FX', 'TRADABLE'),
('FX:GBPJPY', 'British Pound / Japanese Yen', 'FX', 'TRADABLE')
ON CONFLICT (symbol) DO NOTHING;
