package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.tests.scenarios.framework.AbstractClientAction;
import org.syncany.tests.scenarios.framework.ClientActions;
import org.syncany.tests.scenarios.framework.CreateFileTree;
import org.syncany.tests.scenarios.framework.Executable;
import org.syncany.tests.scenarios.framework.LockFile;
import org.syncany.tests.scenarios.framework.UnlockFile;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.FileUtil;

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
	public void testPermissionDeniedNotReadable() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		
		// Run
		File noReadPermissionFile = clientA.createNewFile("no-read-permission-file");
		Path filePath = Paths.get(noReadPermissionFile.getAbsolutePath());
		if (FileUtil.isWindows()) {
			Files.setAttribute(filePath, "dos:readonly", true);
		}
		else if (FileUtil.isUnixLikeOperatingSystem()) {
			Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwxrwx"));
		}		

		runUpAndTestForEmptyDatabase(testConnection, clientA);		
		
		// Tear down
		clientA.cleanup();
	}	

	@Test
	public void testPermissionDeniedNotWritable() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		
		// Run
		File noReadPermissionFile = clientA.createNewFile("no-read-permission-file");
		noReadPermissionFile.setWritable(false, false);
		
		runUpAndTestForEmptyDatabase(testConnection, clientA);		
		
		// Tear down
		clientA.cleanup();
	}	
	
	private void runUpAndTestForEmptyDatabase(Connection connection, TestClient client) throws Exception {
		UpOperationResult upResult = client.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		
		// Test 1: Check result sets for inconsistencies
		assertFalse("Status command expected to return NO changes.", statusResult.getChangeSet().hasChanges());
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
	
	@Test
	public void testLockUnlockFile() throws Exception {		
		final Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);
						
		ClientActions.runOps(clientA, null,
			new AbstractClientAction[] {
				new CreateFileTree(),
				new LockFile(),
				new UnlockFile()
			},
			new Executable() {
				@Override
				public void execute() throws Exception {
					clientA.upWithForceChecksum();		
					
					clientB.down();

					// TODO [low] The assert fails for the LockFile action because getLocalFiles() does not include locked files, and client A has one more locked file than client B
					assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
					assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());					
				}			
			}
		);
		
		// TODO [low] Add asserts here, this does not check if the locked file is indexed or not. Something like changeSet.ignoredFiles should be added.
		
		clientA.cleanup();
		clientB.cleanup();
	}
}
