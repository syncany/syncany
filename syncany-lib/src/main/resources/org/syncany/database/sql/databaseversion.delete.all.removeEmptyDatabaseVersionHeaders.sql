-- We delete a databaseversion if it contains nothing. 
-- This is checked by joining on all relevent tables and filtering 
-- the ids that appear there. The only table that has a foreign key
-- on databaseversion_id is fileversion, since fileversions are always
-- contained in a filehistory. All other elements can be
-- left in a databaseversion by themselves.
delete from databaseversion 
where id not in (
	select dbv.id
	from databaseversion dbv
	join filehistory fh on dbv.id=fh.databaseversion_id
)
and id not in (
	select dbv.id
	from databaseversion dbv
	join chunk on dbv.id=chunk.databaseversion_id
)
and id not in (
	select dbv.id
	from databaseversion dbv
	join multichunk on dbv.id=multichunk.databaseversion_id
)
and id not in (
    select dbv.id
	from databaseversion dbv
	join filecontent on dbv.id=filecontent.databaseversion_id
)
