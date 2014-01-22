select fc.checksum, fc.size, fcc.chunk_checksum, fcc.num
from filecontent fc
join filecontent_chunk fcc on fc.checksum=fcc.filecontent_checksum
where fc.checksum=?
order by fcc.num asc
