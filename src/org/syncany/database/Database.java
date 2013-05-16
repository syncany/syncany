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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Database {
	private DatabaseVersion fullDatabaseVersion;
    private TreeMap<Long, DatabaseVersion> allDatabaseVersions;    
    private Map<String, PartialFileHistory> filenameHistoryCache;

    public Database() {
    	fullDatabaseVersion = new DatabaseVersion();    	
    	allDatabaseVersions = new TreeMap<Long, DatabaseVersion>();    	
        filenameHistoryCache = new HashMap<String, PartialFileHistory>();
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
	
	public MultiChunkEntry getMultiChunk(byte[] id) {
		return fullDatabaseVersion.getMultiChunk(id);
	}	
	
	public PartialFileHistory getFileHistory(String filePath) {
		return filenameHistoryCache.get(filePath); 
	}
	
	public PartialFileHistory getFileHistory(long fileId) {
		return fullDatabaseVersion.getFileHistory(fileId); 
	}
	
	public void addDatabaseVersion(DatabaseVersion dbv) {	
		// TODO This should figure out the last local version from the vector clock
		// TODO Should the local version be identified by an empty string in the vector clock?
		long newLocalDatabaseVersion;
		VectorClock newDatabaseVersion = null;	

		if (allDatabaseVersions.isEmpty()) {
			// Increment local version
			newLocalDatabaseVersion = 1; 
			
			newDatabaseVersion = new VectorClock();
			// Set vector clock of database version
			newDatabaseVersion.setClock("", newLocalDatabaseVersion); // TODO "" represents local client
		}
		
		else {
			// Increment local version
			newLocalDatabaseVersion = allDatabaseVersions.lastKey()+1;

			// Set vector clock of database version
			newDatabaseVersion = getLastDatabaseVersion().getVectorClock().clone();	
			newDatabaseVersion.setClock("", newLocalDatabaseVersion); // TODO "" represents local client
		}		
		
		dbv.setVectorClock(newDatabaseVersion);
		
		// Add to map
		allDatabaseVersions.put(newLocalDatabaseVersion, dbv);
		
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
