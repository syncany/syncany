package org.syncany.experimental.db;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.syncany.Constants;
import org.syncany.util.ByteArray;

public class DatabaseVersion {
    private static final Logger logger = Logger.getLogger(Database.class.getSimpleName());
    
    // DB Version and versions of other users (= DB basis) 
    protected Map<String, Long> globalDatabaseVersion; // vector clock, machine name to database version map
    
    // Full DB in RAM
    protected Map<ByteArray, ChunkEntry> chunkCache;
    protected Map<ByteArray, MultiChunkEntry> multiChunkCache;
    protected Map<ByteArray, FileContent> contentCache;
    protected Map<Long, FileHistory> historyCache;
    
    public DatabaseVersion() {
    	globalDatabaseVersion = new HashMap<String, Long>();
    	
        chunkCache = new HashMap<ByteArray, ChunkEntry>();
        multiChunkCache = new HashMap<ByteArray, MultiChunkEntry>();
        contentCache = new HashMap<ByteArray, FileContent>();
        historyCache = new HashMap<Long, FileHistory>();  
    }

	public Map<String, Long> getGlobalDatabaseVersion() {
		return Collections.unmodifiableMap(globalDatabaseVersion);
	}

	public void setGlobalDatabaseVersion(String machineName, long machineVersion) {
		globalDatabaseVersion.put(machineName, machineVersion);
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
        multiChunkCache.put(new ByteArray(multiChunk.getChecksum()), multiChunk);                
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
    
    public void addFileHistory(FileHistory history) {
        historyCache.put(history.getFileId(), history);
    }
    
    public FileHistory getFileHistory(Long fileId) {
        return historyCache.get(fileId);
    }
        
    public Collection<FileHistory> getFileHistories() {
        return historyCache.values();
    }  
  
        
	

}
