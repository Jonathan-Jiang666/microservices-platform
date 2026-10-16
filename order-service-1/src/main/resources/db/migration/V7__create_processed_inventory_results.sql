CREATE TABLE processed_inventory_results (
                                             event_id CHAR(36) NOT NULL PRIMARY KEY,
                                             event_type VARCHAR(100) NOT NULL,
                                             order_number VARCHAR(100) NOT NULL,
                                             payload_hash CHAR(64) NOT NULL,
                                             processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);