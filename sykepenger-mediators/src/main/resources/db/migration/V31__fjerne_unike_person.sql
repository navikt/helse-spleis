alter table person add column aktor_id bigint;
alter table person add column opprettet timestamp default now();
alter table person add column sist_avstemt TIMESTAMP;

update person
    set aktor_id = u.aktor_id,
        opprettet = u.opprettet,
        sist_avstemt = u.sist_avstemt
from unike_person u
    where person.fnr = u.fnr;

alter table person alter column aktor_id set not null;
alter table person alter column opprettet set not null;

alter table person drop constraint person_fnr_fk;

drop table unike_person;