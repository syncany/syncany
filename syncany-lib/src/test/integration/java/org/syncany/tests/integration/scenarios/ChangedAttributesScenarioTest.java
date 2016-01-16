/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.integration.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.ChangeSet;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.status.StatusOperationResult;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.EnvironmentUtil;

public class ChangedAttributesScenarioTest {
	@Test
	public void testChangeAttributes() throws Exception {		
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		clientA.createNewFile("file1.jpg");
		clientA.upWithForceChecksum();
		
		clientB.down();
		
		File bFile = clientB.getLocalFile("file1.jpg");
		Path bFilePath = Paths.get(bFile.getAbsolutePath());
		
		if (EnvironmentUtil.isWindows()) {
			Files.setAttribute(bFilePath, "dos:readonly", true);
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			Files.setPosixFilePermissions(bFilePath, PosixFilePermissions.fromString("rwxrwxrwx"));
		}		
		
		StatusOperationResult statusResult = clientB.status();
		assertNotNull(statusResult);
		
		ChangeSet changes = statusResult.getChangeSet();
		
		assertTrue("Status-Operation should return changes.", changes.hasChanges());
		UpOperationResult upResult = clientB.up();
		StatusOperationResult statusResultFromUp = upResult.getStatusResult();
		
		// Test 1: Check result sets for inconsistencies
		assertTrue("Status should return changes.", statusResultFromUp.getChangeSet().hasChanges());
		assertTrue("File should be uploaded.", upResult.getChangeSet().hasChanges());
		
		// Test 2: Check database for inconsistencies
		SqlDatabase database = clientB.loadLocalDatabase();

		assertEquals("File should be uploaded.", 1, database.getFileList("file1.jpg", null, false, false, false, null).size());		
		assertEquals("There should be a new database version, because file should not have been added.", 2, database.getLocalDatabaseBranch().size());
		
		// B down
		clientA.down();

		// Test 1: file1.jpg permissions
		File aFile = clientA.getLocalFile("file1.jpg");
		Path aFilePath = Paths.get(aFile.getAbsolutePath());
		
		if (EnvironmentUtil.isWindows()) {
			Object readOnlyAttribute = Files.getAttribute(aFilePath, "dos:readonly");
			assertTrue("Read-only should be true.", (Boolean) readOnlyAttribute);
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			Set<PosixFilePermission> posixFilePermissions = Files.getPosixFilePermissions(aFilePath);
			assertEquals("Should be rwxrwxrwx.", "rwxrwxrwx", PosixFilePermissions.toString(posixFilePermissions));
		}	
		
		// Test 2: The rest
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}	
	
	@Test
	public void testNewFileWithDifferingAttributes() throws Exception {		
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Create new file with differing attributes
		clientA.createNewFile("file1.jpg");
		FileUtils.copyFile(clientA.getLocalFile("file1.jpg"), clientB.getLocalFile("file1.jpg"));
		
		File aFile = clientA.getLocalFile("file1.jpg"); // Client B's attributes differ!
		Path aFilePath = Paths.get(aFile.getAbsolutePath());		
		
		Object aReadOnlyAttribute = null;
		Set<PosixFilePermission> aPosixFilePermissions = null;
		
		File bFile = clientB.getLocalFile("file1.jpg"); // Client B's attributes differ!
		Path bFilePath = Paths.get(bFile.getAbsolutePath());
		
		if (EnvironmentUtil.isWindows()) {
			aReadOnlyAttribute = Files.getAttribute(aFilePath, "dos:readonly");
			Files.setAttribute(bFilePath, "dos:readonly", true);
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			aPosixFilePermissions = Files.getPosixFilePermissions(aFilePath);
			Files.setPosixFilePermissions(bFilePath, PosixFilePermissions.fromString("rwxrwxrwx"));
		}	
				
		clientA.upWithForceChecksum();		
		DownOperationResult downResult = clientB.down(); // This is the key operation 
		
		// Test 1: Check result sets for inconsistencies
		assertTrue("File should be downloaded.", downResult.getChangeSet().hasChanges());
		
		// Test 2: file1.jpg permissions (again!
		if (EnvironmentUtil.isWindows()) {
			Object bReadOnlyAttribute = Files.getAttribute(aFilePath, "dos:readonly");
			assertEquals("Read-only should be true.", aReadOnlyAttribute, bReadOnlyAttribute);
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			Set<PosixFilePermission> bPosixFilePermissions = Files.getPosixFilePermissions(aFilePath);
			assertEquals("Should be rwxrwxrwx.", PosixFilePermissions.toString(aPosixFilePermissions), PosixFilePermissions.toString(bPosixFilePermissions));
		}	
		
		// Test 3: The rest
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}	
}
