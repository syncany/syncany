package org.syncany.tests.scenarios;

import static org.junit.Assert.*;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.database.Database;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.PartialFileHistory;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class FileVanishedScenarioTest {	
	private static final Logger logger = Logger.getLogger(FileVanishedScenarioTest.class.getSimpleName());
	
	// TODO [low] If a file has vanished, are its chunks and multichunks still added to the database, and then uploaded? If so, fix this!
	
	@Test
	public void testCallUpWhileDeletingFiles() throws Exception {
		// Setup 
		final Connection testConnection = TestConfigUtil.createTestLocalConnection();
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);
		final int numFiles = 100;
		final int numFilesVanished = 50;
		final int numFilesRemaining = numFiles - numFilesVanished;
		final int sizeFiles = 500*1024;
		
		// Prepare by creating test files
		logger.log(Level.INFO, "Creating test files ...");
		
		for (int i=0; i<=numFiles; i++) {
			clientA.createNewFile("A-original"+i, sizeFiles);
		}
		
		// Prepare threads (delete & run up)
		Thread deleteFilesThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i=numFiles; i>=numFilesVanished; i--) {
					boolean deleteSuccess = clientA.deleteFile("A-original"+i);
					
					if (deleteSuccess) {
						logger.log(Level.SEVERE, "Deleted "+clientA.getLocalFile("A-original"+i));
					}
					else {
						logger.log(Level.SEVERE, "FAILED TO DELETE FILE "+clientA.getLocalFile("A-original"+i));
					}
					
					try { Thread.sleep(50); }
					catch (Exception e) { }
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
					e.printStackTrace();
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
		
		// Test 1: There should be between 50 and 100 file histories in the database
		Database databaseClientA = clientA.loadLocalDatabase();
		
		assertTrue("There should be less file histories than originally added files.", databaseClientA.getFileHistories().size() < numFiles);
		assertTrue("There should be more (or equal size) file histories than files there are.", databaseClientA.getFileHistories().size() >= numFilesRemaining);
		
		// Test 2: Now up the rest, there should be exactly 50 files in the database
		clientA.up();		
		databaseClientA = clientA.loadLocalDatabase();
		assertEquals("There should be EXACTLY "+numFilesRemaining+" file histories in the database.", numFilesRemaining, getNumNotDeletedFileHistories(databaseClientA));
		
		// Test 3: After that, the sync between the clients should of course still work
		clientB.down();
		assertFileListEquals("Files of both clients should be identical.", clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());				
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
	
	@Test
	public void testFolderVanishesWhenSyncingDown() throws Exception {		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// A 
		clientA.createNewFolder("folder1");		
		clientA.up();

		// B
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		// A
		clientA.createNewFile("folder1/file1");	
		clientA.up();
		
		// B 
		clientB.deleteFile("folder1");
		assertFalse(clientB.getLocalFile("folder1").exists());
		
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
	
	private int getNumNotDeletedFileHistories(Database db) {
		int numNotDeletedFileHistories = 0;
		
		for (PartialFileHistory fileHistory : db.getFileHistories()) {
			if (fileHistory.getLastVersion().getStatus() != FileStatus.DELETED) {
				numNotDeletedFileHistories++;
			}
		}
		
		return numNotDeletedFileHistories;
	}
}
