select dbvm.*
from databaseversion_master dbvm
where 
	dbvm.client=?
	and dbvm.client_version<=?
order by dbvm.id	 
