DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spleis-avstemming') THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "spleis-avstemming";
        END IF;
    END
$do$
