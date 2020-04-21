ALTER TABLE person ADD COLUMN melding_id UUID NULL DEFAULT NULL;

create index "index_person_melding_id" on person using btree (melding_id);
