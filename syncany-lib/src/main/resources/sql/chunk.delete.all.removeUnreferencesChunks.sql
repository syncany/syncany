-- The double "not in" is not required, but it prevents a foreign key constraint
-- issue if we didn't delete all the filecontents/multichunks properly

delete from chunk
where 
	    checksum not in (select distinct chunk_checksum from multichunk_chunk)
	and checksum not in (select distinct chunk_checksum from filecontent_chunk)	
