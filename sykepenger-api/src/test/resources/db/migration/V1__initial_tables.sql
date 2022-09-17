CREATE TABLE unike_person
(
    fnr          BIGINT PRIMARY KEY,
    aktor_id     BIGINT    NOT NULL
);
create index "index_aktor_id" on unike_person(aktor_id);

CREATE TABLE person
(
    id             BIGSERIAL,
    skjema_versjon INT                      NOT NULL,
    fnr            BIGINT                   NOT NULL,
    aktor_id       BIGINT                   NOT NULL,
    data           JSON                     NOT NULL,
    opprettet      TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    PRIMARY KEY (id)
);

create index "index_person_fnr" on person(fnr, id desc);

CREATE TABLE melding
(
    id           BIGSERIAL,
    fnr          BIGINT                   NOT NULL,
    melding_id   VARCHAR(40)              NOT NULL,
    melding_type VARCHAR(40) UNIQUE       NOT NULL,
    data         JSON                     NOT NULL,
    lest_dato    TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    PRIMARY KEY (id)
);

CREATE INDEX "index_melding_fnr" ON melding(fnr);