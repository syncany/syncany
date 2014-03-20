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
import org.syncany.operations.StatusOperation;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
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
	public void testIssue58_1() throws Exception {
		/*
		 * This is the attempt to reproduce issue #58
		 * https://github.com/binwiederhier/syncany/issues/58
		 * 
		 */
		
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		CleanupOperationOptions cleanupOptionsOnlyMergeDatabases = new CleanupOperationOptions();
		cleanupOptionsOnlyMergeDatabases.setMergeRemoteFiles(true);
		cleanupOptionsOnlyMergeDatabases.setRemoveOldVersions(true);
		cleanupOptionsOnlyMergeDatabases.setRepackageMultiChunks(false);
				
		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);
		
		UpOperationOptions upOperationOptionsNoCleanup = new UpOperationOptions();
		upOperationOptionsNoCleanup.setStatusOptions(statusOptionsForceChecksum);
		upOperationOptionsNoCleanup.setForceUploadEnabled(true);		
		upOperationOptionsNoCleanup.setCleanupEnabled(true);	
		upOperationOptionsNoCleanup.setCleanupOptions(cleanupOptionsOnlyMergeDatabases);

		// Run preparations
		
		clientA.down();
		clientA.createNewFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 0 (A1)

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 1 (A2)
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 2 (A2,B1)

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 3 (A3,B1)

		clientA.down();
		clientA.createNewFile("Syncany Crypto.jpg");
		clientA.createNewFile("Syncany Crypto (1).jpg"); // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Added "6262ef818c4f72bd8a69768aa261c2eb16a6c1a5"				
		clientA.createNewFile("Syncany Crypto (2).jpg");
		clientA.createNewFile("Syncany Crypto (3).jpg");
		clientA.up(upOperationOptionsNoCleanup); // 4 (A4,B1)

		clientB.down();
		clientB.createNewFolder("python_ctf_workshop");
		clientB.createNewFile("python_ctf_workshop/level01.py");		
		clientB.createNewFile("python_ctf_workshop/level02.py");
		clientB.up(upOperationOptionsNoCleanup); // 5 (A4,B2)

		clientA.down();
		clientA.createNewFile("Wordlists.gz");
		clientA.up(upOperationOptionsNoCleanup); // 6 (A5,B2)

		clientB.down();
		clientB.createNewFolder("Untitled Folder/workspace/.metadata/.plugins/org.eclipse.core.resources/.history/a8/");		
		clientB.createNewFile("Untitled Folder/workspace/.metadata/.plugins/org.eclipse.core.resources/.history/a8/b0cc654a817b001310f7c9cad6a53f98");
		// ...
		clientB.up(upOperationOptionsNoCleanup); // 7 (A5,B3)

		clientA.down();
		clientA.moveFile("Syncany Crypto.jpg", "Untitled Folder/Syncany Crypto.jpg");
		clientA.moveFile("Syncany Crypto (1).jpg", "Untitled Folder/Syncany Crypto (1).jpg");  // <<<<<<<<<<<<<< Moved first here
		clientA.moveFile("Syncany Crypto (2).jpg", "Untitled Folder/Syncany Crypto (2).jpg"); 
		clientA.moveFile("Syncany Crypto (3).jpg", "Untitled Folder/Syncany Crypto (3).jpg");
		clientA.moveFile("Wordlists.gz", "Untitled Folder/Wordlists.gz");
		clientA.moveFile("python_ctf_workshop", "Untitled Folder/python_ctf_workshop");
		clientA.createNewFolder("Untitled Folder/Untitled Folder2");
		// ...
		clientA.up(upOperationOptionsNoCleanup); // 8 (A6,B3)

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 9 (A6,B4) <<<< wins the conflict
		
		Thread.sleep(100);
		
		clientA.down();
		//clientA.changeFile("A-file.jpg");
		clientA.moveFile("Untitled Folder", "philipp");  // <<<<<<<<<<<<<< Moved again in losing DIRTY version
		clientA.up(upOperationOptionsNoCleanup); // 10 (A7,B3) <<<< loses the conflict (later marked DIRTY by A)

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 11 (A6,B5) <<<<<< This is where C should have a conflict
				
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 12
fail("xx");
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 13
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 14

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 15

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 16

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 17

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 18

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 19

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 20

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 21

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 22

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 23
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 24

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 25
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 26

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 27
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 28		
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 29

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 30

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 31

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 32
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 33	
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 34
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 35
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 36
						
		// Sync them up
		clientA.down();
		clientB.down();
		
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());		
		
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
	public void testIssue58_2() throws Exception {
		/*
		 * This is the attempt to reproduce issue #58
		 * https://github.com/binwiederhier/syncany/issues/58
		 * 
		 */
		
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		CleanupOperationOptions cleanupOptionsOnlyMergeDatabases = new CleanupOperationOptions();
		cleanupOptionsOnlyMergeDatabases.setMergeRemoteFiles(true);
		cleanupOptionsOnlyMergeDatabases.setRemoveOldVersions(true);
		cleanupOptionsOnlyMergeDatabases.setRepackageMultiChunks(false);
				
		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);
		
		UpOperationOptions upOperationOptionsNoCleanup = new UpOperationOptions();
		upOperationOptionsNoCleanup.setStatusOptions(statusOptionsForceChecksum);
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
		clientA.createNewFile("ADDED_IN_DBV_8");		
		clientA.up(upOperationOptionsNoCleanup); // 8

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 9 <<<< DIRTY, wins
		
		Thread.sleep(100);
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientB.moveFile("ADDED_IN_DBV_8", "MOVED_IN_DIRTY_DBV_9");  // <<<<<<<<<<<<<<
		clientA.up(upOperationOptionsNoCleanup); // 9 <<<< DIRTY, loses

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 10 <<<<<< This is where C should have a conflict
				
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 11

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 12

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 13
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 14

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 15

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 16

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 17
/*
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 18

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 19

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 20

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 21

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 22

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 23
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 24

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 25
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 26

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 27
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 28		
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 29

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 30

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 31

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 32
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 33	
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsNoCleanup); // 34
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 35
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsNoCleanup); // 36
						*/
		// Sync them up
		clientA.down();
		clientB.down();
		
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());		
		
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
