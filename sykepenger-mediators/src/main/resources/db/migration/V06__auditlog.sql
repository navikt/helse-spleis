CREATE TABLE auditlog(
    id                      bigserial primary key,
    pid                     bigint not null default pg_backend_pid(),
    tidspunkt               timestamptz not null default now(),
    personidentifikator     text not null,
    epost                   text not null,
    diff                    text not null,
    beskrivelse             text not null
);