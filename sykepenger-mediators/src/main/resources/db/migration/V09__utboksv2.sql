DROP TABLE IF EXISTS utboks;

CREATE TABLE utboks (
    id uuid PRIMARY KEY,                                -- Dette er @id'en på meldingen
    lopenummer bigint GENERATED ALWAYS AS IDENTITY,     -- For å kunne sortere meldingene basert på når de er lagt inn
    forarsaket_av uuid NOT NULL,                        -- Dette er hendelseId'en til hendelsen som forårsaket at denne meldingen ble sendt til utboks.
    key text,
    json jsonb NOT NULL,
    mottaker text NOT NULL,                             -- Dette sier noe om hvilken topic meldingen skal sendes til. Rapid/Subsumsjon
    opprettet timestamptz NOT NULL                      -- @opprettetUTC fra meldingen
);

CREATE INDEX idx_utboks_key ON utboks (key) WHERE key IS NOT NULL;
CREATE INDEX idx_utboks_null_key_lopenummer ON utboks (lopenummer) WHERE key IS NULL;

-- Samme skjema som utboks, bare med et sendt-tidsstempel.
CREATE TABLE sendt (
    id uuid PRIMARY KEY,
    lopenummer bigint NOT NULL UNIQUE,
    forarsaket_av uuid NOT NULL,
    key text,
    json jsonb NOT NULL,
    mottaker text NOT NULL,
    opprettet timestamptz NOT NULL,
    sendt timestamptz NOT NULL                          -- Tidspunktet meldingen ble sendt
);

-- For å effektivt kunne finne alle meldinger som er sendt ut på grunn av en gitt hendelse
CREATE INDEX idx_sendt_forarsaket_av ON sendt USING hash (forarsaket_av);
-- For å effektivt kunne finne alle meldinger som er sendt ut for en gitt person
CREATE INDEX idx_sendt_key ON sendt (key) WHERE key IS NOT NULL;
