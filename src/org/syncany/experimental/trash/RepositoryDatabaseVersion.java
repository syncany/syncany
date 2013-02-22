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
package org.syncany.experimental.trash;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.syncany.Constants;
import org.syncany.experimental.db.ChunkEntry;
import org.syncany.experimental.db.FileContent;
import org.syncany.experimental.db.FileHistory;
import org.syncany.experimental.db.FileVersion;
import org.syncany.experimental.db.MultiChunkEntry;
import org.syncany.util.ByteArray;



public class RepositoryDatabaseVersion {
	private long currentVersion;
	private Map<ByteArray, ChunkEntry> chunkEntries;
	private Map<ByteArray, MultiChunkEntry> multiChunkEntries;
	private Map<ByteArray, FileContent> contentCache;
	private Map<Long, FileHistory> fileHistories;

    public RepositoryDatabaseVersion() {
        chunkEntries = new HashMap<ByteArray, ChunkEntry>();
        multiChunkEntries = new HashMap<ByteArray, MultiChunkEntry>();
        contentCache = new HashMap<ByteArray, FileContent>();
        fileHistories = new HashMap<Long, FileHistory>();
    }

	public long getCurrentVersion() {
		return currentVersion;
	}
	protected void setCurrentVersion(long currentVersion) {
		this.currentVersion = currentVersion;
	}
	
// Chunk
    
    public ChunkEntry getChunk(byte[] checksum) {
        return chunkEntries.get(new ByteArray(checksum));
    }    
    
    public void addChunk(ChunkEntry chunk) {
        chunkEntries.put(new ByteArray(chunk.getChecksum()), chunk);
        newChunkCache.add(chunk);
    }
    
    public Collection<ChunkEntry> getChunks() {
        return chunkEntries.values();
    }
    
    // Multichunk    
    
    public void addMultiChunk(MultiChunkEntry multiChunk) {
        multiChunkEntries.put(new ByteArray(multiChunk.getChecksum()), multiChunk);                
        newMultiChunkCache.add(multiChunk);
    }
    
    public Collection<MultiChunkEntry> getMultiChunks() {
        return multiChunkEntries.values();
    }

    public Collection<MultiChunkEntry> getNewMultiChunks() {
        return newMultiChunkCache;
    }
    
    // History
    
    public void addFileHistory(FileHistory history) {
        fileHistories.put(history.getFileId(), history);
        newHistoryCache.add(history);        
    }
    
    public FileHistory getFileHistory(Long fileId) {
        return fileHistories.get(fileId);
    }
    
    public FileHistory getFileHistory(String relativePath, String name) {
        String relativeFilename = relativePath+Constants.DATABASE_FILE_SEPARATOR+name;        
        return filenameHistoryCache.get(relativeFilename);
    }        
    
    public Collection<FileHistory> getFileHistories() {
        return fileHistories.values();
    }
    
    // Version
    
    public FileVersion createFileVersion(FileHistory history) {
        FileVersion newVersion = history.createVersion();
        FileVersion firstNewVersion = newVersionCache.get(history.getFileId());
                
        if (firstNewVersion == null) {
            newVersionCache.put(history.getFileId(), newVersion);
        }

        //newHistoryCache.add(history); // history updated!
                
        // To file name based cache
        String relativeFilename = newVersion.getPath()+Constants.DATABASE_FILE_SEPARATOR+newVersion.getName();
        filenameHistoryCache.put(relativeFilename, history);
        
        return newVersion;
    }
        
    // Content
 
    public FileContent getContent(byte[] checksum) {
        return contentCache.get(new ByteArray(checksum));
    }

    public void addContent(FileContent content) {
        contentCache.put(new ByteArray(content.getChecksum()), content);
        newContentCache.add(content);
    }
}
