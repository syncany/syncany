package org.syncany.tests.connection.local;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.connection.Uploader;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginInfo;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.connection.plugins.local.LocalPluginInfo;
import org.syncany.connection.plugins.local.LocalTransferManager;
import org.syncany.tests.util.TestUtil;

public class UploaderWithLocalPluginTest {
	private File tempLocalSourceDir;
	private File tempLocalRepoDir;
	private Map<String, String> localPluginSettings;
	
	@Before
	public void setUp() throws Exception {
		File rootDir = TestUtil.createTempDirectoryInSystemTemp();
		
		tempLocalSourceDir = new File(rootDir+"/local");
		tempLocalSourceDir.mkdir();
		
		tempLocalRepoDir = new File(rootDir+"/repo");		
		tempLocalRepoDir.mkdir();
				
		localPluginSettings = new HashMap<String, String>();
		localPluginSettings.put("path", tempLocalRepoDir.getAbsolutePath());
	}
	
	@After
	public void tearDown() {
		//TestUtil.deleteDirectory(tempLocalSourceDir);
	}
	
	@Test
	public void testUploadQueue() throws StorageException, IOException, InterruptedException {
		// Setup
		int localFileSize1MB = 1*1024*1024;
		int localFileCount = 10;
		
		Connection connection = loadPluginAndCreateConnection();
		List<File> localFilesToUpload = TestUtil.generateRandomBinaryFilesInDirectory(tempLocalSourceDir, localFileSize1MB, localFileCount);
		
		// Go!		
		Uploader uploader = new Uploader(connection);
		
		assertTrue("Queue expected to be empty.", uploader.isQueueEmtpy());
		
		for (File localFileToUpload : localFilesToUpload) {
			uploader.queue(localFileToUpload);
		}
		
		assertEquals("Queue expected to contain "+localFilesToUpload.size()+" files.", localFilesToUpload.size(), uploader.getQueueSize());
		
		uploader.start();
		
		assertFalse("Queue expected to contain files.", uploader.isQueueEmtpy());
		
		Thread.sleep(5000);
		
		assertTrue("Queue expected to be empty.", uploader.isQueueEmtpy());
		
		/*Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
		}, 5000);*/
	}
	
	@Test
	public void testMultipleStartsAndStops() throws StorageException {
		Connection connection = loadPluginAndCreateConnection();
		Uploader uploader = new Uploader(connection);
		
		boolean uploaderStartSuccess = uploader.start();
		assertTrue("Uploader expected to start successfully.", uploaderStartSuccess);
		
		uploaderStartSuccess = uploader.start();
		assertFalse("Uploader expected to fail second start.", uploaderStartSuccess);
		
		boolean uploaderIsRunning = uploader.isRunning();
		assertTrue("Uploader expected to be running.", uploaderIsRunning);
		
		boolean uploaderStopSuccess = uploader.stop();
		assertTrue("Uploader expected to be stopped.", uploaderStopSuccess);
		
		uploaderStopSuccess = uploader.stop();
		assertFalse("Uploader expected to fail second stop.", uploaderStopSuccess);
	}
	
	
	private Connection loadPluginAndCreateConnection() throws StorageException {
		PluginInfo pluginInfo = Plugins.get("local");	
		
		Connection connection = pluginInfo.createConnection();				
		connection.init(localPluginSettings);
		
		return connection;
	}
				
}
