CREATE TABLE utbetalingsreferanse
(
    id             BIGSERIAL,
    aktor_id       VARCHAR(32)              NOT NULL,
    orgnr          VARCHAR(32)              NOT NULL,
    sakskompleksId VARCHAR(36)              NOT NULL,
    PRIMARY KEY (id)
);
