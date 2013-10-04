package org.syncany.tests.scenarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertConflictingFileExists;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.operations.UpOperation.SyncUpOperationOptions;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class DirtyDatabaseScenarioTest {
	@Test
	public void testDirtyDatabase() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// Run 
		SyncUpOperationOptions upOptionsForceEnabled = new SyncUpOperationOptions();
		upOptionsForceEnabled.setForceUploadEnabled(true);
		
		clientA.createNewFile("A-file1.jpg", 50*1024);
		clientA.up(upOptionsForceEnabled);
				
		clientB.createNewFile("A-file1.jpg", 50*1024);
		clientB.up(upOptionsForceEnabled);
		
		clientB.down(); // This creates a dirty database		
		assertTrue("Dirty database should exist.", clientB.getDirtyDatabaseFile().exists());
		assertFileEquals("Files should be identical", clientA.getLocalFile("A-file1.jpg"), clientB.getLocalFile("A-file1.jpg"));
		assertConflictingFileExists("A-file1.jpg", clientB.getLocalFiles());
		
		clientB.up(); // This deletes the dirty database file
		assertFalse("Dirty database should NOT exist.", clientB.getDirtyDatabaseFile().exists());
		
		clientA.down(); // This pulls down the conflicting file
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		assertConflictingFileExists("A-file1.jpg", clientA.getLocalFiles());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
