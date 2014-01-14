select fv.*
from databaseversion dbv
join filehistory fh on dbv.id=fh.databaseversion_id
join fileversion fv on fh.id=fv.filehistory_id
where dbv.localtime<=?
  and fv.status<>'DELETED'
  and fv.version=(select max(fv1.version) from fileversion fv1 where fv.filehistory_id=fv1.filehistory_id)
