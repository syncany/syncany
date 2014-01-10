select *
from databaseversion dbv
join databaseversion_vectorclock vc on dbv.id=vc.databaseversion_id
where dbv.client=?
order by dbv.id desc
