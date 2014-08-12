-- Determine all the multichunks for a database version

select mcc.*, mc.size
from databaseversion dbv
join multichunk mc on dbv.id=mc.databaseversion_id
join multichunk_chunk mcc on mc.id=mcc.multichunk_id
where dbv.vectorclock_serialized=?

