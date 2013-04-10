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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Database {

	private DatabaseVersion fullDatabaseVersion;
    private DatabaseVersion newestDatabaseVersion;
    private TreeMap<Long, DatabaseVersion> allDatabaseVersions;
    
    private Map<String, FileHistoryPart> filenameHistoryCache;

    public Database() {
    	fullDatabaseVersion = new DatabaseVersion();    	
    	newestDatabaseVersion = null;
    	allDatabaseVersions = new TreeMap<Long, DatabaseVersion>();
    	
        filenameHistoryCache = new HashMap<String, FileHistoryPart>();
    }   
	
	public long getFirstLocalDatabaseVersion() {
		return allDatabaseVersions.firstKey();
	}
	
	public long getLastLocalDatabaseVersion() {
		return allDatabaseVersions.lastKey();
	}
	
	public DatabaseVersion getLastDatabaseVersion() {
		return allDatabaseVersions.lastEntry().getValue();
	}

	public DatabaseVersion getDatabaseVersion(long databaseVersion) {
		return allDatabaseVersions.get(databaseVersion);
	}
		
	public Map<Long, DatabaseVersion> getDatabaseVersions() {
		return Collections.unmodifiableMap(allDatabaseVersions);
	}

	public FileContent getContent(byte[] checksum) {
		return fullDatabaseVersion.getFileContent(checksum);
	}
	
	public ChunkEntry getChunk(byte[] checksum) {
		return fullDatabaseVersion.getChunk(checksum);
	}
	
	public FileHistoryPart getFileHistory(String filePath) {
		return filenameHistoryCache.get(filePath); 
	}
	
	public void addDatabaseVersion(DatabaseVersion dbv) {	
		// TODO This should figure out the last local version from the vector clock
		// TODO Should the local version be identified by an empty string in the vector clock?
		
		
		if (allDatabaseVersions.isEmpty()) {
			// Increment local version
			long newLocalDatabaseVersion = 1; 

			// Set vector clock of database version
			VectorClock newDatabaseVersion = new VectorClock();	
			newDatabaseVersion.setClock("", newLocalDatabaseVersion); // TODO "" represents local client
			
			dbv.setDatabaseVersion(newDatabaseVersion);
			
			// Add to map
			allDatabaseVersions.put(newLocalDatabaseVersion, dbv);
		}
		
		else {
			// Increment local version
			long newLocalDatabaseVersion = allDatabaseVersions.lastKey()+1;

			// Set vector clock of database version
			VectorClock newDatabaseVersion = getLastDatabaseVersion().getDatabaseVersion().clone();	
			newDatabaseVersion.setClock("", newLocalDatabaseVersion); // TODO "" represents local client
			
			dbv.setDatabaseVersion(newDatabaseVersion);
			
			// Add to map
			allDatabaseVersions.put(newLocalDatabaseVersion, dbv);
		}		
		
		// Merge full version / populate cache
		mergeDBVinDB(fullDatabaseVersion, dbv);
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
			if (targetDatabaseVersion.getMultiChunk(sourceMultiChunk.getChecksum()) == null) {
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
		for (FileHistoryPart sourceFileHistory : sourceDatabaseVersion.getFileHistories()) {
			FileHistoryPart targetFileHistory = targetDatabaseVersion.getFileHistory(sourceFileHistory.getFileId());
			
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
		for (FileHistoryPart cacheFileHistory : targetDatabaseVersion.getFileHistories()) {
			String fileName = cacheFileHistory.getLastVersion().getFullName();
			
			filenameHistoryCache.put(fileName, cacheFileHistory);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<versions>");
		for (DatabaseVersion dbv : allDatabaseVersions.values()) {
			sb.append("<version>");
			sb.append(dbv);
			sb.append("</version>");
		}
		sb.append("</version>");
		
		return sb.toString();
	}
	
	
}
