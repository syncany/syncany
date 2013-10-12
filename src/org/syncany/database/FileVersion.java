/*
 * Syncany
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import java.util.Arrays;
import java.util.Date;

import org.syncany.util.StringUtil;

/**
 *
 * @author pheckel
 */
public class FileVersion implements Cloneable {
	// Mandatory
    private Long version;   
    private String path;
    private FileType type; 
    private FileStatus status;    
    private Long size; 
    private Date lastModified;

    // Mandatory (if type is symlink)
    private String linkTarget;
    
    // Optional
    private String createdBy;
    private byte[] checksum;
    private Date updated;
    private String posixPermissions;
    private String dosAttributes;
    
    public FileVersion() {
        // Fressen.
    }      

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
    
    public byte[] getChecksum() {
		return checksum;
	}

	public void setChecksum(byte[] checksum) {
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
				+ lastModified + ", linkTarget=" + linkTarget + ", createdBy=" + createdBy + ", checksum=" + StringUtil.toHex(checksum) + ", updated="
				+ updated + ", posixPermissions=" + posixPermissions + ", dosAttributes=" + dosAttributes + "]";
	}

	@Override
    public FileVersion clone() {
        try {
            FileVersion clone = (FileVersion) super.clone();
            
            clone.setChecksum(getChecksum());
            clone.setCreatedBy(getCreatedBy());
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
		result = prime * result + Arrays.hashCode(checksum);
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((lastModified == null) ? 0 : lastModified.hashCode());
		result = prime * result + ((size == null) ? 0 : size.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((updated == null) ? 0 : updated.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileVersion other = (FileVersion) obj;
		if (!Arrays.equals(checksum, other.checksum))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (lastModified == null) {
			if (other.lastModified != null)
				return false;
		} else if (!lastModified.equals(other.lastModified))
			return false;
		if (size == null) {
			if (other.size != null)
				return false;
		} else if (!size.equals(other.size))
			return false;
		if (status != other.status)
			return false;
		if (type != other.type)
			return false;
		if (updated == null) {
			if (other.updated != null)
				return false;
		} else if (!updated.equals(other.updated))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	public enum FileStatus {
		UNKNOWN ("UNKNOWN"), 
		NEW ("NEW"), 
		CHANGED ("CHANGED"), 
		RENAMED ("RENAMED"), 
		DELETED ("DELETED"),
		ATTRS_CHANGED ("ATTRS_CHANGED"),
		UNCHANGED ("UNCHANGED");
		
		private String name;       
		
		private FileStatus(String name) {
			this.name = name;
		}
		
		public boolean equalsName(String otherName){
			return (otherName == null) ? false : name.equals(otherName);
		}
		
		public String toString() {
			return name;
		}	
	}
	
	public enum FileType {
		FILE ("FILE"), 
		FOLDER ("FOLDER"),
		SYMLINK ("SYMLINK");
		
		private String name;       
		
		private FileType(String name) {
			this.name = name;
		}
		
		public boolean equalsName(String otherName){
			return (otherName == null) ? false : name.equals(otherName);
		}
		
		public String toString() {
			return name;
		}	
	}
}
