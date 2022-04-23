CREATE TABLE slettet_vedtaksperiode
(
    id                BIGSERIAL,
    fodselsnummer     VARCHAR,
    vedtaksperiode_id UUID,
    data              JSONB,
    opprettet         TIMESTAMP NOT NULL DEFAULT now()
);