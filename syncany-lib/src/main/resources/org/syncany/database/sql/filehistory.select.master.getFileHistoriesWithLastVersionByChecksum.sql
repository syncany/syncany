select *
from fileversion fv
JOIN databaseversion dbv 
     ON fv.databaseversion_id=dbv.id 
     AND dbv.status='MASTER'  
where fv.filecontent_checksum = ?
order by filehistory_id asc, version asc

