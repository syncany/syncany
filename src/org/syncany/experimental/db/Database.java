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

/**
 *1. Die DB hat keine Version die sie mitführt, das fehlt. Eine fortlaufende Nummer, die in jeden Speichervorgang mit reinsoll. Anhand der man dann den Dateinamen erzeugen kann
(12:23:42 AM) Steffen Dangmann: die db-dateinamen, jib
(12:24:53 AM) Philipp Heckel: 2. ich weiß nicht ob das Konzept newChunks, newMultiChunks, ... in der DB haltbar ist. Derzeit bleiben Chnks, Multichunks, etc. die per db.addXXX geadded werden in den newXXXX-Lists bis save() ausgeführt wird. In save() wird dann nur das zeug aus newXXX geschrieben oder die ganze DB. 
(12:25:00 AM) Steffen Dangmann: (btw. für alle serializables http://frequal.com/java/PracticalSerialVersionIdGuidelines.html , sollte man auch noch nachziehen)
(12:26:05 AM) Philipp Heckel: ich hatte noch was, das fällt mir aber grad nicht ein..
(12:26:33 AM) Steffen Dangmann: Punkt 2 ist doch nen issue von cache+persistence..
 * @author pheckel
 */
//FIXME fix it, introduce version. store out persistence-logic into DAO
@Deprecated
public class Database {
	private static final byte DATABASE_FORMAT_VERSION = 0x01;
    private static final Logger logger = Logger.getLogger(Database.class.getSimpleName());
    
    private Map<ByteArray, ChunkEntry> chunkCache;
    private Map<ByteArray, MultiChunkEntry> multiChunkCache;
    private Map<ByteArray, FileContent> contentCache;
    private Map<Long, FileHistory> historyCache;
    private Map<Long, FileVersion> versionCache;
    
    private Map<String, FileHistory> filenameHistoryCache;
    
    private Set<ChunkEntry> newChunkCache;
    private Set<MultiChunkEntry> newMultiChunkCache;
    private Set<FileContent> newContentCache;
    private Set<FileHistory> newHistoryCache;
    private Map<Long, FileVersion> newVersionCache;

    //XXXprivate Map<Long, FileVersion> databaseVersionTofileVersionMap;

    
    
    public Database() {
        chunkCache = new HashMap<ByteArray, ChunkEntry>();
        multiChunkCache = new HashMap<ByteArray, MultiChunkEntry>();
        contentCache = new HashMap<ByteArray, FileContent>();
        historyCache = new HashMap<Long, FileHistory>();
        versionCache = new HashMap<Long, FileVersion>();
        
        filenameHistoryCache = new HashMap<String, FileHistory>();

        newChunkCache = new HashSet<ChunkEntry>();
        newMultiChunkCache = new HashSet<MultiChunkEntry>();
        newContentCache = new HashSet<FileContent>();
        newHistoryCache = new HashSet<FileHistory>();
        newVersionCache = new HashMap<Long, FileVersion>();
    }

    public synchronized void save(File file) throws IOException {
        save(file, true);
    }
    
    public synchronized void save(File file, boolean full) throws IOException {
    	save(file, full, true);
    }
    
    public synchronized void save(File file, boolean full, boolean gzip) throws IOException {
        Collection l;
        DataOutputStream dos = (gzip) 
        	? new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))
        	: new DataOutputStream(new FileOutputStream(file));
        
        // Signature and version        
        dos.write("Syncany".getBytes()); 
        dos.writeByte(DATABASE_FORMAT_VERSION);
        
        // Chunks
        l = (full) ? chunkCache.values() : newChunkCache;
        
        if (l == null || l.isEmpty()) {                        
            dos.writeInt(0); // count
        }
        else {
            dos.writeInt(l.size()); // count
            
            for (Object obj : l) {
                ((Persistable) obj).write(dos);
                //lastChunkId.setValue(((Chunk) obj).getId());                     
            }            
        }
        
        // Metachunks        
        l = (full) ? multiChunkCache.values() : newMultiChunkCache;
        
        if (l == null || l.isEmpty()) {                        
            dos.writeInt(0); // count
        }
        else {
            dos.writeInt(l.size()); // count
            
            for (Object obj : l) {
                ((Persistable) obj).write(dos);
                //lastChunkId.setValue(((Chunk) obj).getId());                     
            }            
        }      
        
        // Content        
        l = (full) ? contentCache.values() : newContentCache;
        
        if (l == null || l.isEmpty()) {                        
            dos.writeInt(0); // count
        }
        else {
            dos.writeInt(l.size()); // count
            
            for (Object obj : l) {
                ((Persistable) obj).write(dos);
                //lastChunkId.setValue(((Chunk) obj).getId());                     
            }            
        }      
        
        // File histories
        l = (full) ? historyCache.values() : newHistoryCache;
        
        if (l == null || l.isEmpty()) {                        
            dos.writeInt(0); // count
        }
        else {
            dos.writeInt(l.size()); // count
            
            for (Object obj : l) {
                //((Persistable) obj).write(dos);
                FileHistory history = (FileHistory) obj;
                history.write(dos);
           }            
        }      

        // File versions
        if (full) {
            // Count
            int versionCount = 0;

            for (FileHistory history : historyCache.values()) {            
                versionCount += history.getVersions().size();
            }
            
            // Write
            dos.writeInt(versionCount);
            
            for (FileHistory history : historyCache.values()) {            
                for (FileVersion version : history.getVersions()) {
                    version.write(dos);
                }
            }
        }
        else {
            // Count
            int versionCount = 0;
            
            for (FileVersion firstNewVersion : newVersionCache.values()) { // TODO O(n^2) !!
                for (FileVersion version : firstNewVersion.getHistory().getVersions()) {
                    if (version.getVersion() >= firstNewVersion.getVersion()) {
                        versionCount++;
                    }
                }
            }
            
            // Write
            dos.writeInt(versionCount);
            
            for (FileVersion firstNewVersion : newVersionCache.values()) { // TODO O(n^2)
                for (FileVersion version : firstNewVersion.getHistory().getVersions()) {
                    if (version.getVersion() >= firstNewVersion.getVersion()) {
                        version.write(dos);
                    }                        
                }
            }
        }                        
        
        dos.close();     
        
        
        // Clear the 'new' entries
        newChunkCache.clear();
        newMultiChunkCache.clear();
        newContentCache.clear();
        newHistoryCache.clear();
        newVersionCache.clear();
    }
    
    public synchronized void load(File file) throws IOException {
    	load(file, true);
    }
    
    public synchronized void load(File file, boolean gzip) throws IOException {
        DataInputStream dis = (gzip)
        	? new DataInputStream(new GZIPInputStream(new FileInputStream(file)))
        	: new DataInputStream(new FileInputStream(file));
        
        // Signature and version
        byte[] shouldFileSig = "Syncany".getBytes();
        byte[] isFileSig = new byte[shouldFileSig.length];
        dis.read(isFileSig);
        
        if (!Arrays.equals(shouldFileSig, isFileSig)) {
            throw new IOException("Invalid file: not a Syncany file.");
        }
        
        int version = dis.readByte();
        
        if ((version & 0xff) != DATABASE_FORMAT_VERSION) {
            throw new IOException("Invalid file: version "+version+" not supported.");
        }
                
        // Chunks
        int chunkCount = dis.readInt();
        
        for (int i = 0; i < chunkCount; i++) {
            ChunkEntry chunk = new ChunkEntry();
            chunk.read(dis);
            
            //System.out.println("read chunk "+Arrays.toString(chunk.getChecksum()));
            ByteArray key = new ByteArray(chunk.getChecksum());
            if (chunkCache.get(key) == null) {
                chunkCache.put(key, chunk);
            }
        }
        
        // Metachunks
        int metaChunkCount = dis.readInt();

        for (int i = 0; i < metaChunkCount; i++) {
            MultiChunkEntry metaChunk = new MultiChunkEntry(this);
            metaChunk.read(dis);

            //System.out.println("read metachunk "+Arrays.toString(metaChunk.getChecksum()));
            ByteArray key = new ByteArray(metaChunk.getChecksum());
            if (!multiChunkCache.containsKey(key)) {
                multiChunkCache.put(key, metaChunk);
            }
        }
        
        // Content
        int contentCount = dis.readInt();

        for (int i = 0; i < contentCount; i++) {
            FileContent content = new FileContent(this);
            content.read(dis);

            //System.out.println("read content "+Arrays.toString(content.getChecksum()));
            ByteArray key = new ByteArray(content.getChecksum());
            if (!contentCache.containsKey(key)) {
                contentCache.put(key, content);
            }
        }        
        
        // Histories
        int historyCount = dis.readInt();

        for (int i = 0; i < historyCount; i++) {
            FileHistory fileHistory = new FileHistory();
            fileHistory.read(dis);

            //System.out.println("read history "+fileHistory.getFileId());
            if (!historyCache.containsKey(fileHistory.getFileId())) {
                historyCache.put(fileHistory.getFileId(), fileHistory);
            }          
        }                   
        
        // Versions
        int versionCount = dis.readInt();
        
        for (int i = 0; i < versionCount; i++) {
            FileVersion fileVersion = new FileVersion(this);
            fileVersion.read(dis);

            //System.out.println("read version "+fileVersion.getName());
            // added by the read-method
        }
        
        dis.close();
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
        newChunkCache.add(chunk);
    }
    
    public Collection<ChunkEntry> getChunks() {
        return chunkCache.values();
    }
    
    // Multichunk    
    
    public void addMultiChunk(MultiChunkEntry multiChunk) {
        multiChunkCache.put(new ByteArray(multiChunk.getChecksum()), multiChunk);                
        newMultiChunkCache.add(multiChunk);
    }
    
    public Collection<MultiChunkEntry> getMultiChunks() {
        return multiChunkCache.values();
    }

    public Collection<MultiChunkEntry> getNewMultiChunks() {
        return newMultiChunkCache;
    }
    
    // History
    
    public void addFileHistory(FileHistory history) {
        historyCache.put(history.getFileId(), history);
        newHistoryCache.add(history);        
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
