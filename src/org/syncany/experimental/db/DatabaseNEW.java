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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.syncany.Constants;
import org.syncany.util.ByteArray;
import org.syncany.util.StringUtil;

//FIXME fix it, introduce version. store out persistence-logic into DAO
public class DatabaseNEW {
	private static final byte DATABASE_FORMAT_VERSION = 0x01;
    private static final Logger logger = Logger.getLogger(Database.class.getSimpleName());
    
    private long currentDatabaseVersion;
    
    // Full DB in RAM
    private Map<ByteArray, ChunkEntry> chunkCache;
    private Map<ByteArray, MultiChunkEntry> multiChunkCache;
    private Map<ByteArray, FileContent> contentCache;
    private Map<Long, FileHistory> historyCache;
    private Map<Long, FileVersion> versionCache;
    
    // DB Version to X
    private Map<Long, Set<ChunkEntry>> versionChunks;
    private Map<Long, Set<MultiChunkEntry>> versionMultiChunks;
    private Map<Long, Set<FileContent>> versionContents;
    private Map<Long, Set<FileHistory>> versionFileHistories;
    private Map<Long, Set<FileVersion>> versionFileVersions;
    
    private Map<String, Long> vectorClockMachineNameToDatabaseVersion;

    // Quick access
    private Map<String, FileHistory> filenameHistoryCache;
    
    
    public DatabaseNEW() {
    	currentDatabaseVersion = 0;
    	
        chunkCache = new HashMap<ByteArray, ChunkEntry>();
        multiChunkCache = new HashMap<ByteArray, MultiChunkEntry>();
        contentCache = new HashMap<ByteArray, FileContent>();
        historyCache = new HashMap<Long, FileHistory>();
        versionCache = new HashMap<Long, FileVersion>();
        
        versionChunks = new HashMap<Long, Set<ChunkEntry>>();
        versionMultiChunks = new HashMap<Long, Set<MultiChunkEntry>>();
        versionContents = new HashMap<Long, Set<FileContent>>();
        versionFileHistories = new HashMap<Long, Set<FileHistory>>();
        versionFileVersions = new HashMap<Long, Set<FileVersion>>();

        filenameHistoryCache = new HashMap<String, FileHistory>();
    }

    public long getCurrentDatabaseVersion() {
		return currentDatabaseVersion;
	}

	public void setCurrentDatabaseVersion(long currentDatabaseVersion) {
		this.currentDatabaseVersion = currentDatabaseVersion;
		
		versionChunks.put(currentDatabaseVersion, new HashSet<ChunkEntry>());
		versionMultiChunks.put(currentDatabaseVersion, new HashSet<MultiChunkEntry>());
		versionContents.put(currentDatabaseVersion, new HashSet<FileContent>());
		versionFileHistories.put(currentDatabaseVersion, new HashSet<FileHistory>());
		versionFileVersions.put(currentDatabaseVersion, new HashSet<FileVersion>());
	}
	
	public Map<Long, FileVersion> getVersionCache() {
		return versionCache;
	}

	public Map<Long, Set<ChunkEntry>> getVersionChunks() {
		return versionChunks;
	}

	public Map<Long, Set<MultiChunkEntry>> getVersionMultiChunks() {
		return versionMultiChunks;
	}

	public Map<Long, Set<FileContent>> getVersionContents() {
		return versionContents;
	}

	public Map<Long, Set<FileHistory>> getVersionFileHistories() {
		return versionFileHistories;
	}

	public Map<Long, Set<FileVersion>> getVersionFileVersions() {
		return versionFileVersions;
	}

	public static String toDatabasePath(String filesystemPath) {
        return convertPath(filesystemPath, File.separator, Constants.DATABASE_FILE_SEPARATOR);
    }
    
    public static String toFilesystemPath(String databasePath) {
        return convertPath(databasePath, Constants.DATABASE_FILE_SEPARATOR, File.separator);
    }    
    
    private static String convertPath(String fromPath, String fromSep, String toSep) {
        String toPath = fromPath.replace(fromSep, toSep);
        
        // Trim (only at the end!)
        while (toPath.endsWith(toSep)) {
            toPath = toPath.substring(0, toPath.length()-toSep.length());
        }        
        
        return toPath;
    }

   
   

    // Chunk
    
    public ChunkEntry getChunk(byte[] checksum) {
        return chunkCache.get(new ByteArray(checksum));
    }    
    
    public void addChunk(ChunkEntry chunk) {
        chunkCache.put(new ByteArray(chunk.getChecksum()), chunk);        
        versionChunks.get(currentDatabaseVersion).add(chunk);        
    }
    
    public Collection<ChunkEntry> getChunks() {
        return chunkCache.values();
    }
    
    // Multichunk    
    
    public void addMultiChunk(MultiChunkEntry multiChunk) {
        multiChunkCache.put(new ByteArray(multiChunk.getChecksum()), multiChunk);                
        versionMultiChunks.get(currentDatabaseVersion).add(multiChunk);        
    }
    
    public Collection<MultiChunkEntry> getMultiChunks() {
        return multiChunkCache.values();
    }
    
    // History
    
    public void addFileHistory(FileHistory history) {
        historyCache.put(history.getFileId(), history);
        versionFileHistories.get(currentDatabaseVersion).add(history);
    }
    
    public FileHistory getFileHistory(Long fileId) {
        return historyCache.get(fileId);
    }
    
    public FileHistory getFileHistory(String relativePath, String name) {
        String relativeFilename = relativePath+Constants.DATABASE_FILE_SEPARATOR+name;        
        return filenameHistoryCache.get(relativeFilename);
    }        
    
    public Collection<FileHistory> getFileHistories() {
        return historyCache.values();
    }
    
    // Version
    
    public FileVersion createFileVersion(FileHistory history) {
        FileVersion newVersion = history.createVersion();
        FileVersion firstNewVersion = versionFileVersions.get(history.getFileId());
                
        if (firstNewVersion == null) {
            versionFileVersions.put(history.getFileId(), newVersion);
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
        versionContents.get(currentDatabaseVersion).add(content);
    }
    
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	
    	sb.append("chunkCache:\n");    	
    	for (ChunkEntry e : chunkCache.values()) {
    		sb.append("- Chunk "+StringUtil.toHex(e.getChecksum())+"\n");
    	}
    	
    	sb.append("multiChunkCache:\n");
    	for (MultiChunkEntry e : multiChunkCache.values()) {
    		sb.append("- MultiChunk "+StringUtil.toHex(e.getChecksum())+": \n");
    		
        	for (ChunkEntry e2 : e.getChunks()) {
        		sb.append("  + Chunk "+StringUtil.toHex(e2.getChecksum())+" (in MultiChunk "+StringUtil.toHex(e2.getMultiChunk().getChecksum())+")\n");
        	}
    	}    	
    	
    	sb.append("contentCache:\n");
    	for (FileContent e : contentCache.values()) {
    		sb.append("- Content "+StringUtil.toHex(e.getChecksum())+"\n");
    		
        	for (ChunkEntry e2 : e.getChunks()) {
        		sb.append("  + Chunk "+StringUtil.toHex(e2.getChecksum())+" (in MultiChunk "+StringUtil.toHex(e2.getMultiChunk().getChecksum())+")\n");
        	}    		
    	}   
    	
    	sb.append("historyCache:\n");
    	for (FileHistory e : historyCache.values()) {
    		sb.append("- FileHistory "+e.getFileId()+"\n");
    		
        	for (FileVersion e2 : e.getVersions()) {
        		sb.append("  + FileVersion "+e2.getVersion()
        				+", isFolder "+((e2.isFolder()) ? "yes" : "no")
        				+", Content "+((e2.getContent() == null) ? "(none)" : StringUtil.toHex(e2.getContent().getChecksum()))
        				+", "+e2.getPath()+"/"+e2.getName()+"\n");        		
        	}            	
    	}    	    	
    	
    	return sb.toString();
    }

}
