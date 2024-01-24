alter table orders
add column created_by varchar(255);
alter table orders
add column last_modified_by varchar(255);