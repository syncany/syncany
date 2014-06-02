-- Determine all the chunks for a database version

select checksum, size
from chunk
where databaseversion_id=(select id from databaseversion where vectorclock_serialized=?)

