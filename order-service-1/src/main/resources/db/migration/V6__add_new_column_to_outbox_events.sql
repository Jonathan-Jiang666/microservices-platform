ALTER TABLE outbox_events
    ADD COLUMN next_attempt_at DATETIME(6) NULL;

CREATE INDEX idx_outbox_events_retry
    ON outbox_events(status, next_attempt_at, created_at);

UPDATE outbox_events
SET next_attempt_at = created_at
WHERE next_attempt_at IS NULL;