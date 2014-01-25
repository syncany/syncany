-- Determine all the multichunks for a database version

-- The multichunk_full view is joined with the "chunk" table, so the view 
-- might also contain multichunks from previous database versions.

-- To remove these, the "minus" query removes the multichunks from 
-- previous versions.

-- TODO [medium] This is not particularly comprehensive. Refactor this.

select mcc.*
from multichunk_full mcf
join multichunk_chunk mcc on mcf.multichunk_id=mcc.multichunk_id
where mcf.databaseversion_vectorclock_serialized=?

minus 

select mcc.*
from multichunk_full mcf
join multichunk_chunk mcc on mcf.multichunk_id=mcc.multichunk_id
where mcf.databaseversion_id<(select id from databaseversion where vectorclock_serialized=?)
