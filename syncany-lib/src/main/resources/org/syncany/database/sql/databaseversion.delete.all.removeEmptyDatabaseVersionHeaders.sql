delete from databaseversion 
where id not in (
	select dbv.id
	from databaseversion dbv
	join fileversion fv on dbv.id=fv.databaseversion_id
)
and id not in (
	select dbv.id
	from databaseversion dbv
	join chunk on dbv.id=chunk.databaseversion_id
)
