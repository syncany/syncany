select *
from fileversion fv
JOIN databaseversion dbv 
     ON fv.databaseversion_id=dbv.id 
     AND dbv.status='MASTER'
where fv.path=? 
order by updated desc
