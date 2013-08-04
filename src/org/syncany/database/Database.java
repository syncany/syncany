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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Database {
    private List< DatabaseVersion> databaseVersions;    
	private DatabaseVersion cachedFullDatabaseVersion;
    private Map<String, PartialFileHistory> filenameHistoryCache;

    public Database() {
    	cachedFullDatabaseVersion = new DatabaseVersion();    	
    	databaseVersions = new ArrayList<DatabaseVersion>();    	
        filenameHistoryCache = new HashMap<String, PartialFileHistory>();
    }   	
	
	public DatabaseVersion getLastDatabaseVersion() {
		if (databaseVersions.size() == 0) {
			return null;
		}
		
		return databaseVersions.get(databaseVersions.size()-1);
	}
	
	public DatabaseVersion getFirstDatabaseVersion() {
		if (databaseVersions.size() == 0) {
			return null;
		}
		
		return databaseVersions.get(0);
	}
		
		
	public List<DatabaseVersion> getDatabaseVersions() {
		return Collections.unmodifiableList(databaseVersions);
	}

	public FileContent getContent(byte[] checksum) {
		return cachedFullDatabaseVersion.getFileContent(checksum);
	}
	
	public ChunkEntry getChunk(byte[] checksum) {
		return cachedFullDatabaseVersion.getChunk(checksum);
	}
	
	public MultiChunkEntry getMultiChunk(byte[] id) {
		return cachedFullDatabaseVersion.getMultiChunk(id);
	}	
	
	public PartialFileHistory getFileHistory(String filePath) {
		return filenameHistoryCache.get(filePath); 
	}
	
	public PartialFileHistory getFileHistory(long fileId) {
		return cachedFullDatabaseVersion.getFileHistory(fileId); 
	}
	
	public Branch getBranch() {
		Branch branch = new Branch();
		
		for (DatabaseVersion databaseVersion : databaseVersions) {
			branch.add(databaseVersion.getHeader());
		}
		
		return branch;
	}
	
	public void addDatabaseVersion(DatabaseVersion databaseVersion) {		
		// Add to map
		databaseVersions.add(databaseVersion);
		
		// Merge full version / populate cache
		mergeDBVinDB(cachedFullDatabaseVersion, databaseVersion);
	} 
	
	private void mergeDBVinDB(DatabaseVersion targetDatabaseVersion, DatabaseVersion sourceDatabaseVersion) {
		// Chunks
		for (ChunkEntry sourceChunk : sourceDatabaseVersion.getChunks()) {
			if (targetDatabaseVersion.getChunk(sourceChunk.getChecksum()) == null) {
				targetDatabaseVersion.addChunk(sourceChunk);
			}
		}
		
		// Multichunks
		for (MultiChunkEntry sourceMultiChunk : sourceDatabaseVersion.getMultiChunks()) {
			if (targetDatabaseVersion.getMultiChunk(sourceMultiChunk.getId()) == null) {
				targetDatabaseVersion.addMultiChunk(sourceMultiChunk);
			}
		}
		
		// Contents
		for (FileContent sourceFileContent : sourceDatabaseVersion.getFileContents()) {
			if (targetDatabaseVersion.getFileContent(sourceFileContent.getChecksum()) == null) {
				targetDatabaseVersion.addFileContent(sourceFileContent);
			}
		}		
		
		// Histories
		for (PartialFileHistory sourceFileHistory : sourceDatabaseVersion.getFileHistories()) {
			PartialFileHistory targetFileHistory = targetDatabaseVersion.getFileHistory(sourceFileHistory.getFileId());
			
			if (targetFileHistory == null) {
				targetDatabaseVersion.addFileHistory(sourceFileHistory);
			}
			else {
				for (FileVersion sourceFileVersion : sourceFileHistory.getFileVersions().values()) {
					if (targetFileHistory.getFileVersion(sourceFileVersion.getVersion()) == null) {
						targetFileHistory.addFileVersion(sourceFileVersion);
					}
				}
			}
		}
		
		// Cache all file paths + names to fileHistories
		// TODO file a deleted, file b same path/name => chaos
		for (PartialFileHistory cacheFileHistory : targetDatabaseVersion.getFileHistories()) {
			String fileName = cacheFileHistory.getLastVersion().getFullName();
			
			filenameHistoryCache.put(fileName, cacheFileHistory);
		}
	}
	
}
