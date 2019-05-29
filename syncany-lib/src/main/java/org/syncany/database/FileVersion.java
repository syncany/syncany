/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.database;

import java.io.File;
import java.util.Date;

import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.PartialFileHistory.FileHistoryId;

/**
 * A file version represents a version of a file at a certain time and captures
 * all of a file's properties.
 *
 * <p>A {@link PartialFileHistory} typically consists of multiple <code>FileVersion</code>s,
 * each of which is the incarnation of the same file, but with either changed properties,
 * or changed content.
 *
 * <p>The <code>FileVersion</code>'s checksum attribute implicitly links to a {@link FileContent},
 * which represents the content of a file. Multiple file versions can link to the same file content.
 *
 * @see PartialFileHistory
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class FileVersion implements Cloneable {
	// Optional
	private FileHistoryId fileHistoryId;

	// Mandatory
	private Long version; // TODO [low] This can be an Integer. No need for a long!
	private String path;
	private FileType type;
	private FileStatus status;
	private Long size;
	private Date lastModified;

	// Mandatory (if type is symlink)
	private String linkTarget;

	// Optional
	private FileChecksum checksum;
	private Date updated;
	private String posixPermissions;
	private String dosAttributes;

	public FileVersion() {
		// Fressen.
	}

	public FileHistoryId getFileHistoryId() {
		return fileHistoryId;
	}

	public void setFileHistoryId(FileHistoryId fileHistoryId) {
		this.fileHistoryId = fileHistoryId;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public FileType getType() {
		return type;
	}

	public void setType(FileType type) {
		this.type = type;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public FileStatus getStatus() {
		return status;
	}

	public void setStatus(FileStatus status) {
		this.status = status;
	}

	public String getPath() {
		return path;
	}

	public String getName() {
		return new File(path).getName();
	}

	public void setPath(String path) {
		this.path = path;
	}

	public FileChecksum getChecksum() {
		return checksum;
	}

	public void setChecksum(FileChecksum checksum) {
		this.checksum = checksum;
	}

	public Long getSize() { // TODO [low] Redundant field 'size', this field should not exist. Instead the content's size should be used. This was introduced as a convenience field.
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public String getLinkTarget() {
		return linkTarget;
	}

	public void setLinkTarget(String linkTarget) {
		this.linkTarget = linkTarget;
	}

	public String getPosixPermissions() {
		return posixPermissions;
	}

	public void setPosixPermissions(String posixPermissions) {
		this.posixPermissions = posixPermissions;
	}

	public String getDosAttributes() {
		return dosAttributes;
	}

	public void setDosAttributes(String dosAttributes) {
		this.dosAttributes = dosAttributes;
	}

	@Override
	public String toString() {
		return "FileVersion [version=" + version + ", path=" + path + ", type=" + type + ", status=" + status + ", size=" + size + ", lastModified="
				+ lastModified + ", linkTarget=" + linkTarget + ", checksum=" + checksum + ", updated="
				+ updated + ", posixPermissions=" + posixPermissions + ", dosAttributes=" + dosAttributes + "]";
	}

	@Override
	public FileVersion clone() {
		try {
			FileVersion clone = (FileVersion) super.clone();

			clone.setChecksum(getChecksum());
			clone.setLastModified(getLastModified());
			clone.setUpdated(getUpdated());
			clone.setPath(getPath());
			clone.setType(getType());
			clone.setVersion(getVersion());
			clone.setSize(getSize());
			clone.setDosAttributes(getDosAttributes());
			clone.setPosixPermissions(getPosixPermissions());
			clone.setLinkTarget(getLinkTarget());
			clone.setStatus(getStatus());

			return clone;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((checksum == null) ? 0 : checksum.hashCode());
		result = prime * result + ((dosAttributes == null) ? 0 : dosAttributes.hashCode());
		result = prime * result + ((lastModified == null) ? 0 : lastModified.hashCode());
		result = prime * result + ((linkTarget == null) ? 0 : linkTarget.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((posixPermissions == null) ? 0 : posixPermissions.hashCode());
		result = prime * result + ((size == null) ? 0 : size.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((updated == null) ? 0 : updated.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FileVersion)) {
			return false;
		}
		FileVersion other = (FileVersion) obj;
		if (checksum == null) {
			if (other.checksum != null) {
				return false;
			}
		}
		else if (!checksum.equals(other.checksum)) {
			return false;
		}
		if (dosAttributes == null) {
			if (other.dosAttributes != null) {
				return false;
			}
		}
		else if (!dosAttributes.equals(other.dosAttributes)) {
			return false;
		}
		if (lastModified == null) {
			if (other.lastModified != null) {
				return false;
			}
		}
		else if (!lastModified.equals(other.lastModified)) {
			return false;
		}
		if (linkTarget == null) {
			if (other.linkTarget != null) {
				return false;
			}
		}
		else if (!linkTarget.equals(other.linkTarget)) {
			return false;
		}
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		}
		else if (!path.equals(other.path)) {
			return false;
		}
		if (posixPermissions == null) {
			if (other.posixPermissions != null) {
				return false;
			}
		}
		else if (!posixPermissions.equals(other.posixPermissions)) {
			return false;
		}
		if (size == null) {
			if (other.size != null) {
				return false;
			}
		}
		else if (!size.equals(other.size)) {
			return false;
		}
		if (status != other.status) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		if (updated == null) {
			if (other.updated != null) {
				return false;
			}
		}
		else if (!updated.equals(other.updated)) {
			return false;
		}
		if (version == null) {
			if (other.version != null) {
				return false;
			}
		}
		else if (!version.equals(other.version)) {
			return false;
		}
		return true;
	}

	public enum FileStatus {
		NEW("NEW"),
		CHANGED("CHANGED"),
		RENAMED("RENAMED"),
		DELETED("DELETED");

		private String name;

		private FileStatus(String name) {
			this.name = name;
		}

		public boolean equalsName(String otherName) {
			return (otherName == null) ? false : name.equals(otherName);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * A {@link FileVersion} can be of either one of the types in this enum.
	 * Types are treated differently during the index and synchronization process.
	 */
	public enum FileType {
		FILE("FILE"),
		FOLDER("FOLDER"),
		SYMLINK("SYMLINK");

		private String name;

		private FileType(String name) {
			this.name = name;
		}

		public boolean equalsName(String otherName) {
			return (otherName == null) ? false : name.equals(otherName);
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
