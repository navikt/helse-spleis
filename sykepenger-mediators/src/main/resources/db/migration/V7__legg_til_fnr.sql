ALTER TABLE person
    ADD COLUMN fnr VARCHAR(32);

ALTER TABLE hendelse
    ADD COLUMN fnr VARCHAR(32);

create index "index_person_fnr" on person using btree (fnr);
create index "index_hendelse_fnr" on hendelse using btree (fnr);
