ALTER TABLE outbox_events
    ADD COLUMN next_attempt_at DATETIME NULL;
   
CREATE INDEX idx_outbox_retry
    ON outbox_events(status, next_attempt_at, created_at);