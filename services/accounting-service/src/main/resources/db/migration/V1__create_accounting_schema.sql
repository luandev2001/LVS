CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    currency CHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE journal_entry (
    id UUID PRIMARY KEY,
    source_event_id UUID NOT NULL UNIQUE,
    payment_id UUID NOT NULL,
    entry_type VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    currency CHAR(3) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE journal_line (
    id BIGSERIAL PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES journal_entry(id),
    account_id BIGINT NOT NULL REFERENCES account(id),
    side VARCHAR(10) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    CONSTRAINT ck_journal_line_side CHECK (side IN ('DEBIT','CREDIT')),
    CONSTRAINT ck_journal_line_amount CHECK (amount > 0)
);

CREATE INDEX idx_journal_entry_payment_id ON journal_entry(payment_id);
CREATE INDEX idx_journal_entry_occurred_at ON journal_entry(occurred_at);
CREATE INDEX idx_journal_line_account_id ON journal_line(account_id);

INSERT INTO account(code, name, currency) VALUES
 ('CASH','Cash / Settlement','USD'),
 ('PAYMENT_REVENUE','Payment Revenue','USD'),
 ('REFUND_EXPENSE','Refund Expense','USD')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE processed_payment_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
