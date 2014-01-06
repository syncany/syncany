delete from filehistory
where id in (
		select distinct fh.id
		from databaseversion dbv
		join filehistory fh on dbv.id=fh.databaseversion_id
		where dbv.status='DIRTY'
	minus distinct
		select distinct fh.id
		from databaseversion dbv
		join filehistory fh on dbv.id=fh.databaseversion_id
		where dbv.status='MASTER'
)
