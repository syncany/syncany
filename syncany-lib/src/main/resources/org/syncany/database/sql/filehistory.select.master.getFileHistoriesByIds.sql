select * 
from fileversion_master
where filehistory_id in (unnest(?))
order by filehistory_id, version
