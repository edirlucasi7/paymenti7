CREATE TABLE payments (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PROCESSING', 'APPROVED', 'DECLINED', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE idempotency_requests (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    operation VARCHAR(50) NOT NULL,
    idempotency_key UUID NOT NULL,
    request_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PROCESSING', 'APPROVED', 'DECLINED', 'FAILED')),
    payment_id UUID NOT NULL,
    response_http_status INTEGER,
    response_body JSONB,
    response_headers JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_idempotency_request UNIQUE (merchant_id, operation, idempotency_key),
    CONSTRAINT uq_idempotency_payment UNIQUE (payment_id),
    CONSTRAINT fk_idempotency_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_idempotency_lifecycle CHECK (
        (status = 'PROCESSING' AND completed_at IS NULL AND expires_at IS NULL)
        OR
        (status <> 'PROCESSING' AND response_http_status IS NOT NULL
            AND response_body IS NOT NULL AND response_headers IS NOT NULL
            AND completed_at IS NOT NULL AND expires_at IS NOT NULL)
    )
);

CREATE INDEX idx_idempotency_terminal_expiration
    ON idempotency_requests (expires_at)
    WHERE status <> 'PROCESSING';

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL REFERENCES payments (id),
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    delivery_status VARCHAR(16) NOT NULL CHECK (delivery_status IN ('PENDING', 'PUBLISHED'))
);

CREATE INDEX idx_payment_outbox_pending
    ON outbox_events (occurred_at, id)
    WHERE delivery_status = 'PENDING';
