package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.SyncUpOperationResult;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class FileLockedScenarioTest {
	@Test
	public void testFileLocked() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		
		// Run! 
		clientA.createNewFile("locked-file");
		RandomAccessFile lockedFile = new RandomAccessFile(clientA.getLocalFile("locked-file"), "rw");		
		FileLock lockedFileLock = lockedFile.getChannel().lock();
		
		runUpAndTestForEmptyDatabase(testConnection, clientA);	
		
		// Tear down
		lockedFileLock.release();
		lockedFile.close();
		
		clientA.cleanup();
	}
	
	@Test
	public void testPermissionDenied() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		
		// Run
		File noReadPermissionFile = clientA.createNewFile("no-read-permission-file");
		noReadPermissionFile.setReadable(false);
		
		runUpAndTestForEmptyDatabase(testConnection, clientA);		
		
		// Tear down
		clientA.cleanup();
	}	
	
	private void runUpAndTestForEmptyDatabase(Connection connection, TestClient client) throws Exception {
		SyncUpOperationResult upResult = client.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		
		// Test 1: Check result sets for inconsistencies
		assertTrue("Status command expected to return changes.", statusResult.getChangeSet().hasChanges());
		assertFalse("File should NOT be uploaded while it is locked.", upResult.getChangeSet().hasChanges());
		
		// Test 2: Check database for inconsistencies
		Database database = client.loadLocalDatabase();
		DatabaseVersion databaseVersion = database.getLastDatabaseVersion();

		assertNull("File should NOT be uploaded while it is locked.", database.getFileHistory("large-test-file"));		
		assertNull("There should NOT be a new database version, because file should not have been added.", databaseVersion);
		
		// Test 3: Check file system for inconsistencies
		File repoPath = ((LocalConnection) connection).getRepositoryPath();		
		assertEquals("Repository should NOT contain any files.", 0, repoPath.list().length);			
	}
}
