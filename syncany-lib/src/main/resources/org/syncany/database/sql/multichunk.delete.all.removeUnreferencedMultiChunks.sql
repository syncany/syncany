delete from multichunk
where id not in (select distinct multichunk_id from multichunk_chunk)
