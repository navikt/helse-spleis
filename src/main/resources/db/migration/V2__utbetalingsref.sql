CREATE TABLE utbetalingsreferanse
(
    id              VARCHAR(30),
    aktor_id        VARCHAR(32)              NOT NULL,
    orgnr           VARCHAR(32)              NOT NULL,
    sakskompleks_id VARCHAR(36)              NOT NULL,
    PRIMARY KEY (id)
);
