ALTER TABLE delivery_events
ADD COLUMN distance_km NUMERIC(10, 2) DEFAULT 0.00;
ALTER TABLE driver_rate_cards
    RENAME COLUMN per_delivery_rate TO base_rate;
ALTER TABLE driver_rate_cards
ADD COLUMN per_km_rate NUMERIC(10, 2) DEFAULT 1.50;
-- test driver
UPDATE driver_rate_cards
SET base_rate = 3.00,
    per_km_rate = 1.20
WHERE driver_id = 'TEST-DRIVER-999';