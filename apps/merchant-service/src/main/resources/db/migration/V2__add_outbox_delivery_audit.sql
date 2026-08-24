ALTER TABLE outbox_events
    ADD COLUMN delivery_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN superseded_by_event_id UUID REFERENCES outbox_events (id),
    ADD CONSTRAINT chk_outbox_events_delivery_status
        CHECK (delivery_status IN ('PENDING', 'PUBLISHED', 'SUPPRESSED'));

UPDATE outbox_events
SET delivery_status = 'PUBLISHED'
WHERE processed_at IS NOT NULL;

CREATE INDEX idx_outbox_events_pending_aggregate_latest
    ON outbox_events (aggregate_id, occurred_at DESC, id DESC)
    WHERE delivery_status = 'PENDING' AND processed_at IS NULL;
