create sequence hibernate_sequence start 1 increment 1;

create table user_role (
    user_id int8 not null,
    roles varchar(255)
);

create table usr (
    id int8 not null,
    activation varchar(255),
    active boolean not null,
    email varchar(255),
    password varchar(255) not null,
    username varchar(255) not null,
    primary key (id)
);

create table meeting (
    id int8 not null,
    name varchar(255),
    description varchar(255),
    creator varchar(255) not null,
    creation_date varchar(255) not null,
    locked boolean not null,
    primary key (id)
);

create table voting (
    id int8 not null,
    name varchar(255),
    description varchar(255),
    primary key (id)
);

create table answer (
    id int8 not null,
    name varchar(255),
    primary key (id)
);

create table vote (
    id int8 not null,
    username varchar(255) not null,
    primary key (id)
);

alter table if exists user_role
    add constraint user_role_user_fk
    foreign key (user_id) references usr;