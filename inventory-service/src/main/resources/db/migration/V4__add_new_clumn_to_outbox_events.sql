ALTER TABLE outbox_events
    ADD COLUMN last_error TEXT NULL,
    ADD COLUMN failed_at DATETIME(6) NULL;