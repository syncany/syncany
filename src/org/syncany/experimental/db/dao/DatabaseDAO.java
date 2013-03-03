package org.syncany.experimental.db.dao;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.syncany.experimental.db.ChunkEntry;
import org.syncany.experimental.db.Database;
import org.syncany.experimental.db.DatabaseVersion;
import org.syncany.experimental.db.FileContent;
import org.syncany.experimental.db.FileHistory;
import org.syncany.experimental.db.FileVersion;
import org.syncany.experimental.db.MultiChunkEntry;

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

	
	public void save(Database db, File destinationFile) throws IOException {
		save(db, db.getFirstLocalDatabaseVersion(), db.getLastLocalDatabaseVersion(), destinationFile);
	}
	
	public void save(Database db, long versionFrom, long versionTo, File destinationFile) throws IOException {		
        Collection l;
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(destinationFile));  
		
        // DAOs
        ChunkDAO chunkDAO = new ChunkDAO();
        MultiChunkDAO multiChunkDAO = new MultiChunkDAO();
        FileHistoryDAO fileHistoryDAO = new FileHistoryDAO();
		FileContentDAO fileContentDAO = new FileContentDAO();
        
        // Signature and version        
        dos.write("Syncany".getBytes()); 
        dos.writeByte(DATABASE_FORMAT_VERSION);
        
        // Amount of versions
        dos.writeLong(versionTo-versionFrom);
        
        for (long i = versionFrom; i <= versionTo; i++) {
        	// Local version
			dos.writeLong(i);

			// Write object
			DatabaseVersion dbv = db.getDatabaseVersion(i);
        				
			// Global Version
			Map<String, Long> globalDatabaseVersion = dbv.getGlobalDatabaseVersion();
			
			dos.writeInt(globalDatabaseVersion.size());
			
			for (Map.Entry<String, Long> clientVersionEntry : globalDatabaseVersion.entrySet()) {
				String clientName = clientVersionEntry.getKey();
				Long clientVersion = clientVersionEntry.getValue();
				
				dos.writeByte(clientName.length());
				dos.writeBytes(clientName);
				dos.writeLong(clientVersion);
			}
			
			// Get the version
			Collection<ChunkEntry> newChunkCache = dbv.getChunks();
			Collection<MultiChunkEntry> newMultiChunkCache = dbv.getMultiChunks();
			Collection<FileContent> newContentCache = dbv.getFileContents();
			Collection<FileHistory> newHistoryCache = dbv.getFileHistories();

			// Chunks
			l = newChunkCache;

			if (l == null || l.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(l.size()); // count

				for (Object obj : l) {
					chunkDAO.writeChunk((ChunkEntry) obj, dos);
				}
			}

			// Multichunks
			l = newMultiChunkCache;

			if (l == null || l.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(l.size()); // count

				for (Object obj : l) {
					multiChunkDAO.writeMultiChunk((MultiChunkEntry) obj, dos);
				}
			}

			// Content
			l = newContentCache;

			if (l == null || l.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(l.size()); // count

				for (Object obj : l) {
					fileContentDAO.writeFileContent((FileContent) obj, dos);
				}
			}

			// File histories & versions
			l = newHistoryCache;

			if (l == null || l.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(l.size()); // count

				for (Object obj : l) {
					fileHistoryDAO.writeFileHistory((FileHistory) obj, dos);
				}
			}
		}

		dos.close();			
	}	
    
    public void load(Database db, File sourceFile) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile));
        
        // DAOs
        ChunkDAO chunkDAO = new ChunkDAO();
        MultiChunkDAO multiChunkDAO = new MultiChunkDAO();
        FileHistoryDAO fileHistoryDAO = new FileHistoryDAO();
        FileContentDAO fileContentDAO = new FileContentDAO();
        
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
          
        // Database version count
        long databaseVersionCount = dis.readLong();
        
        for (long i=0; i<databaseVersionCount; i++) {
        	// Local database version (= key)
        	long localDatabaseVersion = dis.readLong();

        	// Read obect (= value)
        	DatabaseVersion dbv = new DatabaseVersion();
        	
        	// Global database version
        	int globalDatabaseVersionClientCount = dis.readInt();
        	
        	for (int j=0; j<globalDatabaseVersionClientCount; j++) {
        		// Client name
        		int clientNameLength = dis.readByte();
        		byte[] clientNameBytes = new byte[clientNameLength];
        		dis.readFully(clientNameBytes);
        		String clientName = new String(clientNameBytes);
        		
        		// Client version
        		long clientVersion = dis.readLong();
        		
        		// Add
        		dbv.setGlobalDatabaseVersion(clientName, clientVersion);
        	}
        	
        	// Chunks
            int chunkCount = dis.readInt();
            
            for (int j = 0; j < chunkCount; j++) {
                ChunkEntry chunk = chunkDAO.readChunk(dis);                
                dbv.addChunk(chunk);
            }
            
            // Multichunks
            int metaChunkCount = dis.readInt();

            for (int j = 0; j < metaChunkCount; j++) {
                MultiChunkEntry multiChunk = multiChunkDAO.readMultiChunk(db, dis);
                dbv.addMultiChunk(multiChunk);                
            }
            
            // Content
            int fileContentCount = dis.readInt();

            for (int j = 0; j < fileContentCount; j++) {
                FileContent content = fileContentDAO.readFileContent(db, dis);
                dbv.addFileContent(content);
            }        
            
            // Histories
            int fileHistoryCount = dis.readInt();

            for (int j = 0; j < fileHistoryCount; j++) {
                FileHistory fileHistory = fileHistoryDAO.readFileHistory(db, dis);
                dbv.addFileHistory(fileHistory);
            }                 
        	
            // Add version to Database object
            db.setDatabaseVersion(localDatabaseVersion, dbv);
        }
                       
        dis.close();
    }
    
	public void merge(DatabaseVersion targetDatabaseVersion, DatabaseVersion sourceDatabaseVersion) {
		// Chunks
		for (ChunkEntry sourceChunk : sourceDatabaseVersion.getChunks()) {
			if (targetDatabaseVersion.getChunk(sourceChunk.getChecksum()) == null) {
				targetDatabaseVersion.addChunk(sourceChunk);
			}
		}
		
		// Multichunks
		for (MultiChunkEntry sourceMultiChunk : sourceDatabaseVersion.getMultiChunks()) {
			if (targetDatabaseVersion.getMultiChunk(sourceMultiChunk.getChecksum()) == null) {
				targetDatabaseVersion.addMultiChunk(sourceMultiChunk);
			}
		}
		
		// Contents
		for (FileContent sourceFileContent : sourceDatabaseVersion.getFileContents()) {
			if (targetDatabaseVersion.getFileContent(sourceFileContent.getChecksum()) == null) {
				targetDatabaseVersion.addFileContent(sourceFileContent);
			}
		}		
		
		// Histories
		for (FileHistory sourceFileHistory : sourceDatabaseVersion.getFileHistories()) {
			FileHistory targetFileHistory = targetDatabaseVersion.getFileHistory(sourceFileHistory.getFileId());
			
			if (targetFileHistory == null) {
				targetDatabaseVersion.addFileHistory(sourceFileHistory);
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