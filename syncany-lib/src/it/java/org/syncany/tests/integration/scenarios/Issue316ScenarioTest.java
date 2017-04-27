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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertConflictingFileExists;
import static org.syncany.tests.util.TestAssertUtil.assertConflictingFileNotExists;

import java.util.Arrays;

import org.junit.Test;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.cleanup.CleanupOperationResult;
import org.syncany.operations.cleanup.CleanupOperationResult.CleanupResultCode;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlUtil;

public class Issue316ScenarioTest {
	@Test
	public void testIssue316CleanupThenDeleteFile() throws Exception {		
		/*
		 * This is a test for issue #316. It creates a situation in which a 'down'
		 * fails after a cleanup. In that first down, the local database is deleted 
		 * entirely, so that all databases are downloaded again, so all remote file
		 * versions are compared to "null". In this bug, comparing a deleted file version
		 * to a local existing file failed, because this case was thought to not happen
		 * ever.  
		 */
		
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(Arrays.asList(new String[] {
				// List of failing operations (regex)
				// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

				"rel=(5|6|7) .+download.+multichunk" // << 3 retries!
		}));

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		java.sql.Connection databaseConnectionB = clientB.getConfig().createDatabaseConnection();
				
		CleanupOperationOptions cleanupOptionsKeepOne = new CleanupOperationOptions();
		cleanupOptionsKeepOne.setMaxDatabaseFiles(1);
		cleanupOptionsKeepOne.setForce(true);	
				
		clientA.createNewFile("Kazam_screencast_00010.mp4");
		clientA.upWithForceChecksum();
				
		clientB.down();
		assertTrue(clientB.getLocalFile("Kazam_screencast_00010.mp4").exists());
		
		clientA.createNewFile("SomeFileTOIncreaseTheDatabaseFileCount");
		clientA.upWithForceChecksum();
		
		CleanupOperationResult cleanupResult = clientA.cleanup(cleanupOptionsKeepOne);
		assertEquals(CleanupResultCode.OK, cleanupResult.getResultCode());
		
		clientA.deleteFile("Kazam_screencast_00010.mp4");
		clientA.upWithForceChecksum();
		
		// First 'down' of client B after the cleanup. 
		// This fails AFTER the local database was wiped.
		
		boolean downFailedAtB = false;
		
		try {
			clientB.down();
		}
		catch (Exception e) {
			downFailedAtB = true;
		}
		
		assertTrue("Down operation should have failed.", downFailedAtB);
		assertEquals("0", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionB));
		assertEquals("0", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionB));
		assertEquals("0", TestSqlUtil.runSqlSelect("select count(*) from known_databases", databaseConnectionB));
		
		// Second 'down' of client B; This should delete the file 'Kazam_screencast_00010.mp4',
		// because it matches the checksum of the 'DELETED' entry
		
		clientB.down();
		assertConflictingFileNotExists("Kazam_screencast_00010.mp4", clientB.getLocalFiles());
		assertFalse(clientB.getLocalFile("Kazam_screencast_00010.mp4").exists());
		
		// Tear down
		clientB.deleteTestData();
		clientA.deleteTestData();
	}			

	@Test
	public void testIssue316CleanupThenDeleteFileButLocalFileChanged() throws Exception {		
		/*
		 * Same test as above, but local file has changed at client B.
		 */
		
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(Arrays.asList(new String[] {
				// List of failing operations (regex)
				// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

				"rel=(5|6|7) .+download.+multichunk" // << 3 retries!
		}));

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
				
		CleanupOperationOptions cleanupOptionsKeepOne = new CleanupOperationOptions();
		cleanupOptionsKeepOne.setMaxDatabaseFiles(1);
		cleanupOptionsKeepOne.setForce(true);	
				
		clientA.createNewFile("Kazam_screencast_00010.mp4");
		clientA.upWithForceChecksum();
				
		clientB.down();
		assertTrue(clientB.getLocalFile("Kazam_screencast_00010.mp4").exists());
		
		clientB.changeFile("Kazam_screencast_00010.mp4"); // <<<<<<<<< Different from above test
		
		clientA.createNewFile("SomeFileTOIncreaseTheDatabaseFileCount");
		clientA.upWithForceChecksum();
		
		CleanupOperationResult cleanupResult = clientA.cleanup(cleanupOptionsKeepOne);
		assertEquals(CleanupResultCode.OK, cleanupResult.getResultCode());
		
		clientA.deleteFile("Kazam_screencast_00010.mp4");
		clientA.upWithForceChecksum();
		
		// First 'down' of client B after the cleanup. 
		// This fails AFTER the local database was wiped.
		
		boolean downFailedAtB = false;
		
		try {
			clientB.down();
		}
		catch (Exception e) {
			downFailedAtB = true;
		}
		
		assertTrue("Down operation should have failed.", downFailedAtB);
		
		// Second 'down' of client B; This should delete the file 'Kazam_screencast_00010.mp4',
		// because it matches the checksum of the 'DELETED' entry
		
		clientB.down();
		assertConflictingFileExists("Kazam_screencast_00010.mp4", clientB.getLocalFiles()); // <<<<<<<<< Different from above test 
		assertFalse(clientB.getLocalFile("Kazam_screencast_00010.mp4").exists());
		
		// Tear down
		clientB.deleteTestData();
		clientA.deleteTestData();
	}		
	
}
