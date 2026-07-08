CREATE TABLE IF NOT EXISTS driver_payouts (
    id UUID PRIMARY KEY,
    driver_id VARCHAR(255) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    deliveries_processed INTEGER NOT NULL,
    payout_calculated_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
);
CREATE INDEX idx_payout_driver ON driver_payouts (driver_id);
CREATE INDEX idx_payout_status ON driver_payouts (status);