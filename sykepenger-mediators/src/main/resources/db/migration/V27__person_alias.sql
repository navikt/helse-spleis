create table if not exists person_alias(
    fnr bigint primary key,
    person_id bigint not null references person(id)
);
create index if not exists person_alias_fk on person_alias(person_id);

insert into person_alias (fnr, person_id)
select fnr, id from person;