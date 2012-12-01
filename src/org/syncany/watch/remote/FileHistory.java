/*
 * Syncany, www.syncany.org
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
package org.syncany.watch.remote;

import java.util.Collection;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileHistory {
    private String machineName;
    private long fileId;    
    
    private TreeMap<Long, FileUpdate> history;
    private FileHistory pruneHistory;
    private FileHistory branchHistory;
    private FileUpdate lastLocalUpdate;

    public FileHistory(String machineName, long fileId) {
        this.machineName = machineName;
        this.fileId = fileId;
        this.history = new TreeMap<Long, FileUpdate>();
        this.lastLocalUpdate = null;
    }
    
    public void add(FileUpdate u) {
        history.put(u.getVersion(), u);
    }
    
    public FileUpdate get(long version) {
        return history.get(version);
    }
    
    public Collection<FileUpdate> getAllValues(){
    	
    	return history.values();
    }
    
    public FileUpdate getFirstUpdate() {
        return history.firstEntry().getValue();
    }

    public FileUpdate getLastUpdate() {
        return history.lastEntry().getValue();
    }

    public String getMachineName() {
        return machineName;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public long getFileId() {
        return fileId;
    }        

    public TreeMap<Long, FileUpdate> getHistory() {
        return history;
    }

    public FileHistory getPruneHistory() {
        return pruneHistory;
    }

    public void setPruneHistory(FileHistory pruneHistory) {
        this.pruneHistory = pruneHistory;
    }

    public FileUpdate getLastLocalUpdate() {
        return lastLocalUpdate;
    }

    public FileHistory getBranchHistory() {
        return branchHistory;
    }

    public void setBranchHistory(FileHistory branchHistory) {
        this.branchHistory = branchHistory;
    }

    public void setLastLocalUpdate(FileUpdate lastLocalUpdate) {
        this.lastLocalUpdate = lastLocalUpdate;
    }
    
    public NavigableMap<Long, FileUpdate> getNewLocalUpdates() {
        if (lastLocalUpdate == null) {
            return history;
        }
        
        else {
            return history.subMap(lastLocalUpdate.getVersion(), false, history.lastKey(), true);        
        }
    }

    public FileHistory branch() {
        long newFileId = new Random().nextLong(); // TODO Assumes no clashes; check!!                                        
        FileHistory branch = new FileHistory(machineName, newFileId);
        
        for (FileUpdate update : getHistory().values()) {
            FileUpdate newUpdate = (FileUpdate) update.clone();
            newUpdate.setFileId(newFileId);
            
            branch.add(newUpdate);
        }

        return branch;
    }    

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileHistory other = (FileHistory) obj;
        if (this.history != other.history && (this.history == null || !this.history.equals(other.history))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.history != null ? this.history.hashCode() : 0);
        return hash;
    }



}
