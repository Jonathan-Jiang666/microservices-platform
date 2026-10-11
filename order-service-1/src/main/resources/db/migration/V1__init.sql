create table orders
(
    id  bigint(20) not null primary key auto_increment,
    order_number varchar(64) not null,
    user_id bigint(20) not null,
    order_status varchar(30) not null,
    total_amount numeric(19,4) not null,
    currency varchar(3) not null,
    idempotency_key varchar(100) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    version bigint(20) not null default 0,
    unique key uk_order_number(order_number),
    unique key uk_idempotency_key(idempotency_key),
    constraint chk_orders_amount check (total_amount >= 0)
);


create table order_items
(
    id bigint not null primary key auto_increment,
    order_id bigint not null,
    product_id varchar(255) not null,
    product_name varchar(255) not null,
    unit_price numeric(19,4) not null,
    quantity int not null,
    subtotal numeric(19,4) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,
    version bigint(20) not null default 0,
    constraint fk_order_items_order_id foreign key (order_id) references orders(id) on delete cascade,
    constraint chk_order_items_quantity check (quantity > 0),
    constraint chk_order_items_subtotal check (subtotal >= 0)
);

create index idx_orders_user_id on orders(user_id);

create index idx_orders_status on orders(order_status);

create index idx_order_created_at on orders(created_at);

create index idx_order_updated_at on orders(updated_at);

create index idx_orders_user_created on orders(user_id, created_at DESC);


create table idempotency_keys(
    id bigint not null primary key auto_increment,
    idempotency_key varchar(100) not null,
    request_hash varchar(128),
    resource_type varchar(50),
    resource_id varchar(100),

    response_status integer,
    response_body text,

    status varchar(50) not null,

    created_at timestamp not null default current_timestamp,
    expires_at timestamp,

    constraint uk_idempotency_key unique (idempotency_key)

);

create table outbox_events(
    id  char(36) not null primary key,
    aggregate_id varchar(100) not null,
    aggregate_type varchar(100) not null,
    event_type varchar(100) not null,
    payload json not null,
    status varchar(50) not null default 'PENDING',
    retry_count int not null default 0,
    created_at timestamp not null default current_timestamp,
    published_at timestamp
);

create index idx_outbox_events_status_created_at on outbox_events(status, created_at);
create index idx_outbox_events_created_at on outbox_events(created_at);
create index idx_outbox_events_published_at on outbox_events(published_at);




