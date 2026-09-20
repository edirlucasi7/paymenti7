ALTER TABLE payments ADD COLUMN payment_method_token VARCHAR(512);
UPDATE payments SET payment_method_token = 'legacy_unavailable' WHERE payment_method_token IS NULL;
ALTER TABLE payments ALTER COLUMN payment_method_token SET NOT NULL;

ALTER TABLE outbox_events ADD COLUMN schema_version INTEGER NOT NULL DEFAULT 1;

CREATE TABLE authorization_result_inbox (
    event_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('APPROVED', 'DECLINED', 'FAILED')),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_authorization_result_inbox_payment
    ON authorization_result_inbox (payment_id);
