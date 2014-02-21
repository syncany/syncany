-- Determine all the chunks for a database version

-- Takes into account that a chunk may appear in multiple database versions,
-- but originally belongs to the database version where it appeared first

-- Example: If a chunk appears in database version (A1), it is not returned
-- in this query if databaseversion_vectorclock_serialized='(A17)', even if
-- the chunk is used by a file in this database version.

select checksum, size
from chunk_full
where databaseversion_vectorclock_serialized=?

minus 

select checksum, size
from chunk_full 
where databaseversion_id<(select id from databaseversion where vectorclock_serialized=?)
