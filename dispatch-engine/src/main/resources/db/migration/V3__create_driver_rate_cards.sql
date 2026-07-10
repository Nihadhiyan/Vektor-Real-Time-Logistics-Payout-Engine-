CREATE TABLE IF NOT EXISTS driver_rate_cards (
    driver_id VARCHAR(255) PRIMARY KEY,
    per_delivery_rate NUMERIC(10, 2) NOT NULL
);
INSERT INTO driver_rate_cards(driver_id, per_delivery_rate)
VALUES ('TEST-DRIVER-999', 7.50);
INSERT INTO driver_rate_cards (driver_id, per_delivery_rate)
VALUES ('PREMIUM-DRIVER-001', 12.00);