ALTER TABLE payments ADD COLUMN saga_id UUID;
ALTER TABLE payments ADD COLUMN inventory_id BIGINT;
ALTER TABLE payments ADD COLUMN inventory_quantity BIGINT;
ALTER TABLE payments ADD COLUMN saga_failure_mode VARCHAR(50);
ALTER TABLE payments ADD COLUMN saga_status VARCHAR(40);
ALTER TABLE payments ADD COLUMN saga_updated_at TIMESTAMPTZ;
CREATE INDEX idx_payments_saga_status ON payments(saga_status, saga_updated_at);

CREATE TABLE processed_saga_events (event_id UUID PRIMARY KEY, event_type VARCHAR(100) NOT NULL, processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
