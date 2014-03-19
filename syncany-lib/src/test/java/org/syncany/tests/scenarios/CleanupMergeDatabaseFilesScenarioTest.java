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

import static org.junit.Assert.*;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.io.File;

import org.junit.Test;
import org.syncany.config.to.ConfigTO;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.operations.CleanupOperation.CleanupOperationOptions;
import org.syncany.operations.UpOperation.UpOperationOptions;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class CleanupMergeDatabaseFilesScenarioTest {
	@Test
	public void testCleanupMergeDatabaseFilesScenario1() throws Exception {
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		CleanupOperationOptions cleanupOptionsOnlyMergeDatabases = new CleanupOperationOptions();
		cleanupOptionsOnlyMergeDatabases.setMergeRemoteFiles(true);
		cleanupOptionsOnlyMergeDatabases.setRemoveOldVersions(false);
		cleanupOptionsOnlyMergeDatabases.setRepackageMultiChunks(false);
		
		UpOperationOptions upOperationOptionsNoCleanup = new UpOperationOptions();
		upOperationOptionsNoCleanup.setForceUploadEnabled(true);
		upOperationOptionsNoCleanup.setCleanupEnabled(true);	
		upOperationOptionsNoCleanup.setCleanupOptions(cleanupOptionsOnlyMergeDatabases);

		// Run preparations
		int[] clientUpSequence = new int[] {
				// Modeled after a crashing real-world scenario
				// 1 = A down+up, 2 = B down+up

				// The actual sequence was:
				// 1, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 1, 2, 1, 1, 1, 2,
				// 2, 2, 2, 1, 1, 1, 2, 1, 1, 1, 2, 2, 2, 1, 1, 2, 1, 1

				// This simplified sequence also crashes/crashed
				// 16x "1", merge happens after 15!
				1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1
		};

		for (int i=0; i<clientUpSequence.length; i++) {
			if (clientUpSequence[i] == 1) {
				clientA.down();

				clientA.createNewFile("A-file"+i+".jpg", i);
				clientA.up(upOperationOptionsNoCleanup);
			}
			else {
				clientB.down();
				
				clientB.createNewFile("B-file"+i+".jpg", i);
				clientB.up(upOperationOptionsNoCleanup);				
			}
		}
		
		clientA.down();
		clientB.down();
		
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// Make sure the "merge" process actually happened
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000001").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000005").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000010").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());
		
		// Run
		clientC.down(); // <<< Here is/was the issue: Client C failed when downloading 
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientC.getDatabaseFile());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}
	
	@Test
	public void testCleanupMergeDatabaseFilesScenario2() throws Exception {
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		CleanupOperationOptions cleanupOptionsOnlyMergeDatabases = new CleanupOperationOptions();
		cleanupOptionsOnlyMergeDatabases.setMergeRemoteFiles(true);
		cleanupOptionsOnlyMergeDatabases.setRemoveOldVersions(false);
		cleanupOptionsOnlyMergeDatabases.setRepackageMultiChunks(false);
		
		UpOperationOptions upOperationOptionsNoCleanup = new UpOperationOptions();
		upOperationOptionsNoCleanup.setForceUploadEnabled(true);
		upOperationOptionsNoCleanup.setCleanupEnabled(true);	
		upOperationOptionsNoCleanup.setCleanupOptions(cleanupOptionsOnlyMergeDatabases);

		// Run preparations
		int A_NEW = 11;
		int A_UPD = 12;
		int A_DEL = 13;
		
		int B_NEW = 21;
		int B_UPD = 22;
		int B_DEL = 23;
		
		int[] clientUpSequence = new int[] {
			A_NEW, A_UPD, A_UPD, B_UPD, B_UPD, A_UPD, A_UPD, A_DEL, // one file history 
			A_NEW, A_UPD, A_UPD, B_UPD, A_UPD, A_UPD, A_UPD, A_UPD, A_UPD, A_UPD, A_UPD, B_DEL, // And another			
		};

		
		for (int i=0; i<clientUpSequence.length; i++) {
			if (clientUpSequence[i] >= 10 && clientUpSequence[i] <= 19) {
				clientA.down();

				if (clientUpSequence[i] == A_NEW) {
					clientA.createNewFile("A-file.jpg");
				}
				else if (clientUpSequence[i] == A_UPD) {
					clientA.changeFile("A-file.jpg");
				}
				else if (clientUpSequence[i] == A_DEL) {
					clientA.deleteFile("A-file.jpg");
				}

				clientA.up(upOperationOptionsNoCleanup);
			}
			else { 
				clientB.down();

				if (clientUpSequence[i] == B_NEW) {
					clientB.createNewFile("A-file.jpg");
				}
				else if (clientUpSequence[i] == B_UPD) {
					clientB.changeFile("A-file.jpg");
				}
				else if (clientUpSequence[i] == B_DEL) {
					clientB.deleteFile("A-file.jpg");
				}

				clientB.up(upOperationOptionsNoCleanup);			
			}
		}
		
		clientA.down();
		clientB.down();
		
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// Make sure the "merge" process actually happened
		/*assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000001").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000005").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000010").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());*/
		
		// Run
		clientC.down(); // <<< Here is/was the issue: Client C failed when downloading 
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientC.getDatabaseFile());

		
		fail("implement this");
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
	
	@Test
	public void testCleanupDirty() throws Exception {
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		CleanupOperationOptions cleanupOptionsOnlyMergeDatabases = new CleanupOperationOptions();
		cleanupOptionsOnlyMergeDatabases.setMergeRemoteFiles(true);
		cleanupOptionsOnlyMergeDatabases.setRemoveOldVersions(true);
		cleanupOptionsOnlyMergeDatabases.setRepackageMultiChunks(false);
		
		UpOperationOptions upOperationOptionsNoCleanup = new UpOperationOptions();
		upOperationOptionsNoCleanup.setForceUploadEnabled(true);
		upOperationOptionsNoCleanup.setCleanupEnabled(true);	
		upOperationOptionsNoCleanup.setCleanupOptions(cleanupOptionsOnlyMergeDatabases);

		// Run preparations
		
		clientA.down();
		clientA.createNewFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 0

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 1
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 2

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 3

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 4

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 5

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 6

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 7

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 8

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 9 <<<< DIRTY
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 9 <<<< Wins the conflict

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 10
		
		// Sync them up
		clientA.down();
		clientB.down();
		
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// Make sure the "merge" process actually happened
		/*assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000001").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000005").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000010").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());*/
		
		// Run
		clientC.down(); // <<< Here is/was the issue: Client C failed when downloading 
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientC.getDatabaseFile());

		
		fail("implement this");
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
}
