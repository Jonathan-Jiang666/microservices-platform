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