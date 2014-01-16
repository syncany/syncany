select *
from fileversion_full 
where databaseversion_vectorclock_serialized=?
order by filehistory_id asc, version asc
