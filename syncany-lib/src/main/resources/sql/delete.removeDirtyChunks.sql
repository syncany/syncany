delete from chunk
where checksum in (
		select distinct c.checksum
		from databaseversion dbv
		join filehistory fh on dbv.id=fh.databaseversion_id
		join fileversion fv on fh.id=fv.filehistory_id
		join filecontent fc on fv.filecontent_checksum=fc.checksum
		join filecontent_chunk fcc on fc.checksum=fcc.filecontent_checksum
		join chunk c on fcc.chunk_checksum=c.checksum
		where dbv.status='DIRTY'
	minus distinct 
		select distinct c.checksum
		from databaseversion dbv
		join filehistory fh on dbv.id=fh.databaseversion_id
		join fileversion fv on fh.id=fv.filehistory_id
		join filecontent fc on fv.filecontent_checksum=fc.checksum
		join filecontent_chunk fcc on fc.checksum=fcc.filecontent_checksum
		join chunk c on fcc.chunk_checksum=c.checksum
		where dbv.status='MASTER'
)
