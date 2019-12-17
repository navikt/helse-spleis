ALTER TABLE hendelse
    ALTER COLUMN id TYPE VARCHAR(40);

create index "index_hendelse_id" on hendelse using btree (id);
