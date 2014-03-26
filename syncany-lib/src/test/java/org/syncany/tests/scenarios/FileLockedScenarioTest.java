/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.tests.scenarios.framework.ClientActions;
import org.syncany.tests.scenarios.framework.CreateFileTree;
import org.syncany.tests.scenarios.framework.Executable;
import org.syncany.tests.scenarios.framework.LockFile;
import org.syncany.tests.scenarios.framework.UnlockFile;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.EnvironmentUtil;

public class FileLockedScenarioTest {
	// TODO [high] Fix issues with readonly files on Windows, and r------ files on Linux
	
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
		
		clientA.deleteTestData();
	}
	
	@Test
	public void testPermissionDeniedNotReadable() throws Exception {
		if (EnvironmentUtil.isWindows()) {
			return; // Not possible in windows
		}
		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		
		// Run
		File noReadPermissionFile = clientA.createNewFile("no-read-permission-file");
		Path filePath = Paths.get(noReadPermissionFile.getAbsolutePath());

		Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.OTHERS_READ);
		Files.setPosixFilePermissions(filePath, perms);		

		runUpAndTestForConsistentDatabase(testConnection, clientA);		
		
		// Tear down
		clientA.deleteTestData();
	}	

	@Test
	public void testPermissionDeniedNotWritable() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		
		// Run
		File noReadPermissionFile = clientA.createNewFile("no-read-permission-file");
		noReadPermissionFile.setWritable(false, false);
		
		runUpAndTestForConsistentDatabase(testConnection, clientA);		
		
		// Tear down
		clientA.deleteTestData();
	}	
	
	private void runUpAndTestForConsistentDatabase(Connection connection, TestClient client) throws Exception {
		UpOperationResult upResult = client.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		
		// Test 1: Check result sets for inconsistencies
		assertTrue("Status command expected to return changes.", statusResult.getChangeSet().hasChanges());
		assertTrue("File should be uploaded while it is read-only.", upResult.getChangeSet().hasChanges());
		
		// Test 2: Check database for inconsistencies
		SqlDatabase database = client.loadLocalDatabase();
		assertNotNull("There should be a new database version, because files should have been added.", database.getLastDatabaseVersionHeader());
		
		// Test 3: Check file system for inconsistencies
		File repoPath = ((LocalConnection) connection).getRepositoryPath();		
		assertEquals("Repository should contain any files.", 2, repoPath.list().length);			
	}
	
	private void runUpAndTestForEmptyDatabase(Connection connection, TestClient client) throws Exception {
		UpOperationResult upResult = client.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		
		// Test 1: Check result sets for inconsistencies
		assertFalse("Status command expected to return NO changes.", statusResult.getChangeSet().hasChanges());
		assertFalse("File should NOT be uploaded while it is locked.", upResult.getChangeSet().hasChanges());
		
		// Test 2: Check database for inconsistencies
		SqlDatabase database = client.loadLocalDatabase();

		assertNull("File should NOT be uploaded while it is locked.", database.getFileVersionByPath("large-test-file"));		
		assertNull("There should NOT be a new database version, because file should not have been added.", database.getLastDatabaseVersionHeader());
		
		// Test 3: Check file system for inconsistencies
		File repoPath = ((LocalConnection) connection).getRepositoryPath();
		String[] repoFileList = repoPath.list(new FilenameFilter() {			
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("db-");
			}
		});
		
		assertEquals("Repository should NOT contain any files.", 0, repoFileList.length);			
	}
	
	
	@Test
	public void testLockUnlockFile() throws Exception {		
		final Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);
						
		ClientActions.run(clientA, null, new CreateFileTree(), new Executable() {
			@Override
			public void execute() throws Exception {
				clientA.upWithForceChecksum();
				
				clientB.down();				
				assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
				assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
			}			
		});
		
		ClientActions.run(clientA, null, new LockFile(), new Executable() {
			@Override
			public void execute() throws Exception {
				clientA.upWithForceChecksum();				
				
				clientB.down();
				assertEquals(clientA.getLocalFilesExcludeLockedAndNoRead().size(), clientB.getLocalFilesExcludeLockedAndNoRead().size()-1);
			}			
		});
		
		ClientActions.run(clientA, null, new UnlockFile(), new Executable() {
			@Override
			public void execute() throws Exception {
				clientA.upWithForceChecksum();						

				clientB.down();				
				assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
				assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
			}			
		});
		
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
