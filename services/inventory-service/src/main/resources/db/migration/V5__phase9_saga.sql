CREATE TABLE processed_saga_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE saga_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    message_key VARCHAR(200) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_error VARCHAR(2000)
);

CREATE INDEX idx_saga_outbox_ready ON saga_outbox(status, available_at, created_at);
