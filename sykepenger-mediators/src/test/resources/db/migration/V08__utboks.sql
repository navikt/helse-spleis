CREATE TABLE utboks (
    id uuid PRIMARY KEY,
    forarsaket_av uuid NOT NULL, -- Dette er hendelseId'en til hendelsen som forårsaket at denne meldingen ble sendt til utboks
    key text,
    json jsonb NOT NULL,
    mottaker text NOT NULL,
    sendt timestamptz default null
);

CREATE INDEX idx_utboks_sendt ON utboks (sendt) WHERE sendt IS NULL;
CREATE INDEX idx_utboks_key_sendt ON utboks (key) WHERE sendt IS NULL;