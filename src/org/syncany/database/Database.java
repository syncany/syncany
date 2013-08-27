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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.syncany.util.ByteArray;

public class Database {
    private List<DatabaseVersion> databaseVersions;    
	
    // Caches
    private DatabaseVersion fullDatabaseVersionCache;
    private Map<String, PartialFileHistory> filenameHistoryCache;
    private Map<VectorClock, DatabaseVersion> databaseVersionIdCache;
    private Map<ByteArray, List<PartialFileHistory>> contentChecksumFileHistoriesCache;

    public Database() {
    	databaseVersions = new ArrayList<DatabaseVersion>();    	
        
    	// Caches
    	fullDatabaseVersionCache = new DatabaseVersion();    	
    	filenameHistoryCache = new HashMap<String, PartialFileHistory>();
    	databaseVersionIdCache = new HashMap<VectorClock, DatabaseVersion>();
    	contentChecksumFileHistoriesCache = new HashMap<ByteArray, List<PartialFileHistory>>();
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
	

	public DatabaseVersion getDatabaseVersion(VectorClock vectorClock) {
		return databaseVersionIdCache.get(vectorClock);
	}	

	public FileContent getContent(byte[] checksum) {
		return fullDatabaseVersionCache.getFileContent(checksum);
	}
	
	public ChunkEntry getChunk(byte[] checksum) {
		return fullDatabaseVersionCache.getChunk(checksum);
	}
	
	public MultiChunkEntry getMultiChunk(byte[] id) {
		return fullDatabaseVersionCache.getMultiChunk(id);
	}	
	
	/**
     * Get a multichunk that this chunk is contained in.
     */
	public MultiChunkEntry getMultiChunk(ChunkEntry chunk) {
		return fullDatabaseVersionCache.getMultiChunk(chunk);
	}	
	
	public PartialFileHistory getFileHistory(String relativeFilePath) {
		return filenameHistoryCache.get(relativeFilePath); 
	}
	
	public Collection<PartialFileHistory> getFileHistories(byte[] fileContentChecksum) {
		return contentChecksumFileHistoriesCache.get(new ByteArray(fileContentChecksum));
	}	
	
	public PartialFileHistory getFileHistory(long fileId) {
		return fullDatabaseVersionCache.getFileHistory(fileId); 
	}	

	public Collection<PartialFileHistory> getFileHistories() {
		return fullDatabaseVersionCache.getFileHistories();		
	}
	
	public Branch getBranch() {
		Branch branch = new Branch();
		
		for (DatabaseVersion databaseVersion : databaseVersions) {
			branch.add(databaseVersion.getHeader());
		}
		
		return branch;
	}
	
	public void addDatabaseVersion(DatabaseVersion databaseVersion) {		
		databaseVersions.add(databaseVersion);
		
		// Populate caches, do not reorder, order important!!
		updateDatabaseVersionIdCache(databaseVersion);
		updateFullDatabaseVersionCache(databaseVersion);
		updateFilenameHistoryCache();
		updateContentChecksumCache(databaseVersion);
	} 	

	public void removeDatabaseVersion(DatabaseVersion databaseVersion) {
		databaseVersions.remove(databaseVersion);
		
		// Populate caches, do not reorder, order important!!
		updateFullDatabaseVersionCache();
		updateDatabaseVersionIdCache();
		updateFilenameHistoryCache();
		updateContentChecksumCache();
	}

	private void updateContentChecksumCache() {
		contentChecksumFileHistoriesCache.clear();
		
		for (DatabaseVersion databaseVersion : databaseVersions) {
			updateContentChecksumCache(databaseVersion);
		}
	}
	
	private void updateContentChecksumCache(DatabaseVersion databaseVersion) {
		for (PartialFileHistory fileHistory : databaseVersion.getFileHistories()) {
			byte[] lastVersionChecksum = fileHistory.getLastVersion().getChecksum();
			
			if (lastVersionChecksum != null) {
				ByteArray lastVersionChecksumByteArray = new ByteArray(lastVersionChecksum);
				List<PartialFileHistory> historiesWithVersionsWithSameChecksum = contentChecksumFileHistoriesCache.get(lastVersionChecksumByteArray);
				
				if (historiesWithVersionsWithSameChecksum == null) {
					historiesWithVersionsWithSameChecksum = new ArrayList<PartialFileHistory>();
					contentChecksumFileHistoriesCache.put(lastVersionChecksumByteArray, historiesWithVersionsWithSameChecksum);
				}
				
				historiesWithVersionsWithSameChecksum.add(fileHistory);
			}
		}
	}	
		
	private void updateFilenameHistoryCache() {
		// Cache all file paths + names to fileHistories
		// TODO [high] file a deleted, file b same path/name => chaos
		for (PartialFileHistory cacheFileHistory : fullDatabaseVersionCache.getFileHistories()) {
			String fileName = cacheFileHistory.getLastVersion().getFullName();
			
			filenameHistoryCache.put(fileName, cacheFileHistory);
		}
	}
	
	private void updateDatabaseVersionIdCache(DatabaseVersion newDatabaseVersion) {
		databaseVersionIdCache.put(newDatabaseVersion.getVectorClock(), newDatabaseVersion);
	}
	
	private void updateDatabaseVersionIdCache() {
		databaseVersionIdCache.clear();
		
		for (DatabaseVersion databaseVersion : databaseVersions) {
			updateDatabaseVersionIdCache(databaseVersion);
		}
	}
	
	private void updateFullDatabaseVersionCache() {
		fullDatabaseVersionCache = new DatabaseVersion();
		
		for (DatabaseVersion databaseVersion : databaseVersions) {
			updateFullDatabaseVersionCache(databaseVersion);
		}
	}
	
	private void updateFullDatabaseVersionCache(DatabaseVersion newDatabaseVersion) {
		// Chunks
		for (ChunkEntry sourceChunk : newDatabaseVersion.getChunks()) {
			if (fullDatabaseVersionCache.getChunk(sourceChunk.getChecksum()) == null) {
				fullDatabaseVersionCache.addChunk(sourceChunk);
			}
		}
		
		// Multichunks
		for (MultiChunkEntry sourceMultiChunk : newDatabaseVersion.getMultiChunks()) {
			if (fullDatabaseVersionCache.getMultiChunk(sourceMultiChunk.getId()) == null) {
				fullDatabaseVersionCache.addMultiChunk(sourceMultiChunk);
			}
		}
		
		// Contents
		for (FileContent sourceFileContent : newDatabaseVersion.getFileContents()) {
			if (fullDatabaseVersionCache.getFileContent(sourceFileContent.getChecksum()) == null) {
				fullDatabaseVersionCache.addFileContent(sourceFileContent);
			}
		}		
		
		// Histories
		for (PartialFileHistory sourceFileHistory : newDatabaseVersion.getFileHistories()) {
			PartialFileHistory targetFileHistory = fullDatabaseVersionCache.getFileHistory(sourceFileHistory.getFileId());
			
			if (targetFileHistory == null) {
				fullDatabaseVersionCache.addFileHistory((PartialFileHistory) sourceFileHistory.clone());
			}
			else {
				for (FileVersion sourceFileVersion : sourceFileHistory.getFileVersions().values()) {
					if (targetFileHistory.getFileVersion(sourceFileVersion.getVersion()) == null) {
						targetFileHistory.addFileVersion(sourceFileVersion);
					}
				}
			}
		}		
	}
	
}
