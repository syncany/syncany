-- Determine all the filecontents for a database version

select fc.checksum, fc.size, fcc.chunk_checksum, fcc.num
from filecontent fc
join filecontent_chunk fcc on fc.checksum=fcc.filecontent_checksum
where fc.databaseversion_id=(select id from databaseversion where vectorclock_serialized=?)
order by fc.checksum asc, fcc.num asc
