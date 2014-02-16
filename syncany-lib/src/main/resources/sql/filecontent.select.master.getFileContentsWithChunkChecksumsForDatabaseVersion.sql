-- Determine all the filecontents for a database version

-- Takes into account that a filecontent may appear in multiple database versions,
-- but originally belongs to the database version where it appeared first

select fcf.checksum, fcf.size, fcf.chunk_checksum, fcf.num
from filecontent_full fcf
join filecontent_chunk fcc on fcf.checksum=fcc.filecontent_checksum
where fcf.databaseversion_vectorclock_serialized=?

minus 

select fcf.checksum, fcf.size, fcf.chunk_checksum, fcf.num
from filecontent_full fcf
join filecontent_chunk fcc on fcf.checksum=fcc.filecontent_checksum
where fcf.databaseversion_id<(select id from databaseversion where vectorclock_serialized=?)
order by checksum asc, num asc
