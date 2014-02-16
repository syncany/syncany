select distinct mcc.multichunk_id 
from filecontent fc 
join filecontent_chunk fcc on fc.checksum=fcc.filecontent_checksum 
join multichunk_chunk mcc on fcc.chunk_checksum=mcc.chunk_checksum 
where fc.checksum=?
