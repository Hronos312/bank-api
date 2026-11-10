CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,

    event_id UUID NOT NULL UNIQUE
        REFERENCES processed_events(event_id),

    transaction_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,

    source_account_id BIGINT,
    destination_account_id BIGINT,

    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMPTZ NOT NULL
        DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_status
    ON notifications (status, created_at);
