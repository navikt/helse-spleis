-- brukes ikke. for å gjøre select på aktor_id kan man heller søke
-- i unike_person, og joine unike_person.fnr = person.fnr
drop index if exists index_aktor_id;

-- droppes til fordel for en partial index
drop index if exists index_person_fnr;

-- droppes fordi den ikke brukes
drop index if exists index_person_melding_id;

-- droppes til fordel for partial index
drop index if exists index_person_vedtak;

-- brukes for å gjøre select på fnr, sorter på nyeste rad
-- gjør at spørringen:
-- SELECT data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1
-- kun gjør en Index Only Scan på indeksen, istedenfor Index Scan + Sort
-- total cost blir ca. det samme, men startup-costen er veldig ulik (mindre query planning tid)
-- Postgres-dokumentasjonen sier dette om hvorvidt det kan lønne seg å ha med 'id desc' i indeksen:
-- > […] ORDER BY in combination with LIMIT n: an explicit sort will have to process
-- > all the data to identify the first n rows, but if there is an index
-- > matching the ORDER BY, the first n rows can be retrieved directly,
-- > without scanning the remainder at all.
create index if not exists index_person_fnr on person(fnr,id desc);

-- brukes for å gjøre select på inntektsmeldinger (ifm. replay).
-- beholder den eksisterende generelle indeksen på fnr fordi
-- vi gjør select på mange hendelse-typer når vi deserialiserer person
create index if not exists index_melding_fnr_inntektsmelding on melding(fnr,lest_dato asc)
    where melding_type = 'INNTEKTSMELDING';

-- brukes for å slette eldre rader hvor vedtak=false
create index if not exists index_person_vedtak_partial on person(fnr) where vedtak is false;