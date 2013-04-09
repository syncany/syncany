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
package org.syncany.experimental.db;

import java.util.Date;

import org.syncany.Constants;

/**
 *
 * @author pheckel
 */
public class FileVersion implements Cloneable {
    
    /**
     * <ul>
     * <li>UNKNOWN
     * <li>NEW: New file
     * <lI>CHANGED: The file contents have changed. At least one chunk differs.
     * <li>RENAMED: The file path or name has changed.
     * <li>MERGED: The file history has been merged to a different file.
     * </ul>
     */
    public enum Status { NEW, CHANGED, RENAMED, DELETED, MERGED };
        
    private Long fileId;
    private Long version;   
    private Client createdBy;
    private FileContent content;
    private String path;
    private String name;   
    private Date lastModified;
    private Date updated;
    private Status status;    
    
    public FileVersion() {
        
    }
    
    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long id) {
        this.fileId = id;
    }

    public FileContent getContent() {
        return content;
    }

    public void setContent(FileContent content) {
        this.content = content;
    }

    public Client getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Client createdBy) {
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    public boolean isFolder() {
        return content == null;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    
    public String getFullName() {
    	return this.path + Constants.DATABASE_FILE_SEPARATOR + this.name;
    }
    
    @Override
    public Object clone() {
        try {
            FileVersion clone = (FileVersion) super.clone();
            
            clone.setContent(getContent());
            clone.setCreatedBy(getCreatedBy());
            clone.setLastModified(getLastModified());
            clone.setName(getName());
            clone.setPath(getPath());
            clone.setVersion(getVersion());
            
            return clone;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }        
    }

  
}
