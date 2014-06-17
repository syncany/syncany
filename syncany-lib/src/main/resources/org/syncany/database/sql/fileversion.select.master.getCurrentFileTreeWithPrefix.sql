select *
from (
	select 
		filehistory_id, version, databaseversion_id, path,
		type, status, size, lastmodified, linktarget, filecontent_checksum,
		updated, posixperms, dosattrs,
		substr(path, length(?)+1, length(path)-length(?)) path_short	
	from fileversion_master_last
	where path like concat(?, '%') 
	order by path
)
where locate('/', path_short) = 0
