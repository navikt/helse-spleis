
DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='sparsom') THEN
            GRANT SELECT ON person TO "sparsom";
            GRANT SELECT ON unike_person TO "sparsom";
        END IF;
    END
$do$
