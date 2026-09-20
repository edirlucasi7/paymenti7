CREATE TABLE inbox_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE authorization_requests (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE,
    merchant_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL,
    payment_method_token VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_ROUTING', 'CALL_IN_PROGRESS', 'PENDING_RECONCILIATION', 'COMPLETED')),
    next_route_index INTEGER NOT NULL DEFAULT 0 CHECK (next_route_index >= 0),
    terminal_status VARCHAR(16) CHECK (terminal_status IN ('APPROVED', 'DECLINED', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_authorization_requests_pending
    ON authorization_requests (created_at, id)
    WHERE status = 'PENDING_ROUTING';

CREATE TABLE authorization_attempts (
    id UUID PRIMARY KEY,
    authorization_id UUID NOT NULL REFERENCES authorization_requests (id),
    payment_id UUID NOT NULL,
    acquirer VARCHAR(64) NOT NULL,
    idempotency_reference VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DISPATCHING', 'APPROVED', 'DECLINED', 'SAFE_TO_FALLBACK', 'UNKNOWN')),
    provider_reference VARCHAR(256),
    reason_code VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_authorization_attempts_dispatching
    ON authorization_attempts (created_at)
    WHERE status = 'DISPATCHING';

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    schema_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    delivery_status VARCHAR(16) NOT NULL CHECK (delivery_status IN ('PENDING', 'PUBLISHED'))
);

CREATE INDEX idx_authorization_outbox_pending
    ON outbox_events (occurred_at, id)
    WHERE delivery_status = 'PENDING';
