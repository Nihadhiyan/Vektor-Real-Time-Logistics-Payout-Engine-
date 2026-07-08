CREATE TABLE IF NOT EXISTS delivery_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    driver_id VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_delivery_events_event_id UNIQUE (event_id),
    CONSTRAINT chk_status CHECK (status IN ('EN_ROUTE', 'DELIVERED'))
);
CREATE INDEX idx_unprocessed ON delivery_events (processed, status);
CREATE INDEX idx_driver ON delivery_events (driver_id);
CREATE INDEX idx_occurred_at ON delivery_events (occurred_at);