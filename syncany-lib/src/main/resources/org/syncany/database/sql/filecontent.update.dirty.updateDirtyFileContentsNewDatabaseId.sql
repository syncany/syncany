update filecontent 
set databaseversion_id=?
where databaseversion_id in (
	select id 
	from databaseversion
	where status='DIRTY'
)

