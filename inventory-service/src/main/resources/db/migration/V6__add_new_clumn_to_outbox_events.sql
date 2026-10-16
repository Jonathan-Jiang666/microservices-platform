
UPDATE outbox_events
SET next_attempt_at = created_at
WHERE next_attempt_at IS NULL;