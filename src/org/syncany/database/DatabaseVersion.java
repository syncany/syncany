package org.syncany.database;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.util.ByteArray;
import org.syncany.util.StringUtil;

public class DatabaseVersion {
    private static final Logger logger = Logger.getLogger(DatabaseVersion.class.getSimpleName());
    
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
	
	public void setUploadedFrom(String client) {
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
}
