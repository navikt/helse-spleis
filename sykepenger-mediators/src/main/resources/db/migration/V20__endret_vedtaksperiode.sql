CREATE TABLE endret_vedtaksperiode
(
    id                BIGSERIAL,
    fodselsnummer     VARCHAR,
    vedtaksperiode_id UUID,
    gammel_tilstand   VARCHAR,
    ny_tilstand       VARCHAR,
    opprettet         TIMESTAMP NOT NULL DEFAULT now()
);