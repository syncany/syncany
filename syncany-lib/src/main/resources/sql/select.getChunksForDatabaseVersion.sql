-- Determine all the chunks for a database version

-- Takes into account that a chunk may appear in multiple database versions,
-- but originally belongs to the database version where it appeared first

select  
	min(cf.databaseversion_id) as databaseversion_id, 
	cf.checksum,
	cf.size
from chunk_full cf
where cf.databaseversion_vectorclock_serialized=?
group by cf.checksum, cf.size
