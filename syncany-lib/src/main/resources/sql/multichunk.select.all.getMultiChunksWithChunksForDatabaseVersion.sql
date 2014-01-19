-- Determine all the multichunks for a database version

--- Takes into account that a multichunk may appear in multiple database versions,
--- but originally belongs to the database version where it appeared first

select mcc.*
from multichunk_full mcf
join multichunk_chunk mcc on mcf.multichunk_id=mcc.multichunk_id
where mcf.databaseversion_vectorclock_serialized=?

minus 

select mcc.*
from multichunk_full mcf
join multichunk_chunk mcc on mcf.multichunk_id=mcc.multichunk_id
where mcf.databaseversion_id<(select id from databaseversion where vectorclock_serialized=?)


-- TODO Honestly, I don't know anymore why this query does a "minus", but some
-- tests fail if its not there.

-- select distinct mc.id as multichunk_id, c.checksum as chunk_checksum
-- from databaseversion dbv
-- join fileversion fv on fv.databaseversion_id=dbv.id
-- join filecontent fc on fv.filecontent_checksum=fc.checksum
-- join filecontent_chunk fcc on fc.checksum=fcc.filecontent_checksum
-- join chunk c on fcc.chunk_checksum=c.checksum
-- join multichunk_chunk mcc on c.checksum=mcc.chunk_checksum
-- join multichunk mc on mcc.multichunk_id=mc.id
-- where dbv.vectorclock_serialized=?

