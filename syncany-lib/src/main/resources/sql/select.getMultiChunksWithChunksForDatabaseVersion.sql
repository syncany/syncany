select mcc.*
from multichunk_full mcf
join multichunk_chunk mcc on mcf.multichunk_id=mcc.multichunk_id
where mcf.databaseversion_vectorclock_serialized=?
