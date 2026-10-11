CREATE TABLE processed_events (
    event_id CHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_processed_events
        PRIMARY KEY (event_id),

    INDEX idx_processed_events_aggregate_id (aggregate_id),

    INDEX idx_processed_events_processed_at (processed_at)
);