ALTER TABLE orders
    ADD note VARCHAR(255) NULL,
    ADD billing_address VARCHAR(255) NULL,
    ADD shipping_address VARCHAR(255) NULL,
    ADD recipient_email VARCHAR(100) NULL,
    ADD recipient_name VARCHAR(100) NULL,
    ADD recipient_phone VARCHAR(100) NULL;

UPDATE orders
SET
    billing_address = 'UNKNOWN',
    shipping_address = 'UNKNOWN',
    recipient_email = 'unknown@example.com',
    recipient_name = 'UNKNOWN',
    recipient_phone = 'UNKNOWN'
WHERE billing_address IS NULL
   OR shipping_address IS NULL
   OR recipient_email IS NULL
   OR recipient_name IS NULL
   OR recipient_phone IS NULL;

ALTER TABLE orders
    MODIFY billing_address VARCHAR(255) NOT NULL,
    MODIFY shipping_address VARCHAR(255) NOT NULL,
    MODIFY recipient_email VARCHAR(100) NOT NULL,
    MODIFY recipient_name VARCHAR(100) NOT NULL,
    MODIFY recipient_phone VARCHAR(100) NOT NULL;


