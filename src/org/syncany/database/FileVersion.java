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

import java.util.Arrays;
import java.util.Date;

import org.syncany.config.Constants;
import org.syncany.util.StringUtil;

/**
 *
 * @author pheckel
 */
public class FileVersion implements Cloneable {
	public enum FileStatus {
		UNKNOWN ("UNKNOWN"), NEW ("NEW"), CHANGED ("CHANGED"), RENAMED ("RENAMED"), DELETED ("DELETED"), MERGED ("MERGED");
		
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
		FOLDER ("FOLDER");
		
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
    
	// Mandatory
    private Long version;   
    private String path;
    private String name;   
    private FileType type; 
    private FileStatus status;    

    // Optional
    private String createdBy;
    private byte[] checksum;
    private Date lastModified;
    private Date updated;
    
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileType getType() {
		return type;
	}

	public void setType(FileType type) {
		this.type = type;
	}

	public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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
    
    public String getFullName() {
    	return this.path + Constants.DATABASE_FILE_SEPARATOR + this.name;
    }
    
    public byte[] getChecksum() {
		return checksum;
	}

	public void setChecksum(byte[] checksum) {
		this.checksum = checksum;
	}

	@Override
	public String toString() {
		return "FileVersion(version=" + version + ", createdBy=" + createdBy + ", checksum=" + StringUtil.toHex(checksum) + ", path=" + path
				+ ", name=" + name + ", lastModified=" + lastModified + ", updated=" + updated + ", status=" + status + ")";
	}

	@Override
    public FileVersion clone() {
        try {
            FileVersion clone = (FileVersion) super.clone();
            
            clone.setChecksum(getChecksum());
            clone.setCreatedBy(getCreatedBy());
            clone.setLastModified(getLastModified());
            clone.setUpdated(getUpdated());
            clone.setName(getName());
            clone.setPath(getPath());
            clone.setType(getType());
            clone.setVersion(getVersion());
            
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
		result = prime * result + ((lastModified == null) ? 0 : lastModified.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
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
		if (lastModified == null) {
			if (other.lastModified != null)
				return false;
		} else if (!lastModified.equals(other.lastModified))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (status != other.status)
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
}
