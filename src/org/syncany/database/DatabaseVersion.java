package org.syncany.database;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.util.ByteArray;
import org.syncany.util.StringUtil;

public class DatabaseVersion {
    private static final Logger logger = Logger.getLogger(DatabaseVersion.class.getSimpleName());
    
    // DB Version and versions of other users (= DB basis) 
    private VectorClock vectorClock; // vector clock, machine name to database version map
    
    // Full DB in RAM
    private Map<ByteArray, ChunkEntry> chunkCache;
    private Map<ByteArray, MultiChunkEntry> multiChunkCache;
    private Map<ByteArray, FileContent> contentCache;
    private Map<Long, PartialFileHistory> historyCache;
    
    public DatabaseVersion() {
    	vectorClock = new VectorClock();
    	
        chunkCache = new HashMap<ByteArray, ChunkEntry>();
        multiChunkCache = new HashMap<ByteArray, MultiChunkEntry>();
        contentCache = new HashMap<ByteArray, FileContent>();
        historyCache = new HashMap<Long, PartialFileHistory>();  
    }

	public VectorClock getVectorClock() {
		return vectorClock;
	}

	public void setVectorClock(VectorClock vectorClock) {
		this.vectorClock = vectorClock;
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
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<chunks>");
		for (ChunkEntry chunk : chunkCache.values()) {
			sb.append("<chunk>");
			sb.append(StringUtil.toHex(chunk.getChecksum()));
			sb.append("</chunk>");
		}
		sb.append("</chunks>");
		
		sb.append("<multichunks>");
		for (MultiChunkEntry multiChunk : multiChunkCache.values()) {
			sb.append("<multichunk>");
			sb.append(StringUtil.toHex(multiChunk.getId()));
			sb.append("</multichunk>");
		}
		sb.append("</multichunks>");
		
		sb.append("<rest/>");

		return sb.toString();
	}    
}
