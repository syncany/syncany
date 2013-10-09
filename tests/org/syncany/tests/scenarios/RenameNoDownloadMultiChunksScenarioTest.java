package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.operations.DownOperation.DownOperationResult;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class RenameNoDownloadMultiChunksScenarioTest {
	@Test
	public void testRenameAndCheckIfMultiChunksAreDownloaded() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		
		// Prepare, create file at A, sync it to B
		clientA.createNewFile("A-file1");
		clientA.sync();		
		clientB.sync();
				
		// Now move file, and sync
		clientA.moveFile("A-file1", "A-file-moved1");
		clientA.up();
		
		DownOperationResult downOperationResult = clientB.down();		
		assertEquals("No multichunks should have been downloaded.", 0, downOperationResult.getDownloadedMultiChunks().size());
		assertTrue("Moved files should exist.", clientB.getLocalFile("A-file-moved1").exists());		
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}	
}
