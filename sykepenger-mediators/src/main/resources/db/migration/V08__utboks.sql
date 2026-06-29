CREATE TABLE utboks (
    id uuid PRIMARY KEY,                                -- Dette er @id'en på meldingen
    lopenummer bigint GENERATED ALWAYS AS IDENTITY,
    forarsaket_av uuid NOT NULL,                        -- Dette er hendelseId'en til hendelsen som forårsaket at denne meldingen ble sendt til utboks.
    key text,
    json jsonb NOT NULL,
    mottaker text NOT NULL,                             -- Dette sier noe om hvilken topic meldingen skal sendes til. Rapid/Subsumsjon
    sendt timestamptz default null                      -- Settes først når meldingen er bekreftet sendt til mottaker. Hvis null, så er meldingen ikke sendt enda.
);

CREATE INDEX idx_utboks_sendt ON utboks (sendt) WHERE sendt IS NULL;
CREATE INDEX idx_utboks_key_sendt ON utboks (key) WHERE sendt IS NULL;