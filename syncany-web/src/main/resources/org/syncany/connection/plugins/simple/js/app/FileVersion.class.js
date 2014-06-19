function FileVersion(fileHistoryId, version, path, type, status, size, lastModified, 
	checksum, updated, posixPermissions, dosAttributes) {
	
	this.fileHistoryId = fileHistoryId;
	this.version = version;
	this.path = path;
	this.type = type;
	this.status = status;
	this.size = size;
	this.lastModified = lastModified;
	this.checksum = checksum;
	this.updated = updated;
	this.posixPermissions = posixPermissions;
	this.dosAttributes = (dosAttributes) ? dosAttributes : "";

	// Extensions for the frontend
	this.sortType = (type.toLowerCase() == 'folder') ? 1 : 2;
}
