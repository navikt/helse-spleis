drop index if exists index_person_fnr;

-- gjør 'opprettet'-kolonnen til å bety 'sist oppdatert'
alter table person alter column opprettet drop not null;
alter table person alter column opprettet set default null;
alter table person rename column opprettet to oppdatert;

-- gjør fnr-feltet unikt og fjern aktor_id (siden dette er i unike_person-tabellen)
alter table person
    add constraint person_fnr_uniqe unique(fnr),
    drop column aktor_id;

-- setter 'oppdatert'-tidspunktet automatisk etter alle UPDATEs mot person-tabellen
CREATE  FUNCTION oppdater_oppdatert_tidspunkt()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.oppdatert = now() at time zone 'utc';
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_task_updated_on
    BEFORE UPDATE ON person
    FOR EACH ROW
EXECUTE PROCEDURE oppdater_oppdatert_tidspunkt();