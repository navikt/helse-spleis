DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spleis-opprydding-dev') THEN
            GRANT DELETE ON ALL TABLES IN SCHEMA public TO "spleis-opprydding-dev";
        END IF;
    END
$do$
