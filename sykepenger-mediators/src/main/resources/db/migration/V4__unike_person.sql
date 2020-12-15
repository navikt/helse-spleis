CREATE TABLE unike_person
(
    fnr          BIGINT PRIMARY KEY,
    aktor_id     BIGINT    NOT NULL,
    opprettet    TIMESTAMP NOT NULL DEFAULT now(),
    sist_avstemt TIMESTAMP
);
