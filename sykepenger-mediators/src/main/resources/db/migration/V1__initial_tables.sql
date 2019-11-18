CREATE TABLE person
(
    id        BIGSERIAL,
    aktor_id  VARCHAR(32)              NOT NULL,
    data      JSONB                    NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    PRIMARY KEY (id)
);

create index "index_aktor_id" on person using btree (aktor_id);
