-- This query inserts a single file content / chunk ref, but only if it does not exist already.
-- The query avoids key constraint exceptions. 

-- See:
-- + http://hsqldb.org/doc/2.0/guide/dataaccess-chapt.html#dac_merge_statement
-- + http://stackoverflow.com/a/2655567/1440785

merge into filecontent_chunk as filecontent_chunk_target
using (values(?, ?)) as filecontent_chunk_ref(filecontent_checksum, chunk_checksum)
on (filecontent_chunk_target.filecontent_checksum = filecontent_chunk_ref.filecontent_checksum 
	and filecontent_chunk_target.chunk_checksum = filecontent_chunk_ref.chunk_checksum)
when not matched then insert (filecontent_checksum, chunk_checksum, num) 
	values (filecontent_chunk_ref.filecontent_checksum, filecontent_chunk_ref.chunk_checksum, ?)
