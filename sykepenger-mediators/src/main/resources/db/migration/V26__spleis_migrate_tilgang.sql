DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spleis-migrate') THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "spleis-migrate";
            GRANT UPDATE ON person TO "spleis-migrate";
        END IF;
    END
$do$
