-- This query inserts a single chunk, but only if it does not exist already.
-- The query avoids key constraint exceptions. 

-- See:
-- + http://hsqldb.org/doc/2.0/guide/dataaccess-chapt.html#dac_merge_statement
-- + http://stackoverflow.com/a/2655567/1440785

merge into chunk as chunk_target
using (values(?)) as chunk_ref(checksum)
on (chunk_target.checksum = chunk_ref.checksum)
when not matched then insert (checksum, size) values (chunk_ref.checksum, ?)
