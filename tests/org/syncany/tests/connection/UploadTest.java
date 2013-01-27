package org.syncany.tests.connection;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.MultiChunk;
import org.syncany.chunk.TTTDChunker;
import org.syncany.config.Profile;
import org.syncany.connection.Uploader;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.db.CloneFile;
import org.syncany.tests.FileTestHelper;
import org.syncany.tests.TestSettings;

public class UploadTest {
	
	
	private static final int fileSize = 16*1024*2;
	 // ca. 4 chunks resulting
	private static TestSettings testSettings; 
	private static Uploader uploader;
	private static MultiChunk metaChunk;
	private static Set<CloneFile> updateSet;
	
	
	private static File randomlyCreatedFile;
	
	
	@BeforeClass
	public static void init() throws Exception {
		
		testSettings = TestSettings.getInstance();
		testSettings.createSettingsInstance();
		
		FileTestHelper.emptyDirectory(testSettings.getAppCacheDir());
		FileTestHelper.emptyDirectory(testSettings.getAppDir());
		
		Profile profile = Profile.getInstance();
    	LocalConnection con = new LocalConnection();
    	con.setFolder(testSettings.getRepDir());
    	profile.getRepository().setConnection(con);
    	profile.setRoot(testSettings.getRootFolder());
    	
		uploader = new Uploader(profile);
		
		String fileName = ("testfile-"+Math.random()).replace(".", "");  // random File name in order not to overwrite a possibly existing old file..
		FileTestHelper.generateRandomBinaryFile(new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName), fileSize);
		randomlyCreatedFile = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName);
		
		
		ArrayList<MultiChunk> metaChunkList = FileTestHelper.getMetaChunksOfFile(randomlyCreatedFile);
		
		
		// TODO -> create this test data with methods 
		CloneFile c1 = new CloneFile();
		c1.setPath(testSettings.getRootFolder().getAbsolutePath());		// set this with the help of settings..
		c1.setName(fileName);
		
		System.out.println("c1 path: "+c1.getPath());
		
		
		updateSet = new HashSet<CloneFile>();
		updateSet.add(c1);
		
		
		if(!metaChunkList.isEmpty()) metaChunk = metaChunkList.get(0);
		

		
		// doesn't work until the worker is not started 
//		uploader.queue(mc1, updateSet);
	}
	
	
	@Test()
	public void uploadTest() {
		System.out.println("Starting Worker..");
		uploader.start();
		System.out.print("Worker started");
		
		
		System.out.println("Adding MetaChunk and UpdateSet to uploader queue..");		
		uploader.queue(metaChunk, updateSet);
		
		System.out.println("Test Thread-ID: "+Thread.currentThread().getId());
		
		
		
		long startTime = System.currentTimeMillis();
		System.out.println("Starttime millis: "+startTime);
		
	
		// Wait for Upload
		try {
			Thread.sleep(10000);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Alternatively
//		while((startTime+10000)!=System.currentTimeMillis()) {
//			// do something
//		}
		
		
		System.out.println("Test finished, Endtime millis: "+System.currentTimeMillis());
	}
	
	
	@AfterClass
	public static void cleanUp() {
		FileTestHelper.deleteFile(randomlyCreatedFile);
	}
}
