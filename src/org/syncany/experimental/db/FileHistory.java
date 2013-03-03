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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 *
 * @author pheckel
 */
public class FileHistory {
    private Long fileId;
    private TreeMap<Long, FileVersion> versions;
    
    public FileHistory() {
        this.fileId = new Random().nextLong();
        this.versions = new TreeMap<Long, FileVersion>();
    }    

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Map<Long, FileVersion> getFileVersions() {
        return Collections.unmodifiableMap(versions);
    }
    
    public FileVersion getFileVersion(long version) {
    	return versions.get(version);
    }
    
    public FileVersion getLastVersion() {
        if (versions.isEmpty()) {
            return null;
        }
        
        return versions.lastEntry().getValue();
    }   

    @Deprecated
    public FileVersion createVersion() {        
        FileVersion newVersion;
        
        if (!versions.isEmpty()) {        	
            FileVersion lastVersion = versions.get(versions.size()-1);        
            
            newVersion = (FileVersion) lastVersion.clone();
            newVersion.setVersion(lastVersion.getVersion()+1);
        }
        else {
            newVersion = new FileVersion();
            newVersion.setVersion(1L);
        }
        
        // Add to list
        //versions.add(newVersion);        
        //newVersions.add(newVersion);
        
        return newVersion;
    }
        
    public void addFileVersion(FileVersion fileVersion) {
        versions.put(fileVersion.getVersion(), fileVersion);        
    }
}
