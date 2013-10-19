package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.database.Database;
import org.syncany.operations.DownOperation.DownOperationResult;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.FileUtil;

public class ChangedAttributesScenarioTest {
	@Test
	public void testChangeAttributes() throws Exception {		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		clientA.createNewFile("file1.jpg");
		clientA.upWithForceChecksum();
		
		clientB.down();
		
		File bFile = clientB.getLocalFile("file1.jpg");
		Path bFilePath = Paths.get(bFile.getAbsolutePath());
		
		if (FileUtil.isWindows()) {
			Files.setAttribute(bFilePath, "dos:readonly", true);
		}
		else if (FileUtil.isUnixLikeOperatingSystem()) {
			Files.setPosixFilePermissions(bFilePath, PosixFilePermissions.fromString("rwxrwxrwx"));
		}		
		
		ChangeSet changes = clientB.status();
		
		assertTrue("Status-Operation should return changes.", changes.hasChanges());
		UpOperationResult upResult = clientB.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		
		// Test 1: Check result sets for inconsistencies
		assertTrue("Status should return changes.", statusResult.getChangeSet().hasChanges());
		assertTrue("File should be uploaded.", upResult.getChangeSet().hasChanges());
		
		// Test 2: Check database for inconsistencies
		Database database = clientB.loadLocalDatabase();

		assertNotNull("File should be uploaded.", database.getFileHistory("file1.jpg"));		
		assertEquals("There should be a new database version, because file should not have been added.", 2, database.getDatabaseVersions().size());
		
		// B down
		clientA.down();

		// Test 1: file1.jpg permissions
		File aFile = clientA.getLocalFile("file1.jpg");
		Path aFilePath = Paths.get(aFile.getAbsolutePath());
		
		if (FileUtil.isWindows()) {
			Object readOnlyAttribute = Files.getAttribute(aFilePath, "dos:readonly");
			assertTrue("Read-only should be true.", (Boolean) readOnlyAttribute);
		}
		else if (FileUtil.isUnixLikeOperatingSystem()) {
			Set<PosixFilePermission> posixFilePermissions = Files.getPosixFilePermissions(aFilePath);
			assertEquals("Should be rwxrwxrwx.", "rwxrwxrwx", PosixFilePermissions.toString(posixFilePermissions));
		}	
		
		// Test 2: The rest
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}	
	
	@Test
	public void testNewFileWithDifferingAttributes() throws Exception {		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
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
		
		if (FileUtil.isWindows()) {
			aReadOnlyAttribute = Files.getAttribute(aFilePath, "dos:readonly");
			Files.setAttribute(bFilePath, "dos:readonly", true);
		}
		else if (FileUtil.isUnixLikeOperatingSystem()) {
			aPosixFilePermissions = Files.getPosixFilePermissions(aFilePath);
			Files.setPosixFilePermissions(bFilePath, PosixFilePermissions.fromString("rwxrwxrwx"));
		}	
				
		clientA.upWithForceChecksum();		
		DownOperationResult downResult = clientB.down(); // This is the key operation 
		
		// Test 1: Check result sets for inconsistencies
		assertTrue("File should be downloaded.", downResult.getChangeSet().hasChanges());
		
		// Test 2: file1.jpg permissions (again!
		if (FileUtil.isWindows()) {
			Object bReadOnlyAttribute = Files.getAttribute(aFilePath, "dos:readonly");
			assertEquals("Read-only should be true.", aReadOnlyAttribute, bReadOnlyAttribute);
		}
		else if (FileUtil.isUnixLikeOperatingSystem()) {
			Set<PosixFilePermission> bPosixFilePermissions = Files.getPosixFilePermissions(aFilePath);
			assertEquals("Should be rwxrwxrwx.", PosixFilePermissions.toString(aPosixFilePermissions), PosixFilePermissions.toString(bPosixFilePermissions));
		}	
		
		// Test 3: The rest
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}	
}
