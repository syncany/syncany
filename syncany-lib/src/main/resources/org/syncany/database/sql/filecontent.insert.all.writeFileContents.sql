-- This query inserts a single file content, but only if it does not exist already.
-- The query avoids key constraint exceptions. 

-- See:
-- + http://hsqldb.org/doc/2.0/guide/dataaccess-chapt.html#dac_merge_statement
-- + http://stackoverflow.com/a/2655567/1440785

merge into filecontent as filecontent_target
using (values(?)) as filecontent_ref(checksum)
on (filecontent_target.checksum = filecontent_ref.checksum)
when not matched then insert (checksum, databaseversion_id, size) values (filecontent_ref.checksum, ?, ?)
