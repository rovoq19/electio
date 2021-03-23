create table user_subscriptions (
    subscriber_id int8 not null references usr,
    channel_id int8 not null references meeting,
    primary key (subscriber_id, channel_id)
);

create table voting_subscriptions (
    subscriber_id int8 not null references voting,
    channel_id int8 not null references meeting,
    primary key (subscriber_id, channel_id)
);

create table answer_subscriptions (
    subscriber_id int8 not null references answer,
    channel_id int8 not null references voting,
    primary key (subscriber_id, channel_id)
);

create table vote_subscriptions (
    subscriber_id int8 not null references vote,
    channel_id int8 not null references answer,
    primary key (subscriber_id, channel_id)
);