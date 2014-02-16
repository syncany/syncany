delete from fileversion
where (databaseversion_id, filehistory_id, version) in (
	select fv.databaseversion_id , fv.filehistory_id, fv.version
	from databaseversion dbv
	join fileversion fv on dbv.id=fv.databaseversion_id
	where dbv.status='DIRTY'
)
