function FileVersion(version, path, type, status, size, lastModified, checksum, updated, posixPermissions) {
	this.version = version;
	this.path = path;
	this.type = type;
	this.status = status;
	this.size = size;
	this.lastModified = lastModified;
	this.checksum = checksum;
	this.updated = updated;
	this.posixPermissions = posixPermissions;
}
