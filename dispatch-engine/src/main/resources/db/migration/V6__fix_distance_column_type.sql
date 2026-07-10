-- Fix Hibernate schema validation mismatch for DeliveryEvent.java (Double)
ALTER TABLE delivery_events
ALTER COLUMN distance_km TYPE DOUBLE PRECISION;