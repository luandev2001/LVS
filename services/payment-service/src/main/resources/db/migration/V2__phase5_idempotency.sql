CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE payments ADD COLUMN request_fingerprint VARCHAR(64);

UPDATE payments
SET request_fingerprint = encode(
    digest(order_reference || ':' || amount::text || ':' || upper(currency), 'sha256'),
    'hex'
);

ALTER TABLE payments ALTER COLUMN request_fingerprint SET NOT NULL;

CREATE INDEX idx_payment_idempotency_key ON payments(idempotency_key);
