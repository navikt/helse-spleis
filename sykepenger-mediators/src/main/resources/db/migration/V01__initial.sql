CREATE FUNCTION oppdater_oppdatert_tidspunkt() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.oppdatert = now();
    RETURN NEW;
END;
$$;


CREATE TABLE arbeidstabell (
    arbeid_id text NOT NULL,
    fnr bigint NOT NULL,
    arbeid_startet timestamp without time zone,
    arbeid_ferdig timestamp without time zone
);

CREATE UNIQUE INDEX arbeidstabell_arbeid_fnr ON arbeidstabell USING btree (arbeid_id, fnr);


CREATE TABLE melding (
     id bigserial primary key,
     fnr bigint NOT NULL,
     melding_id character varying(40) NOT NULL,
     melding_type character varying(40) NOT NULL,
     data json NOT NULL,
     lest_dato timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
     behandlet_tidspunkt timestamp without time zone
);

CREATE INDEX index_melding_fnr ON melding USING btree (fnr);
CREATE INDEX index_melding_fnr_inntektsmelding ON melding USING btree (fnr, lest_dato) WHERE ((melding_type)::text = 'INNTEKTSMELDING'::text);
CREATE UNIQUE INDEX index_melding_id ON melding USING btree (melding_id);

CREATE TABLE person (
    id bigserial primary key,
    skjema_versjon integer NOT NULL,
    fnr bigint NOT NULL unique,
    data text NOT NULL,
    oppdatert timestamp with time zone,
    aktor_id bigint NOT NULL,
    opprettet timestamp without time zone DEFAULT now() NOT NULL,
    sist_avstemt timestamp without time zone
);

CREATE INDEX index_person_skjemaversjon ON person USING btree (skjema_versjon);
CREATE TRIGGER update_user_task_updated_on BEFORE UPDATE ON person FOR EACH ROW EXECUTE FUNCTION oppdater_oppdatert_tidspunkt();

CREATE TABLE person_alias (
    fnr bigint primary key ,
    person_id bigint NOT NULL references person(id)
);

CREATE INDEX person_alias_fk ON person_alias USING btree (person_id);


DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spleis-migrate') THEN
            GRANT ALL ON TABLE arbeidstabell TO "spleis-migrate";
            GRANT SELECT ON TABLE melding TO "spleis-migrate";
            GRANT SELECT,UPDATE ON TABLE person TO "spleis-migrate";
        END IF;
    END
$do$;

DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spleis-avstemming') THEN
            GRANT SELECT ON TABLE melding TO "spleis-avstemming";
            GRANT SELECT ON TABLE person TO "spleis-avstemming";
            GRANT SELECT ON TABLE person_alias TO "spleis-avstemming";
        END IF;
    END
$do$;

DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='spleis-api') THEN
            GRANT SELECT ON TABLE melding TO "spleis-api";
            GRANT SELECT ON TABLE person TO "spleis-api";
            GRANT SELECT ON TABLE person_alias TO "spleis-api";
        END IF;
    END
$do$;