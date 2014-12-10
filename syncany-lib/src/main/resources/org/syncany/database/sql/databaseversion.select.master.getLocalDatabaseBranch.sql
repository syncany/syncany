select dbv.id, dbv.localtime, dbv.client, vc.client as vc_client, vc.logicaltime as vc_logicaltime 
from databaseversion dbv 
join databaseversion_vectorclock vc on vc.databaseversion_id=dbv.id 
where dbv.status='MASTER'
order by dbv.id asc, vc.client
