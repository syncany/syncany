select distinct mc.id 
from databaseversion dbv
join filehistory fh on dbv.id=fh.databaseversion_id
join fileversion fv on fh.id=fv.filehistory_id
join filecontent fc on fv.filecontent_checksum=fc.checksum
join filecontent_chunk fcc on fc.checksum=fcc.filecontent_checksum
join chunk c on fcc.chunk_checksum=c.checksum
join multichunk_chunk mcc on c.checksum=mcc.chunk_checksum
join multichunk mc on mcc.multichunk_id=mc.id
where 
	dbv.status=? 
	and mc.id=?
