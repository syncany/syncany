package org.syncany.database;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.syncany.util.ByteArray;

public class DatabaseVersion {
    private DatabaseVersionHeader header; 
    
    // Full DB in RAM
    private Map<ByteArray, ChunkEntry> chunkCache;
    private Map<ByteArray, MultiChunkEntry> multiChunkCache;
    private Map<ByteArray, FileContent> contentCache;
    private Map<Long, PartialFileHistory> historyCache;
    
    public DatabaseVersion() {
    	header = new DatabaseVersionHeader();
    	
        chunkCache = new HashMap<ByteArray, ChunkEntry>();
        multiChunkCache = new HashMap<ByteArray, MultiChunkEntry>();
        contentCache = new HashMap<ByteArray, FileContent>();
        historyCache = new HashMap<Long, PartialFileHistory>();  
    }
    
	public DatabaseVersionHeader getHeader() {
		return header;
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
	
	public String getUploadedFrom() {
		return header.getClient();
	}

    // Chunk
    
    public ChunkEntry getChunk(byte[] checksum) {
        return chunkCache.get(new ByteArray(checksum));
    }    
    
    public void addChunk(ChunkEntry chunk) {
        chunkCache.put(new ByteArray(chunk.getChecksum()), chunk);        
    }
    
    public Collection<ChunkEntry> getChunks() {
        return chunkCache.values();
    }
    
    // Multichunk    
    
    public void addMultiChunk(MultiChunkEntry multiChunk) {
        multiChunkCache.put(new ByteArray(multiChunk.getId()), multiChunk);                
    }
    
    public MultiChunkEntry getMultiChunk(byte[] multiChunkId) {
    	return multiChunkCache.get(new ByteArray(multiChunkId));
    }
    
    public Collection<MultiChunkEntry> getMultiChunks() {
        return multiChunkCache.values();
    }

	
	// Content

	public FileContent getFileContent(byte[] checksum) {
		return contentCache.get(new ByteArray(checksum));
	}

	public void addFileContent(FileContent content) {
		contentCache.put(new ByteArray(content.getChecksum()), content);
	}

	public Collection<FileContent> getFileContents() {
		return contentCache.values();
	}
	
    // History
    
    public void addFileHistory(PartialFileHistory history) {
        historyCache.put(history.getFileId(), history);
    }
    
    public PartialFileHistory getFileHistory(long fileId) {
        return historyCache.get(fileId);
    }
        
    public Collection<PartialFileHistory> getFileHistories() {
        return historyCache.values();
    }  
    
    public void addFileVersionToHistory(long fileHistoryID, FileVersion fileVersion) {
    	historyCache.get(fileHistoryID).addFileVersion(fileVersion);
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
}
