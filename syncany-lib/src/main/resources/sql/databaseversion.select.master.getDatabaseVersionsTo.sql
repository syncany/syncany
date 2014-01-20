select dbvm.*
from databaseversion_master dbvm
where 
	dbvm.client=?
	and dbvm.id <= (
		select id from databaseversion_master
		where client=? and client_version=?
	)
order by dbvm.id	 
