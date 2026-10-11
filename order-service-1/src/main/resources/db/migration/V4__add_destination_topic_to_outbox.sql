alter table outbox_events
    add column destination_topic varchar(100) null;

update outbox_events
set destination_topic = 'order.events'
where destination_topic is null;


alter table outbox_events
    modify destination_topic varchar(100) not null;