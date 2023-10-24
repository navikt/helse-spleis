create table if not exists arbeidstabell (
    arbeid_id text unique not null,
    fnr bigint not null,
    arbeid_startet timestamp,
    arbeid_ferdig timestamp
);

create index if not exists arbeidstabell_fnr on arbeidstabell(arbeid_id,fnr);

DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spleis-migrate') THEN
            GRANT ALL ON TABLE "arbeidstabell" TO "spleis-migrate";
        END IF;
    END
$do$
