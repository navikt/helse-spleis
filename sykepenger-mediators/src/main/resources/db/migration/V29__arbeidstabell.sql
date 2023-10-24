alter table arbeidstabell drop constraint arbeidstabell_arbeid_id_key;
drop index arbeidstabell_fnr;
create unique index if not exists arbeidstabell_arbeid_fnr on arbeidstabell(arbeid_id,fnr);
