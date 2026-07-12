-- Tracks when a record was claimed for processing, enabling recovery of
-- records stuck in PROCESSING after a crash between claim and outcome.
ALTER TABLE payout_outbox
    ADD COLUMN claimed_at TIMESTAMPTZ;

CREATE INDEX idx_payout_outbox_claimed_at ON payout_outbox (status, claimed_at);