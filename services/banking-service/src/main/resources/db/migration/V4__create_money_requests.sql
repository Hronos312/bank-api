CREATE TABLE money_requests (
    user_id BIGINT NOT NULL,
    request_key VARCHAR(128) NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    response JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, request_key)
);
