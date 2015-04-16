select *
from fileversion_master_last
where fileversion_master_last.filecontent_checksum = ?
order by filehistory_id asc, version asc

