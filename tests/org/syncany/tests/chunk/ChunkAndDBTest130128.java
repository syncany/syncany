package org.syncany.tests.chunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.Chunker;
import org.syncany.chunk.CustomMultiChunker;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.MultiChunker;
import org.syncany.db.ChunkEntry;
import org.syncany.db.FileContent;
import org.syncany.db.FileHistoryPart;
import org.syncany.db.FileVersion;
import org.syncany.db.MultiChunkEntry;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class ChunkAndDBTest130128 {

	@Test
	public void saveLoadSaveTest() throws IOException {
		DatabaseOLD db;
		
		System.out.println("save1()");
		System.out.println("----------------------------");
		saveLoadSaveTestSave1();
		
		System.out.println("load1()");
		System.out.println("----------------------------");
		db = saveLoadSaveTestLoad1();
		
		System.out.println("save2()");
		System.out.println("----------------------------");
		saveLoadSaveTestSave2(db);
	}
	
	public DatabaseOLD saveLoadSaveTestSave1() throws IOException {
		DatabaseOLD db = new DatabaseOLD();
		
		// Create first file
        ChunkEntry chunkA1 = new ChunkEntry(new byte[] { 1,2,3,4,5,7,8,9,0}, 12);
        ChunkEntry chunkA2 = new ChunkEntry(new byte[] { 9,8,7,6,5,4,3,2,1}, 34);
        ChunkEntry chunkA3 = new ChunkEntry(new byte[] { 1,1,1,1,1,1,1,1,1}, 56);
        ChunkEntry chunkA4 = new ChunkEntry(new byte[] { 2,2,2,2,2,2,2,2,2}, 78);
        
        db.addChunk(chunkA1);
        db.addChunk(chunkA2);
        db.addChunk(chunkA3);
        db.addChunk(chunkA4);
		
        FileContent contentA = new FileContent();        
        contentA.addChunk(chunkA1);
        contentA.addChunk(chunkA2);
        contentA.addChunk(chunkA3);
        contentA.addChunk(chunkA4);
        contentA.setChecksum(new byte[]{5,5,5,4,4,5,5,5,5});              
        db.addFileContent(contentA);
        
        FileHistoryPart fileA = new FileHistoryPart();
        
        FileVersion versionA1 = db.createFileVersion(fileA);
        versionA1.setVersion(1L);
        versionA1.setPath("some/path");
        versionA1.setName("fileA.jpg");
        versionA1.setContent(contentA);
       // fileA.addVersion(versionA1);
        
        FileVersion versionA2 = db.createFileVersion(fileA);
        versionA2.setVersion(2L);
        versionA2.setName("renamed-fileA.jpg");        
        //fileA.addVersion(versionA2);
        
        db.addFileHistory(fileA);
        
        // Create second file
        ChunkEntry chunkB1 = new ChunkEntry(new byte[] { 3,3,3,3,3,3,3,3,3}, 910);
        ChunkEntry chunkB2 = new ChunkEntry(new byte[] { 4,4,4,4,4,4,4,4,4}, 1112);
        
        db.addChunk(chunkB1);
        db.addChunk(chunkB2);
        
        FileContent contentB = new FileContent();
        contentB.addChunk(chunkB1);
        contentB.addChunk(chunkB2);   
        contentB.setChecksum(new byte[]{1,1,1,3,3,5,5,5,5});                      
        db.addFileContent(contentB);

        FileHistoryPart fileB = new FileHistoryPart();
        
        FileVersion versionB1 = db.createFileVersion(fileB);
        versionB1.setVersion(1L);
        versionB1.setPath("some/other/path");
        versionB1.setName("fileB.jpg");
        versionB1.setContent(contentB);
        //fileB.addVersion(versionB1);
        
        FileVersion versionB2 = db.createFileVersion(fileB);
        versionB1.setVersion(2L);
        versionB2.setName("renamed-fileB.jpg");        
        //fileB.addVersion(versionB2);
        
        db.addFileHistory(fileB);
        
        // Distribute chunks to multichunks
        MultiChunkEntry multiChunkA = new MultiChunkEntry();
        multiChunkA.addChunk(chunkA1); // from fileA
        multiChunkA.addChunk(chunkA2); // from fileA
        multiChunkA.addChunk(chunkA3); // from fileA
        multiChunkA.setChecksum(new byte[] {6,6,6,6,6,6,6,6,6});
        db.addMultiChunk(multiChunkA);
        
        MultiChunkEntry multiChunkB = new MultiChunkEntry();
        multiChunkB.addChunk(chunkA4); // from fileA
        multiChunkB.addChunk(chunkB1); // from fileB
        multiChunkB.addChunk(chunkB2); // from fileB
        multiChunkB.setChecksum(new byte[] {7,7,7,7,7,7,7,7,7});
        db.addMultiChunk(multiChunkB);

        // Save database
        File dbFile = new File("/tmp/dbfile");
        db.save(dbFile, true, false);        
        
        System.out.println(db);
        return db;
	}
	
	public DatabaseOLD saveLoadSaveTestLoad1() throws IOException {
		DatabaseOLD db = new DatabaseOLD();
		
		// Load database
        File dbFile = new File("/tmp/dbfile");
		db.load(dbFile, false);

        System.out.println(db);
        
        return db;
	}
	
	public DatabaseOLD saveLoadSaveTestSave2(DatabaseOLD db) throws IOException {
		// Create first file
        ChunkEntry chunkC1 = new ChunkEntry(new byte[] { 99,92,93,4,5,7,8,9,0}, 912);
        ChunkEntry chunkC2 = new ChunkEntry(new byte[] { 99,98,97,6,5,4,3,2,1}, 934);
        
        db.addChunk(chunkC1);
        db.addChunk(chunkC2);
               
        FileContent contentC = new FileContent();        
        contentC.addChunk(chunkC1);
        contentC.addChunk(chunkC2);
        contentC.setChecksum(new byte[]{95,95,95,4,4,5,5,5,5});              
        db.addFileContent(contentC);
        
        FileHistoryPart fileC = new FileHistoryPart();
        
        FileVersion versionC1 = db.createFileVersion(fileC);
        versionC1.setVersion(1L);
        versionC1.setPath("some/path");
        versionC1.setName("fileC.jpg");
        versionC1.setContent(contentC);
                
        db.addFileHistory(fileC);
        
        // Distribute chunks to multichunks
        MultiChunkEntry multiChunkC = new MultiChunkEntry();
        multiChunkC.addChunk(chunkC1); // from fileC
        multiChunkC.addChunk(chunkC2); // from fileC
        multiChunkC.setChecksum(new byte[] {9,0,1,2,6,6,6,6,6});
        db.addMultiChunk(multiChunkC);        
        
        // Save new stuff
        File dbFile2 = new File("/tmp/dbfile2");
        db.save(dbFile2, false, false);

        System.out.println(db);
        return db;
	}
	
	@Test
	public void createRandomFileTreeChunkFilesAndReconstructTest() throws IOException {
		save3();
		load3();
	}
	
	public static void save3() throws IOException {
		// Create test environment
		File rootDir = new File("/tmp/syncany-db-test");
		File repoDir = new File("/tmp/syncany-db-test-repo");
		File localDatabaseFile = new File(repoDir+"/db-philipp-now");
		
		if (rootDir.exists()) {
			FileUtil.deleteRecursively(rootDir);
		}
		
		if (repoDir.exists()) {
			FileUtil.deleteRecursively(repoDir);
		}		
		
		rootDir.mkdir();
		repoDir.mkdir();
		
		// Make lots of random files and folders
		int maxFiles = 100;
		
		List<File> randomFiles = new ArrayList<File>();
		List<File> randomDirs = new ArrayList<File>();
		File currentDir = rootDir;
		
		for (int i=0; i<maxFiles; i++) {
			if (!randomDirs.isEmpty()) {
				currentDir = randomDirs.get((int) Math.random()*randomDirs.size());
			}

			if (Math.random() > 0.3) {
				File newFile = new File(currentDir+"/file"+i);
				FileUtil.createRandomFile(newFile, 1000, 500000);
				
				randomFiles.add(newFile);
			}
			else {
				currentDir = new File(currentDir+"/file"+i);
				currentDir.mkdir();
				
				randomDirs.add(currentDir);
				randomFiles.add(currentDir);
			}
		}
		
		// Now copy some files (1:1 copy), and slightly change some of them (1:0.9) 
		for(int i=maxFiles; i<maxFiles+maxFiles/4; i++) {
			File srcFile = randomFiles.get((int) (Math.random()*(double)randomFiles.size()));
			File destDir = randomDirs.get((int) (Math.random()*(double)randomDirs.size()));			
			
			if (srcFile.isDirectory()) {
				continue;
			}
			
			// Alter some of the copies (change some bytes)
			if (Math.random() > 0.5) {
				File destFile = new File(destDir+"/file"+i+"-almost-the-same-as-"+srcFile.getName());
				FileInputStream fis = new FileInputStream(srcFile);
				FileOutputStream fos = new FileOutputStream(destFile);
				
				byte[] buffer = new byte[4096];				

		        int len;
		        while ((len = fis.read(buffer)) > 0) {
		        	// Change x% of the file
		        	if (Math.random() < 0.5) {
		        		//buffer[0] = (byte) (0xff & (buffer[0]+1)); // CHANGE SOMETHING!
		        		//fos.write(buffer, 0, len);
		        		fos.write(buffer, 0, len-1);  // CHANGE SOMETHING!
		        		//fos.write(0xff); // CHANGE SOMETHING!s
//		        		fos.write(buffer, 0, len-1);  // CHANGE SOMETHING!
		        	}
		        	
		        	// Or just copy stuff
		        	else {
		        		fos.write(buffer, 0, len);
		        	}
		        }

		        fis.close();
		        fos.close();

				randomFiles.add(destFile);
			}
			
			// Or simply copy them
			else {
				File destFile = new File(destDir+"/file"+i+"-copy-of-"+srcFile.getName());
				FileUtil.copy(srcFile, destFile);
				
				randomFiles.add(destFile);
			}
		}
		
		
		// NOW START CHUNKING, AND ADDING EVERYTHING TO THE DB
		
		// DB: Create DB and chunk/multichunk entries
		DatabaseOLD db = new DatabaseOLD();		
		
		ChunkEntry chunkEntry = null;
		MultiChunkEntry multiChunkEntry = null;

		// Data: Create chunker and multichunker
		Chunker chunker = new FixedOffsetChunker(16*1024);
		MultiChunker multiChunker = new CustomMultiChunker(512*1024, 0);
		
		Chunk chunk = null;
		MultiChunk multiChunk = null;
		
		for (File file : randomFiles) {
			// Check if file exists, or create new 
			FileHistoryPart fileHistory = db.getFileHistory(FileUtil.getRelativePath(rootDir, file), file.getName());
			
			if (fileHistory == null) {
				fileHistory = new FileHistoryPart();
			}
			
			// Check for versions
			FileVersion fileVersion = fileHistory.getLastVersion();
			
			if (fileVersion == null) {
				fileVersion = db.createFileVersion(fileHistory);
			}
			
			fileVersion.setVersion(1L);
			fileVersion.setPath(FileUtil.getRelativePath(rootDir, file.getParentFile()));
			fileVersion.setName(file.getName());
			
			if (file.isFile()) {
				FileContent content = new FileContent();			
				
				// Create chunks from file
				Enumeration<Chunk> chunks = chunker.createChunks(file);
				
				while (chunks.hasMoreElements()) {
					chunk = chunks.nextElement();
					
					// Update DB
					chunkEntry = db.getChunk(chunk.getChecksum());
					
					if (chunkEntry == null) {
						chunkEntry = new ChunkEntry(chunk.getChecksum(), chunk.getSize());
						db.addChunk(chunkEntry);
						
						// Add chunk data to multichunk
						
						// - Check if multichunk full
						if (multiChunk != null && multiChunk.isFull()) {
							// Data
		                    multiChunk.close();
		                    multiChunk = null;
		                    
		                    // DB
		                    db.addMultiChunk(multiChunkEntry);
		                    multiChunkEntry = null;
		                }
						
						// - Open new multichunk if none existant
						if (multiChunk == null) {
							// Data
							File multiChunkFile = new File(repoDir+"/multichunk-"+StringUtil.toHex(chunk.getChecksum()));							
							multiChunk = multiChunker.createMultiChunk(chunk.getChecksum(), new FileOutputStream(multiChunkFile));
							
							// DB
							multiChunkEntry = new MultiChunkEntry();
							multiChunkEntry.setChecksum(chunk.getChecksum());
						}
						
						// - Add chunk data
						multiChunk.write(chunk);
						multiChunkEntry.addChunk(chunkEntry);
					}
					
					content.addChunk(chunkEntry);					
				}
				
				if (chunk != null) {
					content.setChecksum(chunk.getChecksum());
				}
				else {
					content.setChecksum(null);
				}
				
				fileVersion.setContent(content);
				db.addFileContent(content);
			}
			
			//fileHistory.addVersion(fileVersion);			
			db.addFileHistory(fileHistory);
		}
		
		// Close and add last multichunk
		if (multiChunk != null) {
			// Data
            multiChunk.close();
            multiChunk = null;
            
            // DB
            db.addMultiChunk(multiChunkEntry);
            multiChunkEntry = null;
        }		
		
		// Save database
		db.save(localDatabaseFile, true, false);
		
		System.out.println(db);
	}

	
	public static DatabaseOLD load3() throws IOException {
		// Create test environment
		File reconstructDir = new File("/tmp/syncany-db-test-reconstruct");
		File repoDir = new File("/tmp/syncany-db-test-repo");
		File cacheDir = new File("/tmp/syncany-db-test-cache");
		File localDatabaseFile = new File(repoDir+"/db-philipp-now");
						
		if (reconstructDir.exists()) {
			FileUtil.deleteRecursively(reconstructDir);
		}
		
		if (cacheDir.exists()) {
			FileUtil.deleteRecursively(cacheDir);
		}		

		cacheDir.mkdirs();
		reconstructDir.mkdirs();
		
		MultiChunker multiChunker = new CustomMultiChunker(512*1024, 0);
		
		// Load database
		DatabaseOLD db = new DatabaseOLD();
		db.load(localDatabaseFile, false);

        System.out.println(db);
        
        // Reconstruct all files from repo
        for (FileHistoryPart fileHistory : db.getFileHistories()) {
        	FileVersion lastFileVersion = fileHistory.getLastVersion();
        	File localFile = new File(reconstructDir+"/"+lastFileVersion.getPath()+"/"+lastFileVersion.getName());
        	
        	if (lastFileVersion.isFolder()) {
        		localFile.mkdirs();
        	}
        	
        	else {
        		// Create folders
        		localFile.getParentFile().mkdirs();
        		
        		// Reconstruct file
        		FileOutputStream fos = new FileOutputStream(localFile);
	        	List<ChunkEntry> chunkEntries = lastFileVersion.getContent().getChunks();
	        	
	        	for (ChunkEntry chunkEntry : chunkEntries) {        		
	        		File chunkFile = new File(cacheDir+"/chunk-"+StringUtil.toHex(chunkEntry.getChecksum()));
	        		boolean chunkWritten = false;
	        		
	        		// Extract multichunk if chunk file does not exist	        		
	        		if (!chunkFile.exists()) {
		        		MultiChunkEntry multiChunkEntry = chunkEntry.getMultiChunk();
	        			System.out.println(multiChunkEntry);
	        			System.out.println(StringUtil.toHex(multiChunkEntry.getChecksum()));
	        			File multiChunkFile = new File(repoDir+"/multichunk-"+StringUtil.toHex(multiChunkEntry.getChecksum()));
		        		
	        			MultiChunk multiChunk = multiChunker.createMultiChunk(new FileInputStream(multiChunkFile));
	        			Chunk chunk = null;
	        			
	        			while (null != (chunk = multiChunk.read())) {
	        				FileUtil.writeToFile(chunk.getContent(), new File(cacheDir+"/chunk-"+StringUtil.toHex(chunk.getChecksum())));
	        				
	        				if (Arrays.equals(chunk.getChecksum(), chunkEntry.getChecksum())) {
	        					fos.write(chunk.getContent());	
	        					chunkWritten = true;
	        				}
	        			}
	        		}
	        		else {
	        			byte[] chunkData = FileUtil.readFile(chunkFile);
	        			fos.write(chunkData);
	        			chunkWritten = true;
	        		}
	        		        		
	        		if (!chunkWritten) {
	        			throw new IOException("Chunk "+StringUtil.toHex(chunkEntry.getChecksum())+" not found.");
	        		}
	        	}
	        	
	        	fos.close();
        	}
        }
        
        return db;
	}
}
