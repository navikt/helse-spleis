DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spleis-migrate')
    THEN
        ALTER USER "spleis-migrate" IN DATABASE "spleis" SET pgaudit.log TO 'none';
    END IF;
END $$;
