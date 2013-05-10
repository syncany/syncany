package org.syncany.db;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;


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
	private static final byte[] DATABASE_MAGIC = "Syncany".getBytes();
	private static final byte DATABASE_FORMAT_VERSION = 0x01;

	
	public void save(Database db, File destinationFile) throws IOException {
		save(db, db.getFirstLocalDatabaseVersion(), db.getLastLocalDatabaseVersion(), destinationFile);
	}
	
	public void save(Database db, long versionFrom, long versionTo, File destinationFile) throws IOException {		
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(destinationFile));  
		
        // DAOs
        ChunkDAO chunkDAO = new ChunkDAO();
        MultiChunkDAO multiChunkDAO = new MultiChunkDAO();
        FileHistoryDAO fileHistoryDAO = new FileHistoryDAO();
		FileContentDAO fileContentDAO = new FileContentDAO();
        
        // Signature and version        
        dos.write(DATABASE_MAGIC); 
        dos.writeByte(DATABASE_FORMAT_VERSION);
        
        // Amount of versions
        long databaseVersionCount = versionTo-versionFrom+1; 
        dos.writeLong(databaseVersionCount);
        
        for (long i = versionFrom; i <= versionTo; i++) {
        	// Local version
        	long localDatabaseVersion = i;
			dos.writeLong(localDatabaseVersion);

			// Write object
			DatabaseVersion dbv = db.getDatabaseVersion(i);
        				
			// Global Version
			Map<String, Long> globalDatabaseVersion = dbv.getVectorClock();
			
			dos.writeInt(globalDatabaseVersion.size());
			
			for (Map.Entry<String, Long> clientVersionEntry : globalDatabaseVersion.entrySet()) {
				String clientName = clientVersionEntry.getKey();
				Long clientVersion = clientVersionEntry.getValue();
				
				dos.writeByte(clientName.length());
				dos.writeBytes(clientName);
				dos.writeLong(clientVersion);
			}
			
			// Chunks
			Collection<ChunkEntry> chunks = dbv.getChunks();

			if (chunks == null || chunks.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(chunks.size()); // count

				for (ChunkEntry chunk : chunks) {
					chunkDAO.writeChunk(chunk, dos);
				}
			}

			// Multichunks
			Collection<MultiChunkEntry> multiChunks = dbv.getMultiChunks();

			if (multiChunks == null || multiChunks.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(multiChunks.size()); // count

				for (MultiChunkEntry multiChunk : multiChunks) {
					multiChunkDAO.writeMultiChunk(multiChunk, dos);
				}
			}

			// Content
			Collection<FileContent> fileContents = dbv.getFileContents();

			if (fileContents == null || fileContents.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(fileContents.size()); // count

				for (FileContent fileContent : fileContents) {
					fileContentDAO.writeFileContent(fileContent, dos);
				}
			}

			// File histories & versions
			Collection<FileHistoryPart> fileHistories = dbv.getFileHistories();

			if (fileHistories == null || fileHistories.isEmpty()) {
				dos.writeInt(0); // count
			} else {
				dos.writeInt(fileHistories.size()); // count

				for (FileHistoryPart fileHistory : fileHistories) {
					fileHistoryDAO.writeFileHistory(fileHistory, dos);
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
        	// TODO do something with this!

        	// Read object (= value)
        	DatabaseVersion dbv = new DatabaseVersion();
        	
        	// Global database version
        	VectorClock globalDatabaseVersionClock = new VectorClock(); 
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
        		globalDatabaseVersionClock.setClock(clientName, clientVersion);
        	}
        	
        	dbv.setVectorClock(globalDatabaseVersionClock);
        	
        	// Chunks
            int chunkCount = dis.readInt();
            
            for (int j = 0; j < chunkCount; j++) {
                ChunkEntry chunk = chunkDAO.readChunk(dis);                
                dbv.addChunk(chunk);
            }
            
            // Multichunks
            int metaChunkCount = dis.readInt();

            for (int j = 0; j < metaChunkCount; j++) {
                MultiChunkEntry multiChunk = multiChunkDAO.readMultiChunk(db, dbv, dis);
                dbv.addMultiChunk(multiChunk);                
            }
            
            // Content
            int fileContentCount = dis.readInt();

            for (int j = 0; j < fileContentCount; j++) {
                FileContent content = fileContentDAO.readFileContent(db, dbv, dis);
                dbv.addFileContent(content);
            }        
            
            // Histories
            int fileHistoryCount = dis.readInt();

            for (int j = 0; j < fileHistoryCount; j++) {
                FileHistoryPart fileHistory = fileHistoryDAO.readFileHistory(db, dbv, dis);
                dbv.addFileHistory(fileHistory);
            }                 
        	
            // Add version to Database object
            db.addDatabaseVersion(dbv);
        }
                       
        dis.close();
    }
    
}