DO
$do$
BEGIN
   IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spleis-api') THEN
GRANT SELECT ON ALL TABLES IN SCHEMA public TO "spleis-api";
END IF;
END
$do$
