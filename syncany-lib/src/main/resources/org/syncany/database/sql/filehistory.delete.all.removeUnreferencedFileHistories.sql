delete from filehistory
where (id, databaseversion_id) not in (
	select filehistory_id, databaseversion_id
	from fileversion
)


