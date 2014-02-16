delete from filehistory
where (id, databaseversion_id) in (
	select distinct fh.id, fh.databaseversion_id
	from databaseversion dbv
	join filehistory fh on dbv.id=fh.databaseversion_id
	where dbv.status='DIRTY'
)
