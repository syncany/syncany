-- Should only be used with setMaxRows(1)
select vc.logicaltime
from databaseversion dbv
join databaseversion_vectorclock vc on dbv.id=vc.databaseversion_id
where 
	dbv.status='DIRTY'
	and vc.client=?
order by dbv.id desc

