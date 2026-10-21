-- BR-12 (UK equities): Fauxnance serves US, Indian, FX and crypto markets but no London (LSE) market, so the
-- .L instruments added in V25 stay INACTIVE. Until a UK data source is available, clients can trade these
-- UK companies through their US listings, priced in USD during US market hours. Each was verified against
-- Fauxnance quotes; Unilever, Rio Tinto, Barclays and Lloyds US listings returned no quote and are omitted.
INSERT INTO trading.instruments (symbol, name, asset_class, status) VALUES
('SHEL', 'Shell (UK company, US listing)', 'EQUITY', 'TRADABLE'),
('AZN', 'AstraZeneca (UK company, US listing)', 'EQUITY', 'TRADABLE'),
('HSBC', 'HSBC Holdings (UK company, US listing)', 'EQUITY', 'TRADABLE'),
('BP', 'BP (UK company, US listing)', 'EQUITY', 'TRADABLE'),
('GSK', 'GSK (UK company, US listing)', 'EQUITY', 'TRADABLE'),
('VOD', 'Vodafone Group (UK company, US listing)', 'EQUITY', 'TRADABLE')
ON CONFLICT (symbol) DO NOTHING;
