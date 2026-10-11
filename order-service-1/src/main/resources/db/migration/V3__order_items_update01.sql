ALTER TABLE order_items
    ADD sku_id VARCHAR(255) NULL;

ALTER TABLE order_items
    ADD sku_name VARCHAR(255) NULL;

UPDATE order_items
SET sku_id = product_id,
    sku_name = product_name
WHERE sku_id IS NULL
   OR sku_name IS NULL;

ALTER TABLE order_items
    MODIFY sku_id VARCHAR(255) NOT NULL;

ALTER TABLE order_items
    MODIFY sku_name VARCHAR(255) NOT NULL;

ALTER TABLE order_items
    DROP COLUMN product_id;

ALTER TABLE order_items
    DROP COLUMN product_name;