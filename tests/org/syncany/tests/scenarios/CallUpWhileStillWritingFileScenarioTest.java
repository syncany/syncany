package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class CallUpWhileStillWritingFileScenarioTest {	
	private static final Logger logger = Logger.getLogger(CallUpWhileStillWritingFileScenarioTest.class.getSimpleName());
	
	@Test
	public void testUpWhileWritingFile() throws Exception {
		// Setup 
		final Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);
		
		final File testFile = clientA.getLocalFile("large-test-file");
		final long testFileLength = 100*1024*1024;
		
		Thread writeFileThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					logger.log(Level.SEVERE, "Started thread to write file to "+testFile+"  ...");

					FileOutputStream fos = new FileOutputStream(testFile);
					Random randomEngine = new Random();
					
					byte[] buf = new byte[4096];
					int writtenLen = 0;
					
					while (writtenLen < testFileLength) {
						randomEngine.nextBytes(buf);
					    fos.write(buf, 0, buf.length);
					    
					    writtenLen += buf.length;
					}
					
					fos.close();	
					
					logger.log(Level.SEVERE, "Ended thread to write file to "+testFile+"  ...");					
				}
				catch (Exception e) {
					// Nothing.
				}
			}			
		});
		
		Logging.setGlobalLogLevel(Level.SEVERE);
		
		// Run!
		writeFileThread.start();
		
		Thread.sleep(50);
		
		logger.log(Level.SEVERE, "Started clientA.up()");
		UpOperationResult upResult = clientA.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		logger.log(Level.SEVERE, "Ended clientA.up()");
		
		writeFileThread.join();
		
		// Test 1: Check result sets for inconsistencies
		assertTrue("Status command expected to return changes.", statusResult.getChangeSet().hasChanges());
		assertFalse("File should NOT be uploaded while still writing (no half-file upload).", upResult.getChangeSet().hasChanges());
		
		// Test 2: Check database for inconsistencies
		Database database = clientA.loadLocalDatabase();
		DatabaseVersion databaseVersion = database.getLastDatabaseVersion();

		assertNull("File should NOT be uploaded while still writing (no half-file upload).", database.getFileHistory("large-test-file"));		
		assertNull("There should NOT be a new database version, because file should not have been added.", databaseVersion);
		
		// Test 3: Check file system for inconsistencies
		File repoPath = ((LocalConnection) testConnection).getRepositoryPath();		
		assertEquals("Repository should NOT contain any files.", 0, repoPath.list().length);
	
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
