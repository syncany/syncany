/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * A <tt>PartialFileHistory</tt> represents a single file in a repository over a 
 * certain period of time/versions. Whenever a file is updated or deleted, a new  
 * {@link FileVersion} is added to the file history. 
 * 
 * <p>A file history is identified by a unique random identifier and holds a sorted
 * list of file versions. 
 * 
 * <p>Due to cleanup mechanisms and the delta database concept, the list of file
 * versions is not always complete. The class hence represents a part of the file
 * history.
 * 
 * @see FileVersion
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class PartialFileHistory {
    private static final byte FILE_HISTORY_ID_LENGTH = 20;
    
    private FileHistoryId fileId;
    private TreeMap<Long, FileVersion> versions;
    
    public PartialFileHistory(FileHistoryId fileId) {
        this.fileId = fileId;
        this.versions = new TreeMap<Long, FileVersion>();    	
    }

    public FileHistoryId getFileId() {
        return fileId;
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

    /**
     * Returns an iterator on the version numbers stored in this partial history, in reverse order. 
     * 
     * @return an iterator on the version numbers in reverse order 
     */
    public Iterator<Long> getDescendingVersionNumber() {
    	return Collections.unmodifiableSet(versions.descendingKeySet()).iterator();
    }
    
    public void addFileVersion(FileVersion fileVersion) {
        versions.put(fileVersion.getVersion(), fileVersion);        
    }
    
    @Override
    public PartialFileHistory clone() {
    	PartialFileHistory clone = new PartialFileHistory(fileId);
    	clone.versions.putAll(versions);

    	return clone;
    }

    @Override
    public String toString() {
    	return PartialFileHistory.class.getSimpleName()+"(fileId="+fileId+", versions="+versions+")";
    }
    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileId == null) ? 0 : fileId.hashCode());
		result = prime * result + ((versions == null) ? 0 : versions.hashCode());
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
		PartialFileHistory other = (PartialFileHistory) obj;
		if (fileId == null) {
			if (other.fileId != null)
				return false;
		} else if (!fileId.equals(other.fileId))
			return false;
		if (versions == null) {
			if (other.versions != null)
				return false;
		} else if (!versions.equals(other.versions))
			return false;
		return true;
	}
	
	/**
	 * @author Fabrice Rossi
	 */
	public static class FileHistoryId extends ObjectId {
		private FileHistoryId(byte[] array) {
			super(array);
		}
		
		public static FileHistoryId secureRandomFileId() {
			return new FileHistoryId(ObjectId.secureRandomBytes(FILE_HISTORY_ID_LENGTH));
		}
		
		public static FileHistoryId parseFileId(String s) {
			return new FileHistoryId(ObjectId.parseBytes(s));
		}
	}
}
