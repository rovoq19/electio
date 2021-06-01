create table user_subscriptions (
    subscriber_id int8 not null references usr,
    channel_id int8 not null references meeting,
    primary key (subscriber_id, channel_id)
);

create table group_subscriptions (
    subscriber_id int8 not null references usr,
    channel_id int8 not null references user_group,
    primary key (subscriber_id, channel_id)
);
