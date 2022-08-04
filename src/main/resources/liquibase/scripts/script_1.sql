-- liquibase formatted sql

-- changeSet nakremneva:1

create table notification_task
(
    id      bigserial not null,
    chat_id bigserial not null,
    message varchar(255),
    date    timestamp not null,
    primary key (id)
);