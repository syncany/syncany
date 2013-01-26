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
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author pheckel
 */
public class FileHistory implements Serializable, Persistable {
    private Long id;
    private Long fileId;
    private int profileId;
    private String rootId;
    private List<FileVersion> versions;

    private transient List<FileVersion> newVersions;
    
    public FileHistory() {
        this.fileId = new Random().nextLong();
        this.versions = new ArrayList<FileVersion>();
        this.newVersions = new ArrayList<FileVersion>();
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public List<FileVersion> getVersions() {
        return versions;
    }
    
    public FileVersion getLastVersion() {
        if (versions.isEmpty()) {
            return null;
        }
        
        return versions.get(versions.size()-1);
    }
    
    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }        

    /* package */ FileVersion createVersion() {        
        FileVersion newVersion;
        
        if (!versions.isEmpty()) {
            FileVersion lastVersion = versions.get(versions.size()-1);        
            
            newVersion = (FileVersion) lastVersion.clone();
            newVersion.setVersion(lastVersion.getVersion()+1);
        }
        else {
            newVersion = new FileVersion();
            newVersion.setHistory(this);
            newVersion.setVersion(1L);
        }
        
        // Add to list
        versions.add(newVersion);        
        newVersions.add(newVersion);
        
        return newVersion;
    }
        
    /* package */ void addVersion(FileVersion fileVersion) {
        versions.add(fileVersion);        
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(getFileId());        
    }

    @Override
    public int read(DataInput in) throws IOException {
        setFileId(in.readLong());
        return 8;
    }


    
}
