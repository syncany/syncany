select fvp.*
from databaseversion dbv
join fileversion_purge fvp on dbv.id=fvp.databaseversion_id
where dbv.vectorclock_serialized=?
