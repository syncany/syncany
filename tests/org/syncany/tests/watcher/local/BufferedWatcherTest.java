package org.syncany.tests.watcher.local;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.contrib.BufferedWatcher;

import org.junit.Before;
import org.junit.Test;
import org.syncany.tests.FileTestHelper;
import org.syncany.tests.TestSettings;
import org.syncany.watch.local.CommonLocalWatcher;

public class BufferedWatcherTest {
	
	private static final int fileSize = 16*1024*2;
	private BufferedWatcher bufferedWatcher;
	private TestSettings testSettings;
	
	@Before
	public void init() throws SecurityException, IOException {
		testSettings = TestSettings.getInstance();
		testSettings.createSettingsInstance();
		testSettings.cleanAllFolders();
		
		bufferedWatcher = new BufferedWatcher();
	}
	
	@Test
	public void bufferedWatcherTest() {
		BufferedWatcher b = new BufferedWatcher();
		
		Assert.assertNotNull("BufferedWatcher is null", b);
		
		int delay = b.getDelay();
		boolean dksEvent = b.DEFAULT_KILL_SOURCE_EVENTS;
		
		Assert.assertNotNull("BufferedWatcher DEFAULT_DELAY is null", delay);
		Assert.assertNotNull("Bufferedwatcher DEFAULT_KILL_SOURCE_EVENTS is null", dksEvent);
	}
	
	@Test
	public void bufferedWatcherDelayTest() {
		int delay = 500;
		
		BufferedWatcher b = new BufferedWatcher(delay);
		Assert.assertEquals(delay, b.getDelay());
	}
	
	@Test
	public void BufferedWatcherDelayKSEventTest() {
		int delay = 500;
		boolean killsourceEvents = true;
		
		BufferedWatcher b = new BufferedWatcher(delay, killsourceEvents);
		
		Assert.assertEquals(delay, b.getDelay());
		//Assert.assertEquals(killsourceEvents, b.DEFAULT_KILL_SOURCE_EVENTS);
	}
	
	@Test 
	public void startTest() throws InterruptedException {
		BufferedWatcher b = new BufferedWatcher();
		
		b.start();
		Thread.sleep(10000);
		
		Assert.assertTrue("BufferedWatcher did not start worker or timer", b.isActive());
	}
	
	@Test
	public void stopTest() throws InterruptedException {
		BufferedWatcher b = new BufferedWatcher();
		
		b.start();
		Thread.sleep(10000);
		
		b.stop();
		Thread.sleep(10000);
		
		// Assert.assertFalse("BufferedWatcher did not stop worker or timer", b.isActive());
	}
	
	@Test
	public void isActiveTest() throws Exception {
		// Start BufferedWatcher
		BufferedWatcher b = new BufferedWatcher();
		b.start();
		Thread.sleep(10000);
		
		// Check if bufferedWatcher is active
		Assert.assertEquals(true, b.isActive());
	}
	
	@Test
	public void getDelayTest() throws Exception {
		// Start BufferedWatcher
		BufferedWatcher b = new BufferedWatcher();
		b.start();
		Thread.sleep(10000);
		
		// Check if bufferedWatcher delay
		Assert.assertTrue(b.getDelay() >= 0);
	}
	
	@Test
	public void addWatchTest() throws Exception {
		// Create a file
		String fileName = ("testfile-"+Math.random()).replace(".", "");
		FileTestHelper.generateRandomBinaryFile(new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName), fileSize);
	    File randomFile = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator);
	
	    // Start Watcher
	    CommonLocalWatcher watcher = new CommonLocalWatcher();
	    watcher.start();
	    
	    // Call addWatch on randomFile
		WatchKey key = bufferedWatcher.addWatch(randomFile, true, watcher);
		
		Assert.assertEquals(key.isValid(), true);
	}
	
	@Test 
	public void removeWatchFile() throws Exception {
		// Create a file
		String fileName = ("testfile-"+Math.random()).replace(".", "");
		FileTestHelper.generateRandomBinaryFile(new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName), fileSize);
		File randomFile = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName);
			
		// Remove Watcher of randomfile
		bufferedWatcher.removeWatch(randomFile);
	}
	
	@Test
	public void removeWatchKeyTest() throws Exception {
		// Create randomFile
		String fileName = ("testfile-"+Math.random()).replace(".", "");
		FileTestHelper.generateRandomBinaryFile(new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName), fileSize);
		File randomFile = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator);
		
	    // Start Watcher
	    CommonLocalWatcher watcher = new CommonLocalWatcher();
	    watcher.start();
		
	    WatchKey key = bufferedWatcher.addWatch(randomFile, true, watcher);
	    
	    //Remove key
	    bufferedWatcher.removeWatch(key);
	}
	
	@Test
	public void removeWatchNodeTest() throws IOException {
		// Create randomFile
		String fileName = ("testfile-"+Math.random()).replace(".", "");
		FileTestHelper.generateRandomBinaryFile(new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName), fileSize);
		File randomFilePath = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator);
		File randomFile = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName);
		
	    // Start Watcher
	    CommonLocalWatcher watcher = new CommonLocalWatcher();
	    watcher.start();
		
	    WatchKey key = bufferedWatcher.addWatch(randomFilePath, true, watcher);
	    
	    // Can't create WatchNode to use bufferedWatcher.removeWatch(node)   
	}
	
//	@Test
//	public void getEventFileTest() throws Exception {		
//		// Start watcher
//		CommonLocalWatcher watcher = new CommonLocalWatcher();
//
//		System.out.println("Start LocalWatcher");
//		watcher.start();
//		Thread.sleep(10000);
//
//		// Create a file
//		String fileName = ("testfile-"+Math.random()).replace(".", "");
//		FileTestHelper.generateRandomBinaryFile(new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName), fileSize);
//	    File randomFile = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator);
//			
//		localWatcher.queueCheckFile(randomFile);
//		WatchKey key = bufferedWatcher.addWatch(randomFile, true, watcher);
//		
//		@SuppressWarnings("static-access")
//		WatchEvent<PathWatchEvent> event = (WatchEvent<PathWatchEvent>) indexer.getInstance().getQueue().take();
//		
//		bufferedWatcher.getEventFile(event, key);
//	}
	
	@Test
	public void getRootKeyTest() throws Exception {
		// Get RootFolder
		File rootFile = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath());
		
		// Create File
		String fileName = ("testfile-"+Math.random()).replace(".", "");
		FileTestHelper.generateRandomBinaryFile(new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator+fileName), fileSize);
		File randomFilePath = new File(TestSettings.getInstance().getRootFolder().getAbsolutePath()+File.separator);

		// Start Watcher
	    CommonLocalWatcher watcher = new CommonLocalWatcher();
	    watcher.start();
		WatchKey rootKey = bufferedWatcher.addWatch(rootFile, true, watcher);
		WatchKey randomKey = bufferedWatcher.addWatch(randomFilePath, true, watcher);
		
		// Assert key is equal to retrieved key
		Assert.assertEquals(true, rootKey == bufferedWatcher.getRootKey(randomKey));		
	}
	
	
}
