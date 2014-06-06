select * from (
	select 
		fv.filehistory_id, fv.version, fv.databaseversion_id,
		substr(path, length(?)+1, length(path)-length(?)) path,
		fv.type, fv.status, fv.size, fv.lastmodified, fv.linktarget, fv.filecontent_checksum,
		fv.updated, fv.posixperms, fv.dosattrs		
	from fileversion_master_last fv
	where path like concat(?, '%') 
	order by path
)
where locate('/', path) = 0
