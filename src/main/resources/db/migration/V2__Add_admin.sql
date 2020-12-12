insert into usr (id, username, password, active)
    values (0, 'admin', 'admin', true);

insert into user_role (user_id, roles)
    values (0, 'USER'), (0, 'ADMIN');