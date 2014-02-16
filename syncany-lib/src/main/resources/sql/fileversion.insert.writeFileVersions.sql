insert into fileversion (
	filehistory_id, version, databaseversion_id, path, type, status, 
	size, lastmodified, linktarget, filecontent_checksum, updated, 
	posixperms, dosattrs) 
values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
