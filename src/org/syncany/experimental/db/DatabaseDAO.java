package org.syncany.experimental.db;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Len             Description
 * -------------------------------------------------------                             
 * // Preamble
 * 7 byte          'Syncany' 
 * 1 byte           0x01 (Database format version)
 *   
 * // Chunks
 * 1 int           Number of chunks (= n)  
 *   // If n > 0
 *     // For each chunk (n times)
 *     1 byte      Checksum length (= m)
 *     m byte      Checksum
 * 
 * // Multichunks
 * 1 int           Number of multichunks (= n)
 *   // If n > 0
 *     // For each multichunk (n times)
 *     1 byte      Checksum length (= m)
 *     m byte      Checksum
 *     1 short     Number of chunks in multichunk (= p)
 *     // If p > 0
 *       // For each chunk (p times)
 *       m byte    Chunk checksum
 * 
 * // Content
 * 1 int           Number of contents (= n)
 *   // If n > 0
 *     // For each content (n times)
 *     1 byte      Checksum length (= m)
 *     m byte      Checksum
 *     1 int       Content size
 *     1 short     Number of chunks in content (= p)
 *     // If p > 0
 *       // For each chunk (p times)
 *       m byte    Chunk checksum
 * 
 * // File Histories
 * 1 int           Number of file histories (= n)
 *   // If n > 0
 *     // For each file history (n times)
 *     1 long      File ID
 *     
 * // File Versions
 * 1 int           Number of file versions (= n)
 *   // If n > 0
 *     // For each file version (n times)
 *     1 long      File ID (of file history)
 *     1 long      File version (of this file)
 *     // If is empty or directory
 *       1 byte    0x00 (empty marker)
 *     // If is file and not empty
 *       1 byte    0x01 (non-empty-marker)
 *       1 byte    Content checksum size (= m)
 *       m byte    Content checksum
 *     // Endif
 *     1 short     Path length (= p)
 *     p byte      Path
 *     1 short     Name length (= q)
 *     q byte      Name 
 */  
public class DatabaseDAO {
	
	private static final byte DATABASE_FORMAT_VERSION = 0x01;


	//FIXME
    public synchronized void save(Database db, long versionFrom, long versionTo, File destinationFile) throws IOException {
        Collection l;
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(destinationFile));  
        
        // Signature and version        
        dos.write("Syncany".getBytes()); 
        dos.writeByte(DATABASE_FORMAT_VERSION);
        
		for (long i = versionFrom; i <= versionTo; i++) {
			

			// TODO write global database version (= vector clock)
			// TODO write local database version

			
			Set<ChunkEntry> newChunkCache = db.getVersionChunks().get(i);
			Set<MultiChunkEntry> newMultiChunkCache = db.getVersionMultiChunks().get(i);
			Set<FileContent> newContentCache = db.getVersionContents().get(i);
			Set<FileHistory> newHistoryCache = db.getVersionFileHistories().get(i);
			Set<FileVersion> newVersionCache = db.getVersionFileVersions().get(i);

			// Chunks
			l = newChunkCache;

			if (l == null || l.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(l.size()); // count

				for (Object obj : l) {
					((Persistable) obj).write(dos);
					// lastChunkId.setValue(((Chunk) obj).getId());
				}
			}

			// Metachunks
			l = newMultiChunkCache;

			if (l == null || l.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(l.size()); // count

				for (Object obj : l) {
					((Persistable) obj).write(dos);
					// lastChunkId.setValue(((Chunk) obj).getId());
				}
			}

			// Content
			l = newContentCache;

			if (l == null || l.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(l.size()); // count

				for (Object obj : l) {
					((Persistable) obj).write(dos);
					// lastChunkId.setValue(((Chunk) obj).getId());
				}
			}

			// File histories
			l = newHistoryCache;

			if (l == null || l.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(l.size()); // count

				for (Object obj : l) {
					// ((Persistable) obj).write(dos);
					FileHistory history = (FileHistory) obj;
					history.write(dos);
				}
			}

			// File versions

			// Count

			int versionCount = 0;

			for (FileVersion firstNewVersion : newVersionCache) { // TODO O(n^2)
																	// !!
				for (FileVersion version : firstNewVersion.getHistory()
						.getVersions()) {
					if (version.getVersion() >= firstNewVersion.getVersion()) {
						versionCount++;
					}
				}
			}

			// Write
			dos.writeInt(versionCount);

			for (FileVersion firstNewVersion : newVersionCache) { // TODO O(n^2)
				for (FileVersion version : firstNewVersion.getHistory()
						.getVersions()) {
					if (version.getVersion() >= firstNewVersion.getVersion()) {
						version.write(dos);
					}
				}
			}

		}

		dos.close();

	}
    
//    public synchronized void load(File file) throws IOException {
//    	load(file, true);
//    }
//    
//    public synchronized void load(File file, boolean gzip) throws IOException {
//        DataInputStream dis = (gzip)
//        	? new DataInputStream(new GZIPInputStream(new FileInputStream(file)))
//        	: new DataInputStream(new FileInputStream(file));
//        
//        // Signature and version
//        byte[] shouldFileSig = "Syncany".getBytes();
//        byte[] isFileSig = new byte[shouldFileSig.length];
//        dis.read(isFileSig);
//        
//        if (!Arrays.equals(shouldFileSig, isFileSig)) {
//            throw new IOException("Invalid file: not a Syncany file.");
//        }
//        
//        int version = dis.readByte();
//        
//        if ((version & 0xff) != DATABASE_FORMAT_VERSION) {
//            throw new IOException("Invalid file: version "+version+" not supported.");
//        }
//                
//        // Chunks
//        int chunkCount = dis.readInt();
//        
//        for (int i = 0; i < chunkCount; i++) {
//            ChunkEntry chunk = new ChunkEntry();
//            chunk.read(dis);
//            
//            //System.out.println("read chunk "+Arrays.toString(chunk.getChecksum()));
//            ByteArray key = new ByteArray(chunk.getChecksum());
//            if (chunkCache.get(key) == null) {
//                chunkCache.put(key, chunk);
//            }
//        }
//        
//        // Metachunks
//        int metaChunkCount = dis.readInt();
//
//        for (int i = 0; i < metaChunkCount; i++) {
//            MultiChunkEntry metaChunk = new MultiChunkEntry(this);
//            metaChunk.read(dis);
//
//            //System.out.println("read metachunk "+Arrays.toString(metaChunk.getChecksum()));
//            ByteArray key = new ByteArray(metaChunk.getChecksum());
//            if (!multiChunkCache.containsKey(key)) {
//                multiChunkCache.put(key, metaChunk);
//            }
//        }
//        
//        // Content
//        int contentCount = dis.readInt();
//
//        for (int i = 0; i < contentCount; i++) {
//            FileContent content = new FileContent(this);
//            content.read(dis);
//
//            //System.out.println("read content "+Arrays.toString(content.getChecksum()));
//            ByteArray key = new ByteArray(content.getChecksum());
//            if (!contentCache.containsKey(key)) {
//                contentCache.put(key, content);
//            }
//        }        
//        
//        // Histories
//        int historyCount = dis.readInt();
//
//        for (int i = 0; i < historyCount; i++) {
//            FileHistory fileHistory = new FileHistory();
//            fileHistory.read(dis);
//
//            //System.out.println("read history "+fileHistory.getFileId());
//            if (!historyCache.containsKey(fileHistory.getFileId())) {
//                historyCache.put(fileHistory.getFileId(), fileHistory);
//            }          
//        }                   
//        
//        // Versions
//        int versionCount = dis.readInt();
//        
//        for (int i = 0; i < versionCount; i++) {
//            FileVersion fileVersion = new FileVersion(this);
//            fileVersion.read(dis);
//
//            //System.out.println("read version "+fileVersion.getName());
//            // added by the read-method
//        }
//        
//        dis.close();
//    }
}