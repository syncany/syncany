select fcf.*
from filecontent_full fcf
join filecontent_chunk fcc on fcf.checksum=fcc.filecontent_checksum
where fcf.databaseversion_vectorclock_serialized=?
order by checksum asc, num asc
