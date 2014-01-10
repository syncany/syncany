delete from chunk
where checksum in (
		select checksum
		from chunk_full
		where databaseversion_status='DIRTY'
	minus distinct 
		select checksum
		from chunk_full
		where databaseversion_status='MASTER'
)
