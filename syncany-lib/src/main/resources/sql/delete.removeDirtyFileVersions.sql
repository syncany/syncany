delete from fileversion
where (databaseversion_id, filehistory_id, version) in (
	select databaseversion_id, filehistory_id, version
	from fileversion_full
	where databaseversion_status='DIRTY'
)
