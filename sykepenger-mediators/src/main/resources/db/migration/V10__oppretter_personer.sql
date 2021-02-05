insert into unike_person (fnr,aktor_id)
select p.fnr, p.aktor_id from person p
left join unike_person up on up.fnr = p.fnr
where up.fnr is null
on conflict do nothing;
