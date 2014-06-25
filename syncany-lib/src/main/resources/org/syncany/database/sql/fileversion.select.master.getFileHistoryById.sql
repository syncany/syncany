select * 
from fileversion
where 
	filehistory_id=?
	and databaseversion_id in (
		select id from databaseversion where status='MASTER'
	)
order by version asc
