package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.FileUtil;

public class SymlinkSyncScenarioTest {
	@Test
	public void testSymlinkOneUpOneDown() throws Exception {
		if (!FileUtil.symlinksSupported()) {			
			return; // Skip test for Windows, no symlinks there!
		}
		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		File symlinkFile = clientA.getLocalFile("symlink-name");
		FileUtil.createSymlink(new File("/etc/hosts"), symlinkFile);		
		
		assertTrue("Symlink should exist at "+symlinkFile, symlinkFile.exists());
		
		UpOperationResult upResult = clientA.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		
		// Test 1: Check result sets for inconsistencies
		assertTrue("Status should return changes.", statusResult.getChangeSet().hasChanges());
		assertTrue("File should be uploaded.", upResult.getChangeSet().hasChanges());
		
		// Test 2: Check database for inconsistencies
		Database database = clientA.loadLocalDatabase();
		DatabaseVersion databaseVersion = database.getLastDatabaseVersion();

		assertNotNull("File should be uploaded.", database.getFileHistory("symlink-name"));		
		assertNotNull("There should be a new database version, because file should not have been added.", databaseVersion);
		
		// Test 3: Check file system for inconsistencies
		File repoPath = ((LocalConnection) testConnection).getRepositoryPath();		
		assertEquals("Repository should contain only ONE database file, not multichunks.", 1, repoPath.list().length);	
		
		// B down
		clientB.down();
		assertEquals("Local folder should contain one file (link!)", 1, clientB.getLocalFiles().size());
		
		File localSymlinkFile = clientB.getLocalFile("symlink-name");
		assertTrue("Local symlink file should exist.", localSymlinkFile.exists());
		assertTrue("Local symlink file should be a SYMLINK.", FileUtil.isSymlink(localSymlinkFile));
		assertEquals("Local symlink file should point to actual target.", "/etc/hosts", FileUtil.readSymlinkTarget(localSymlinkFile));
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
	
	@Test
	public void testSymlinkMultipleUpsAndDowns() throws Exception {
		if (!FileUtil.symlinksSupported()) {			
			return; // Skip test for Windows, no symlinks there!
		}

		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		clientA.createNewFile("symlink-target");

		File symlinkFile = clientA.getLocalFile("symlink-name");
		FileUtil.createSymlink(new File("symlink-target"), symlinkFile); // << relative target	
		
		assertTrue("Symlink should exist at "+symlinkFile, symlinkFile.exists());
		// TODO [high] Relative symlinks don't work
		
		clientA.up();

		// B down
		clientB.down();
		assertEquals("Local folder should contain one file (link!)", 1, clientB.getLocalFiles().size());
		
		File localSymlinkFile = clientB.getLocalFile("symlink-name");
		assertTrue("Local symlink file should exist.", localSymlinkFile.exists());
		assertTrue("Local symlink file should be a SYMLINK.", FileUtil.isSymlink(localSymlinkFile));
		assertEquals("Local symlink file should point to actual target.", "symlink-target", FileUtil.readSymlinkTarget(localSymlinkFile));
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
