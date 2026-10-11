create table inventory
(
     id bigint(20) not null primary key auto_increment,
    sku_id varchar(64) not null,
    on_hand_quantity bigint not null default 0,
    reserved_quantity bigint not null default 0,
    version bigint not null default 0,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    unique key uk_sku_id(sku_id),
    constraint chk_available_quantity check (on_hand_quantity >= 0),
    constraint chk_reserved_quantity check (reserved_quantity >= 0),
    constraint chk_inventory_reserved_not_exceed_available check (reserved_quantity <= on_hand_quantity)
);


create table inventory_reservations(
    id bigint not null primary key auto_increment,
    reservation_no varchar(64) not null,
    inventory_id bigint not null,
    order_no varchar(64) not null,
    sku_id varchar(64) not null,
    quantity integer not null,
    status varchar(30)not null , # RESERVED, CONFIRMED , RELEASED, EXPIRED
    expires_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    unique key uk_reservation_no(reservation_no),
    unique key uk_order_sku(order_no, sku_id),
    constraint chk_inventory_reservation_quantity check ( quantity > 0 ),
    foreign key (inventory_id) references inventory(id)

);

create table inventory_transactions
(
    id bigint(20) not null primary key auto_increment,
    transaction_no varchar(64) not null,
    inventory_id bigint not null,
    reserved_id bigint ,
    sku_id varchar(64) not null,
    order_no varchar(64) ,
    reservation_no varchar(64)  ,
    transaction_type varchar(30) not null, #STOCK_IN, RESERVE, RELEASE, SALE, RETURN, ADJUSTMENT
    quantity integer not null,
    quantity_before integer not null,
    quantity_after integer not null,
    reference_id varchar(100) ,
    created_at timestamp not null default current_timestamp,
    unique key uk_transaction_no(transaction_no),
    constraint chk_inventory_transaction_quantity check ( quantity > 0 ),
    foreign key (inventory_id) references inventory(id),
    foreign key (reserved_id) references inventory_reservations(id)

);

CREATE INDEX idx_inventory_reservation_order
    ON inventory_reservations(order_no);

CREATE INDEX idx_inventory_reservation_product
    ON inventory_reservations(sku_id);

CREATE INDEX idx_inventory_reservation_status_expiry
    ON inventory_reservations(status, expires_at);

CREATE INDEX idx_inventory_tx_product_created
    ON inventory_transactions(sku_id, created_at DESC);

CREATE INDEX idx_inventory_tx_order
    ON inventory_transactions(order_no);