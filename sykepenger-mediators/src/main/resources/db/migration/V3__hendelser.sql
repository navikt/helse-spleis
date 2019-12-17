CREATE TABLE hendelse
(
    id        BIGSERIAL,
    aktor_id  VARCHAR(40)              NOT NULL,
    type      VARCHAR(40)              NOT NULL,
    data      JSONB                    NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    mottatt   TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    PRIMARY KEY (id)
);

create index "index_hendelse_aktor_id" on hendelse using btree (aktor_id);
create index "index_hendelsetype" on hendelse using btree (type);
