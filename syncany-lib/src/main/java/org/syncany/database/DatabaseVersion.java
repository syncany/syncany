/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory.FileHistoryId;

/**
 * The database version represents an incremental addition to the local database of 
 * a client. A user's {@link MemoryDatabase} consists of many incremental database versions.
 * 
 * <p>A <tt>DatabaseVersion</tt> is identified by a {@link DatabaseVersionHeader}, a 
 * combination of a {@link VectorClock}, a local timestamp and the original client name.
 * 
 * <p>The database version holds references to the newly added/removed/changed 
 * {@link PartialFileHistory}s as well as the corresponding {@link FileContent}s,
 * {@link ChunkEntry}s and {@link MultiChunkEntry}s.
 * 
 * <p>The current implementation of the database version keeps all references in memory. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DatabaseVersion {
	private DatabaseVersionStatus status;
    private DatabaseVersionHeader header; 
    
    public enum DatabaseVersionStatus {
    	MASTER, DIRTY
    }
    
    // Full DB in RAM
    private Map<ChunkChecksum, ChunkEntry> chunks;
    private Map<MultiChunkId, MultiChunkEntry> multiChunks;
    private Map<FileChecksum, FileContent> fileContents;
    private Map<FileHistoryId, PartialFileHistory> fileHistories;

    // Quick access cache
    private Map<ChunkChecksum, MultiChunkId> chunkMultiChunkCache;    

    public DatabaseVersion() {
    	header = new DatabaseVersionHeader();

        // Full DB in RAM
        chunks = new HashMap<ChunkChecksum, ChunkEntry>();
        multiChunks = new HashMap<MultiChunkId, MultiChunkEntry>();
        fileContents = new HashMap<FileChecksum, FileContent>();
        fileHistories = new HashMap<FileHistoryId, PartialFileHistory>();          

        // Quick access cache
        chunkMultiChunkCache = new HashMap<ChunkChecksum, MultiChunkId>();
    }
    
	public DatabaseVersionHeader getHeader() {
		return header;
	}
	
	public void setHeader(DatabaseVersionHeader header) {
		this.header = header;
	}

	public Date getTimestamp() {
		return header.getDate();
	}

	public void setTimestamp(Date timestamp) {
		this.header.setDate(timestamp);
	}    
	
	public VectorClock getVectorClock() {
		return header.getVectorClock();
	}

	public void setVectorClock(VectorClock vectorClock) {
		this.header.setVectorClock(vectorClock);
	}
	
	public void setClient(String client) {
		this.header.setClient(client);
	}
	
	public String getClient() {
		return header.getClient();
	}
	
	public DatabaseVersionStatus getStatus() {
		return status;
	}

	public void setStatus(DatabaseVersionStatus status) {
		this.status = status;
	}	

    // Chunk
	
	public ChunkEntry getChunk(ChunkChecksum checksum) {
        return chunks.get(checksum);
    }    
    
    public void addChunk(ChunkEntry chunk) {
        chunks.put(chunk.getChecksum(), chunk);        
    }
    
    public Collection<ChunkEntry> getChunks() {
        return chunks.values();
    }
    
    // Multichunk    
    
    public void addMultiChunk(MultiChunkEntry multiChunk) {
        multiChunks.put(multiChunk.getId(), multiChunk);
        
        // Populate cache
        for (ChunkChecksum chunkChecksum : multiChunk.getChunks()) {
        	chunkMultiChunkCache.put(chunkChecksum, multiChunk.getId());
        }
    }
    
    public MultiChunkEntry getMultiChunk(MultiChunkId multiChunkId) {
    	return multiChunks.get(multiChunkId);
    }
    
    /**
     * Get a multichunk that this chunk is contained in.
     */
    public MultiChunkId getMultiChunkId(ChunkChecksum chunk) {
    	return chunkMultiChunkCache.get(chunk);
    }
    
    /**
     * Get all multichunks in this database version.
     */
    public Collection<MultiChunkEntry> getMultiChunks() {
        return multiChunks.values();
    }
	
	// Content

	public FileContent getFileContent(FileChecksum checksum) {
		return fileContents.get(checksum);
	}

	public void addFileContent(FileContent content) {
		fileContents.put(content.getChecksum(), content);
	}

	public Collection<FileContent> getFileContents() {
		return fileContents.values();
	}
	
    // History
    
    public void addFileHistory(PartialFileHistory history) {
        fileHistories.put(history.getFileHistoryId(), history);
    }
    
    public PartialFileHistory getFileHistory(FileHistoryId fileId) {
        return fileHistories.get(fileId);
    }
        
    public Collection<PartialFileHistory> getFileHistories() {
        return fileHistories.values();
    }    
    
    @Override
    public DatabaseVersion clone() {
    	DatabaseVersion clonedDatabaseVersion = new DatabaseVersion();
    	clonedDatabaseVersion.setHeader(getHeader());
		
		for (ChunkEntry chunkEntry : getChunks()) {
			clonedDatabaseVersion.addChunk(chunkEntry);
		}
		
		for (MultiChunkEntry multiChunkEntry : getMultiChunks()) {
			clonedDatabaseVersion.addMultiChunk(multiChunkEntry);
		}
		
		for (FileContent fileContent : getFileContents()) {
			clonedDatabaseVersion.addFileContent(fileContent);
		}
		
		for (PartialFileHistory fileHistory : getFileHistories()) {
			clonedDatabaseVersion.addFileHistory(fileHistory);
		}		
		
		return clonedDatabaseVersion;
    }
    
    @Override
  	public int hashCode() {
  		final int prime = 31;
  		int result = 1;
  		result = prime * result + ((header == null) ? 0 : header.hashCode());
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
  		DatabaseVersion other = (DatabaseVersion) obj;
  		if (header == null) {
  			if (other.header != null) 
  				return false;
  		} else if (!header.equals(other.header))
  			return false;
  		return true;
  	}

	@Override
	public String toString() {
		return "DatabaseVersion [header=" + header + ", chunks=" + chunks.size() + ", multiChunks=" + multiChunks.size() + ", fileContents=" + fileContents.size()
				+ ", fileHistories=" + fileHistories.size() + "]";
	}    
}
