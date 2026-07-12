ALTER TABLE delivery_events DROP CONSTRAINT IF EXISTS chk_status;

ALTER TABLE delivery_events ADD CONSTRAINT chk_status 
CHECK (status IN ('EN_ROUTE', 'DELIVERED', 'CANCELLED', 'FAILED'));