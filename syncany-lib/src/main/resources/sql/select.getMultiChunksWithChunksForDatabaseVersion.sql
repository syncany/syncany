-- Determine all the multichunks for a database version

-- Takes into account that a multichunk may appear in multiple database versions,
-- but originally belongs to the database version where it appeared first

select mcc.*
from multichunk_full mcf
join multichunk_chunk mcc on mcf.multichunk_id=mcc.multichunk_id
where mcf.databaseversion_vectorclock_serialized=?

minus 

select mcc.*
from multichunk_full mcf
join multichunk_chunk mcc on mcf.multichunk_id=mcc.multichunk_id
where mcf.databaseversion_id<(select id from databaseversion where vectorclock_serialized=?)
