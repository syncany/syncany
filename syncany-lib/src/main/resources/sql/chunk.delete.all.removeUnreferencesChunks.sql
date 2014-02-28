delete from chunk
where checksum not in (select distinct chunk_checksum from multichunk_chunk)
