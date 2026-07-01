DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spleis-api') THEN
            GRANT SELECT ON TABLE sendt TO "spleis-api";
        END IF;
    END
$do$;
