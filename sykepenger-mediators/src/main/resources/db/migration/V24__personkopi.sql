CREATE TABLE person_kopi AS TABLE person;

drop index if exists index_person_vedtak_partial;
alter table person drop column vedtak, drop column melding_id, drop column rullet_tilbake;