package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.SyncUpOperationResult;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class SymlinkIgnoreScenarioTest {
	@Test
	public void testChangedModifiedDate() throws Exception {
		if (File.separatorChar == '\\') {			
			return; // Skip test for Windows, no symlinks there!
		}
		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);

		// Run 
		File symlinkFile = clientA.getLocalFile("symlink-name");
		Process process = Runtime.getRuntime().exec(new String[] { "/bin/ln", "-s", "/etc/hosts", symlinkFile.getAbsolutePath() });
		process.waitFor();
		
		assertTrue("Symlink should exist at "+symlinkFile, symlinkFile.exists());
		
		SyncUpOperationResult upResult = clientA.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		
		// Test 1: Check result sets for inconsistencies
		assertFalse("Status should NOT return changes.", statusResult.getChangeSet().hasChanges());
		assertFalse("File should NOT be uploaded. Symlinks are unsupported right now.", upResult.getChangeSet().hasChanges());
		
		// Test 2: Check database for inconsistencies
		Database database = clientA.loadLocalDatabase();
		DatabaseVersion databaseVersion = database.getLastDatabaseVersion();

		assertNull("File should NOT be uploaded. Symlinks are unsupported right now.", database.getFileHistory("large-test-file"));		
		assertNull("There should NOT be a new database version, because file should not have been added.", databaseVersion);
		
		// Test 3: Check file system for inconsistencies
		File repoPath = ((LocalConnection) testConnection).getRepositoryPath();		
		assertEquals("Repository should NOT contain any files.", 0, repoPath.list().length);	
		
		// Tear down
		clientA.cleanup();
	}
}
