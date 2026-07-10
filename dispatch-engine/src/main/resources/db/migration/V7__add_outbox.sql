CREATE TABLE payout_outbox (
    outbox_id UUID PRIMARY KEY,
    driver_id VARCHAR(255) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    bank_reference_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_payout_outbox_status ON payout_outbox(status);