package org.syncany.tests.scenarios;

import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class FileVanishedScenarioTest {	
	private static final Logger logger = Logger.getLogger(FileVanishedScenarioTest.class.getSimpleName());
	
	// TODO [low] If a file has vanished, are its chunks and multichunks still added to the database, and then uploaded? If so, fix this!
	
	@Test
	public void testManyRenames() throws Exception {
		// Setup 
		final Connection testConnection = TestConfigUtil.createTestLocalConnection();
		final TestClient clientA = new TestClient("A", testConnection);
		final int numFiles = 10;
		final int sizeFiles = 5000*1024;
		
		// Prepare by creating test files
		logger.log(Level.INFO, "Creating test files ...");
		
		for (int i=0; i<=numFiles; i++) {
			clientA.createNewFile("A-original"+i, sizeFiles);
		}
		
		// Prepare threads (delete & run up)
		Thread deleteFilesThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i=numFiles; i>=0; i--) {
					clientA.deleteFile("A-original"+i);
				}
			}			
		});
		
		Thread runUpThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					clientA.up();
				}
				catch (Exception e) {
					fail(e.getMessage());
				}				
			}			
		});
		
		// Delete files and run up simultaneously
		// --> This will hopefully lead to a couple of 'vanished' files
		logger.log(Level.INFO, "Starting 'up' thread ...");		
		runUpThread.start();
		
		Thread.sleep(200);

		logger.log(Level.INFO, "Starting 'delete' thread ...");
		deleteFilesThread.start();		
		
		runUpThread.join();
		deleteFilesThread.join();
		
		fail("Some asserts please");
		
		// Tear down
		clientA.cleanup();
	}
}
