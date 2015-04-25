select *
from fileversion
where filecontent_checksum = ?
order by filehistory_id asc, version asc

