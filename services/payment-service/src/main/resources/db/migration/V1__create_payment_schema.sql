CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_reference VARCHAR(100) NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    idempotency_key VARCHAR(150) NOT NULL,
    gateway_reference VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    attempt INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    callback_received_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uk_payment_gateway_reference UNIQUE (gateway_reference)
);
CREATE INDEX idx_payment_status_updated_at ON payments(status, updated_at);
