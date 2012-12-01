package org.syncany.tests.watcher.local;

import java.io.File;
import java.util.concurrent.BlockingQueue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import org.syncany.config.Profile;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.index.Indexer;
import org.syncany.index.requests.CheckIndexRequest;
import org.syncany.index.requests.DeleteIndexRequest;
import org.syncany.index.requests.IndexRequest;
import org.syncany.index.requests.MoveIndexRequest;
import org.syncany.tests.FileTestHelper;
import org.syncany.tests.TestSettings;
import org.syncany.watch.local.CommonLocalWatcher;
import org.syncany.watch.local.LocalWatcher;

public class LocalWatcherTest {
	private static final int fileSize = 16*1024*2;
	private static TestSettings testSettings;
	private static LocalWatcher localWatcher;
	private static Indexer indexer;
	private static File randomFile;
	private static File randomFile2;
	private static String fileName;
	private static String fileName2;
	
	@BeforeClass
	public static void init() throws Exception {
		testSettings = TestSettings.getInstance();
		
		testSettings.createSettingsInstance();
		
		Profile profile = Profile.getInstance();
    	LocalConnection con = new LocalConnection();
    	con.setFolder(testSettings.getRepDir());
    	profile.getRepository().setConnection(con);
    	profile.setRoot(testSettings.getRootFolder());
    	
    	fileName = ("testfile-"+Math.random()).replace(".", "");
    	fileName2 = ("testfile2-"+Math.random()).replace(".", "");
		FileTestHelper.generateRandomBinaryFile(new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName), fileSize);
		randomFile = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName);
		randomFile2 = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName2);
	}
	
	@Test
	public void queueCheckFileTest() throws Exception {
		// Start Watcher
		localWatcher = new CommonLocalWatcher();

		System.out.println("Start LocalWatcher");
		localWatcher.start();
		Thread.sleep(10000);
		
		// Add file to queue
		System.out.println("Adding file to queue");
		localWatcher.queueCheckFile(randomFile);
		System.out.println("File added to queue");
		
		// BlockingQueue<IndexRequest> queue = i.getQueue();
		@SuppressWarnings("static-access")
		CheckIndexRequest event = (CheckIndexRequest) indexer.getInstance().getQueue().take();
		
		Assert.assertTrue(event.getFile().getAbsolutePath().equals(testSettings.getRootFolder()+File.separator+fileName));
		System.out.println("Event file: " + event.getFile().getAbsolutePath() + " Root" + testSettings.getRootFolder());
		Assert.assertTrue(event.getFile().getName().equals(randomFile.getName()));
	}

	@Test
	public void queueMoveFileTest() throws Exception {
/*		localWatcher = new CommonLocalWatcher();

		System.out.println("Start LocalWatcher");
		localWatcher.start();
		Thread.sleep(10000);
		
		localWatcher.queueCheckFile(randomFile2);
		Thread.sleep(10000);
		// queueMoveFile
		localWatcher.queueMoveFile(randomFile, randomFile2);
		
		
		MoveIndexRequest event = (MoveIndexRequest) indexer.getQueue().take();
		
		// 
		System.out.println(event.toString());
	*/}
	
	@Test
	public void queueDeleteFileTest() throws Exception {
		// Start watcher
		localWatcher = new CommonLocalWatcher();

		System.out.println("Start LocalWatcher");
		localWatcher.start();
		Thread.sleep(10000);
		
		// Queue file to delete
		localWatcher.queueDeleteFile(randomFile);
		
		// Retrieve event
		@SuppressWarnings("static-access")
		DeleteIndexRequest event = (DeleteIndexRequest) indexer.getInstance().getQueue().take();
	
		// Event file must be equal to randomFile
		Assert.assertTrue(event.getFile() == randomFile);
	}
	
	@Test
	public void getInstanceTest() throws Exception {
		// Start watcher
		localWatcher = LocalWatcher.getInstance();
		localWatcher.start();
		Thread.sleep(10000);
		
		// Compare
		LocalWatcher localWatcherInstance = LocalWatcher.getInstance();
		Assert.assertTrue(localWatcherInstance.equals(localWatcher)); 
	}
	
	@AfterClass
	public static void cleanUp() {
		localWatcher.stop();
		FileTestHelper.deleteFile(randomFile);
	}
	
}
