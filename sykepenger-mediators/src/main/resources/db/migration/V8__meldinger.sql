DROP TABLE hendelse;

CREATE TABLE melding
(
    id          BIGSERIAL,
    data        JSONB                    NOT NULL,
    lest_dato   TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc'),
    PRIMARY KEY (id)
);
