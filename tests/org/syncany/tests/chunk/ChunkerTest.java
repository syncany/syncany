package org.syncany.tests.chunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.Constants;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.CustomMultiChunk;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.TTTDChunker;
import org.syncany.config.Profile;
import org.syncany.config.Settings;
import org.syncany.db.CloneChunk;
import org.syncany.db.CloneFile;
import org.syncany.db.Database;
import org.syncany.exceptions.InconsistentFileSystemException;
import org.syncany.util.FileUtil;
import org.syncany.tests.FileTestHelper;
import org.syncany.tests.TestSettings;

public class ChunkerTest {

	// Debug-Flag
	private static boolean debug = false;
	
	private static TTTDChunker tttdChunker;
    private static File metaChunkTempFile;
    private static MultiChunk metaChunk;
    private static Set<CloneFile> metaChunkCompletedFiles;
    
    private static final Semaphore metaChunkMutex = new Semaphore(1);
    
	private static File randomlyCreatedFile;
	private static File newFile;
    private static String fileName;
    //private static final int fileSize = 1024*1000; // 100 KB
    private static final int fileSize = 10; // 100 KB
    
    private static List<String> chunkIdStrs = new ArrayList<String>();
    
    private static int sumSize = 0;
    
	private static final int Tmin = 8*1024; // 8 KB
	private static final int Tmax = 16*1024; // 16 KB
	private static final int d = 100;
	private static final int dDash = 50;
//	private static final int fileSize = Tmax*2;
//	private static final int chunkSizeKB = 50;
	private static final int chunkSizeKB = Constants.DEFAULT_CHUNK_SIZE; // 512

	
	private static Enumeration<Chunk> chunkEnum;
	private static boolean chunkerTestFailed = false;
	
	
	@BeforeClass
	public static void init() throws IOException {
		TestSettings testSettings = TestSettings.getInstance();

		if(debug) System.out.println("Cleaning directories..");
		FileTestHelper.emptyDirectory(testSettings.getAppCacheDir());
		FileTestHelper.emptyDirectory(testSettings.getAppDir());
		
		try {
			// wait to see if the folder is empty..
			if(debug) Thread.sleep(5000);
		} catch (Exception e) { }
		
		testSettings.createSettingsInstance();
		
		tttdChunker = new TTTDChunker(16*1024, TTTDChunker.DEFAULT_WINDOW_SIZE, TTTDChunker.DEFAULT_DIGEST_ALG, TTTDChunker.DEFAULT_FINGERPRINT_ALG);
		
		
		if(debug) System.out.println("creating random file..");
		
		fileName = ("testfile-"+Math.random()).replace(".", "");  // random File name in order not to overwrite a possibly existing old file..
		FileTestHelper.generateRandomBinaryFile(new File(testSettings.getAppCacheDir().getAbsolutePath()+File.separator+fileName), fileSize);
		randomlyCreatedFile = new File(testSettings.getAppCacheDir().getAbsolutePath()+File.separator+fileName);

		
		
		/* 
		 * alternatively copy a file into cache dir and test the chunking with this file..
		 */
	/*	fileName = "TESTFILE";
		FileUtil.copy(new File(testSettings.getRootFolder().getAbsoluteFile()+File.separator+fileName), new File(testSettings.getAppCacheDir().getAbsolutePath()+File.separator+fileName));
		randomlyCreatedFile = new File(testSettings.getAppCacheDir().getAbsolutePath()+File.separator+fileName);
		*/
		if(debug) System.out.println("randomlyCreatedFile.exists: "+randomlyCreatedFile.exists());
		
		
		try {
			// wait to see if the folder is empty..
			if(debug) Thread.sleep(5000);
		} catch (Exception e) { }
	}
	
	@Test
	public void TTTDChunkerTest() throws IOException {
		if(debug) System.out.println("\nTTTDChunkerTest..");
		
		
		chunkEnum = tttdChunker.createChunks(randomlyCreatedFile);
		
		int counter = 1;
		while(chunkEnum.hasMoreElements()) {
			Chunk chunk = chunkEnum.nextElement();
			
			if(chunk==null) chunkerTestFailed = true;
			Assert.assertNotNull("Chunk is null!", chunk);
			
			if(debug) {
				System.out.println("Actual Chunk: "+counter++);
				
//				int i = 0;
//				for(byte b : chunk.getChecksum()) {
//					if(debug) System.out.println("Checksum "+i+++": "+b);
//				}
				
				System.out.println("Chunk-size: "+chunk.getSize());
				System.out.println("Chunk-instance: "+chunk+"\n");
			}
		}
		
		if(debug) System.out.println("Generated "+(counter-1)+" chunks.");
	}
	
	
	@Test
	public void MetaChunkerTest() throws IOException, InterruptedException {
		if(debug) System.out.println("\nMetaChunkerTest");

		if(chunkerTestFailed) {
			if(debug) System.out.println("TTTDChunkerTest failed previously, cannot conduct metaChunkerTest.");
			Assert.fail("TTTDChunkerTest failed previously, cannot conduct metaChunkerTest.");
		}
		
		
        // MUTEX
//        metaChunkMutex.acquire();
        
        Chunk chunk = null;
		
        chunkEnum = tttdChunker.createChunks(randomlyCreatedFile);
		
        CloneFile cf = new CloneFile();
        cf.setPath("/Users/ABC");
        cf.setName("a.txt");
        
		try {
			if(debug) System.out.println("Try-Catch-Block");

			
			int count = 0;
			while (chunkEnum.hasMoreElements()) {
				if(debug) System.out.println("\nIteration: "+count++);
				
	            chunk = chunkEnum.nextElement();      
	            
	            
	            // Get from cache; do NOT create
	            CloneChunk cc = Database.getInstance().getChunk(chunk.getChecksum());
	            
	            if (cc != null) {
	                if(debug) System.out.println("from cache: "+cc);
	                cf.addChunk(cc);
	                continue;
	            }
	            
	            if(debug) System.out.println("not from cache");
	            
	            // Chunk does NOT exist so far; create and pack in a meta chunk               
	                    
	            // Close metachunk if it is full
	            // NOTE: this might also be done by the meta chunk timer!
	            if (metaChunk != null && metaChunk.isFull()) {
	            	if(debug) System.out.println("metaChunk is full!");
	            	closeMetaChunk();
	            }
	            
	            // Choose meta ID; by convention, the first chunk ID is the metaId
	            // NOTE: this must be AFTER the closeMetaChunk()-call
	            byte[] metaId;
	            
	            if (metaChunk != null && metaChunk.getId() != null) {
	                metaId = metaChunk.getId();
	            }
	            else {
	                metaId = chunk.getChecksum(); 
	            }                
	            
	            if(debug) System.out.println("Open New MetaChunk");
	            
	            // Open new metachunk
	            if (metaChunk == null) {
	                int chunkSize = Profile.getInstance().getRepository().getChunkSize()*1024;

	                if(debug) System.out.println("ChunkSize: "+chunkSize);
	                chunkSize = 1024*chunkSizeKB;
	                
	                Cipher encCipher = Profile.getInstance().getRepository().getEncryption().createEncCipher(metaId);

	                if(debug) System.out.println("Cache: "+Profile.getInstance().getCache().getFolder());
	                
	                metaChunkTempFile = Profile.getInstance().getCache().createTempFile("metachunk");
//	                metaChunkTempFile = File.createTempFile(String.format("temp-%s-", "metachunk"), ".tmp", testSettings.getAppCacheDir());
	                
	                metaChunk = new CustomMultiChunk(metaId, chunkSize, new CipherOutputStream(new FileOutputStream(metaChunkTempFile), encCipher));
	                //metaChunk = new CustomMultiChunk(metaId, chunkSize, new FileOutputStream(metaChunkTempFile));
	                
	                if(debug) System.out.println("MetaChunkSize (actual): "+metaChunk.getActualSize());
	                if(debug) System.out.println("metaChunk.getId: "+metaChunk.getId());
	                
	                        //new FileOutputStream(metaChunkTempFile));                    
	        //                            new GZIPOutputStream(new FileOutputStream(metaChunkTempFile)));                    
	                
	                
	                // FILES completed in this metachunk (-> can be uploaded)
	                metaChunkCompletedFiles = new HashSet<CloneFile>();

//	                logger.info("Opened new metachunk "+Profile.getInstance().getCache().getMetaChunkFile(metaChunk));
	            }     
	            
	            // Create chunk in DB and add to cache; DO create it!
	            cc = Database.getInstance().getChunk(metaId, chunk.getChecksum(), true);

//	            fileHasNewChunks = true;                
	            cf.addChunk(cc);

	            chunkIdStrs.add(cc.getIdStr());
	            
	            // Now write it to the temp. metachunk
	            metaChunk.write(chunk);
	            
	        } // while (all chunks)
			
			if(metaChunk!=null && metaChunkTempFile!=null) closeMetaChunk();
			
			if(debug) System.out.println("Overall sum of file size: "+sumSize);
			
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		if(debug) {
			System.out.println("Printin chunk-id strings: ");
			
			for(String id : chunkIdStrs) {
				System.out.println("ChunkID: "+id);
			}
		}

        
                    
//        // Last chunk of this file: The last chunk holds the file checksum
//        if (chunk != null) {
//            cf.setChecksum(chunk.getFileChecksum());                 
//            
//            // If this file had new chunks, 
//            // add the file to the "completed in this metachunk" list
//            if (metaChunk != null) {
//                metaChunkCompletedFiles.add(cf);
//            }
//        }    
//
//        // Metachunk is open, file is persisted when the metachunk is persisted
//        if (metaChunk != null) {
//            // Reset timer
//            if (metaChunkTimoutTimerTask != null) {
//                metaChunkTimoutTimerTask.cancel();
//            }
//
//            metaChunkTimoutTimerTask = new MetaChunkTimeoutTimerTask();
//            metaChunkTimeoutTimer.schedule(metaChunkTimoutTimerTask, METACHUNK_UNFINISHED_TIMEOUT);            
//        }
//        
//        // If file has no new chunks, persist it now (because it does not depend
//        // on any metachunks)
//        if (!fileHasNewChunks) {
//            cf.merge(); 
//        }
//
//        if (logger.isLoggable(Level.INFO)) {
//            logger.log(Level.INFO, "Indexer: Done indexing NEW file {0} ...", file);
//        }                        
//        
        // RELEASE MUTEX            
//        metaChunkMutex.release();
		
		
	}
	
	
	@Test
	public void assemblingTest() throws Exception {
		if(debug) System.out.println("\nassemblingTest..");
		
		
		
		if(chunkerTestFailed) {
			if(debug) System.out.println("TTTDChunkerTest failed previously, cannot conduct assemblingTest.");
			Assert.fail("TTTDChunkerTest failed previously, cannot conduct assemblingTest.");
		}
		
		File tempFile = Profile.getInstance().getCache().createTempFile("assemble-"+fileName);
		
		
		try {
			FileOutputStream fos = new FileOutputStream(tempFile, false);
			
			if(debug) System.out.println("- Decrypting chunks to temp file  "+ tempFile.getAbsolutePath() + " ...");
			
			
			for (String chunkIdStr : chunkIdStrs) {
				byte[] checksum = CloneChunk.decodeChecksum(chunkIdStr);
				byte[] metaId = CloneChunk.decodeMetaId(chunkIdStr);

				// Look for plain chunk file a la chunk-xyz
				File decryptedChunkFile = Profile.getInstance().getCache().getPlainChunkFile(checksum);

				String metaChunkFilename = "";
				
				// Extract metachunk to decrypted chunk files
				if (!decryptedChunkFile.exists()) {
					metaChunkFilename = CloneChunk.getFileName(metaId, null);
					
					if(debug) System.out.println("- Chunk does not exist in local cache. Downloading metachunk "+metaChunkFilename+" ...");
					
					// Download metachunk
//					File encryptedMetaChunkFile = Profile.getInstance().getCache().createTempFile("metachunk-"+ CloneChunk.getMetaIdStr(chunkIdStr));
					File encryptedMetaChunkFile = new File(Profile.getInstance().getCache().getFolder().getAbsolutePath()+File.separator+"enc-chunk-"+CloneChunk.getMetaIdStr(chunkIdStr));
//					encryptedMetaChunkFile.deleteOnExit();
					
					
					if(debug) System.out.println("encryptedMetaChunkFile.exists? "+encryptedMetaChunkFile.exists());
					if(debug) System.out.println("encryptedMetaChunkFile.absolutePath "+encryptedMetaChunkFile.getAbsolutePath());
					

					// Decipher and extract to individual chunks
					Cipher decCipher = Profile.getInstance().getRepository().getEncryption().createDecCipher(metaId);
					
					//CustomMultiChunk metaChunk = new CustomMultiChunk(metaId, new FileInputStream(encryptedMetaChunkFile));
					CustomMultiChunk metaChunk = new CustomMultiChunk(metaId, new CipherInputStream(new FileInputStream(encryptedMetaChunkFile), decCipher));

					if(debug) System.out.println("metaChunk: "+metaChunk);
					if(debug) System.out.println("metaChunk.getId: "+metaChunk.getId());
					
					
					
					Chunk chunkx;
					while (null != (chunkx = metaChunk.read())) {
						File decryptedChunkxFile = Profile.getInstance().getCache().getPlainChunkFile(chunkx.getChecksum());

						
						if(debug) System.out.println(" - Extracting chunk " + CloneChunk.encodeIdStr(chunkx.getChecksum())+" to local cache ...");
						
						FileUtil.writeFile(chunkx.getContents(), decryptedChunkxFile);
					}

//					encryptedMetaChunkFile.delete();
				} else {
					
					if(debug) System.out.println("- Chunk EXISTS in cache {0}.: "+decryptedChunkFile.toString());
					
				}

				// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx
				// a chunk can exist in more than one metachunk
				// ---> if multiple clients pack it in a different metachunk
				// (unabhaengig voneinander)

				if(debug) System.out.println("decryptedChunkFile: "+decryptedChunkFile.getAbsolutePath());
				if(debug) System.out.println("decryptedChunkFile.exists? "+decryptedChunkFile.exists());
				
				
				byte[] chunkContents = FileUtil.readFile(decryptedChunkFile);
				int chunkSize = (int) decryptedChunkFile.length();

				// Write decrypted chunk to file
				fos.write(chunkContents, 0, chunkSize);
				if(debug) System.out.println("Written "+metaChunkFilename);
			}

			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new InconsistentFileSystemException(e);
		}
		
		String newFileName = "reconstruct-"+fileName;
		newFile = new File(TestSettings.getInstance().getAppCacheDir().getAbsolutePath()+File.separator+newFileName);
		
        if (!FileUtil.renameVia(tempFile, newFile)) {
        	tempFile.delete();
            throw new InconsistentFileSystemException("Could not rename temp file "+tempFile+" to file "+newFile+"; Enough disk space?");
        }
        
		Assert.assertEquals("FileCheckSum is different!", FileTestHelper.getMD5Checksum(randomlyCreatedFile), FileTestHelper.getMD5Checksum(newFile));
		
	}
	
	
	
	private static void closeMetaChunk() throws IOException {
//    	logger.log(Level.INFO, "CLOSE METACHUNK INSIDE LOOP");
//        closeMetaChunk(false);
        
    	if(debug) System.out.println("Closing Metachunk..");
    	if(debug) System.out.println("MetaChunkSize (actual): "+metaChunk.getActualSize());
        
        // Close meta chunk and get ID (= last chunk's ID)
        metaChunk.close();

        // Rename to final 'temp' metachunk
        File metaChunkFile = Profile.getInstance().getCache().getMetaChunkFile(metaChunk);

        if(debug) {
        	System.out.println("metaChunkFile.exists(): "+metaChunkFile.exists());
        
	        System.out.println("Absolutepath: "+metaChunkFile.getAbsolutePath());
	        
	        System.out.println("Filesize: "+metaChunkFile.length());
	        System.out.println("generated metaChunkTempFile: \""+metaChunkTempFile.getName()+"\" renaming to: \""+metaChunkFile.getName()+"\"");
        }
        

        
        metaChunkTempFile.renameTo(metaChunkFile);

        if(debug) {
        	System.out.println("metaChunkTempFile.getAbsolutePath(): "+metaChunkTempFile.getAbsolutePath());
	        System.out.println("metaChunkTempFile.length: "+metaChunkTempFile.length());
	        System.out.println("metaChunkFile.getAbsolutePath(): "+metaChunkFile.getAbsolutePath());
	        System.out.println("metaChunkFile.length: "+metaChunkFile.length());
        }

        
        sumSize += metaChunkFile.length();
        
//        Set<CloneFile> mergedFiles = new HashSet<CloneFile>();
//        
//        for (CloneFile cf2 : metaChunkCompletedFiles) {
//        	cf2.merge();
//            mergedFiles.add(cf2);
//        }
           
//        for (CloneFile cf2 : mergedFiles) {
//            if (logger.isLoggable(Level.INFO)) {
//                logger.log(Level.INFO, "Indexer: Queuing file {0} at uploader ...", cf2.getFile());
//            }                        
//        }e
        
//        metaChunkProfile.getUploader().queue(metaChunk, mergedFiles);                            

        // Reset meta chunk
//        logger.log(Level.INFO, "Metachunk flushed. NO OPEN Metachunk.");
        if(debug) System.out.println("Metachunk flushed. NO OPEN Metachunk.");
        metaChunk = null; 
	}
	
	@AfterClass
	public static void cleanUp() {
//		FileTestHelper.deleteFile(randomlyCreatedFile);
//		FileTestHelper.deleteFile(newFile);
	}
	
}
