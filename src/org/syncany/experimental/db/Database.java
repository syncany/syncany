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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.syncany.Constants;
import org.syncany.experimental.db.dao.DatabaseDAO;
import org.syncany.util.ByteArray;
import org.syncany.util.StringUtil;

public class Database {
    private long localDatabaseVersion;
	
    private DatabaseVersion fullDatabaseVersion;
    private DatabaseVersion dirtyDatabaseVersion;
    private TreeMap<Long, DatabaseVersion> allDatabaseVersions;

    // Quick access
    private Map<String, FileHistory> filenameHistoryCache;    
    
    public Database() {
    	localDatabaseVersion = 0;

    	fullDatabaseVersion = new DatabaseVersion();    	
    	dirtyDatabaseVersion = null;
    	allDatabaseVersions = new TreeMap<Long, DatabaseVersion>();

        filenameHistoryCache = new HashMap<String, FileHistory>();
        
        incLocalDatabaseVersion();
    }   
    
    public long getLocalDatabaseVersion() {
		return localDatabaseVersion;
	}

	public DatabaseVersion incLocalDatabaseVersion() {
		localDatabaseVersion++;
		
		dirtyDatabaseVersion = new DatabaseVersion();
		allDatabaseVersions.put(localDatabaseVersion, dirtyDatabaseVersion);
		
		return dirtyDatabaseVersion;
	}
	
	public long getFirstLocalDatabaseVersion() {
		return allDatabaseVersions.firstKey();
	}
	
	public long getLastLocalDatabaseVersion() {
		return allDatabaseVersions.lastKey();
	}

	public DatabaseVersion getDatabaseVersion(long databaseVersion) {
		return allDatabaseVersions.get(databaseVersion);
	}
		
	public Map<Long, DatabaseVersion> getDatabaseVersions() {
		return Collections.unmodifiableMap(allDatabaseVersions);
	}

	// FIXME This does not work if fullDatabase is not full
	@Deprecated
	public FileContent getContent(byte[] checksum) {
		return fullDatabaseVersion.getFileContent(checksum);
	}
	
	// FIXME This does not work if fullDatabase is not full
	@Deprecated
	public ChunkEntry getChunk(byte[] checksum) {
		return fullDatabaseVersion.getChunk(checksum);
	}
	
	// FIXME Should we add the new database objects to fullDatabase?
	@Deprecated
	public void setDatabaseVersion(long localDatabaseVersion, DatabaseVersion databaseVersion) {
		allDatabaseVersions.put(localDatabaseVersion, databaseVersion);
		new DatabaseDAO().merge(fullDatabaseVersion, databaseVersion);
	} 
}
