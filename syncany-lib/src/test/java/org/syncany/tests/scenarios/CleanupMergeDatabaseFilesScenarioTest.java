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

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

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
		
		clientA.cleanup();
		
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
	public void testIssue58_2() throws Exception {
		/*
		 * This is the attempt to reproduce issue #58
		 * https://github.com/syncany/syncany/issues/58
		 * 
		 */
		
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile());
		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile());

		CleanupOperationOptions cleanupOptionsOnlyMergeDatabases = new CleanupOperationOptions();
		cleanupOptionsOnlyMergeDatabases.setMergeRemoteFiles(true);
		cleanupOptionsOnlyMergeDatabases.setRemoveOldVersions(true);
		cleanupOptionsOnlyMergeDatabases.setRepackageMultiChunks(false);
		cleanupOptionsOnlyMergeDatabases.setKeepVersionsCount(5);		
				
		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);
		
		UpOperationOptions upOperationOptionsWithCleanupForce = new UpOperationOptions();
		upOperationOptionsWithCleanupForce.setStatusOptions(statusOptionsForceChecksum);
		upOperationOptionsWithCleanupForce.setForceUploadEnabled(true);		
		upOperationOptionsWithCleanupForce.setCleanupEnabled(true);	
		upOperationOptionsWithCleanupForce.setCleanupOptions(cleanupOptionsOnlyMergeDatabases);


		// Run preparations
		
		clientA.down();
		clientA.createNewFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A1)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000001").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000002").exists());

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A2)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000002").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000003").exists());
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A2,B1)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000001").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000002").exists());

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A3,B1)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000003").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000004").exists());

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A4,B1) 
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000004").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000005").exists());
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A4,B2) + (A4,B3) [PURGE]
		clientB.cleanup();		
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000002").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000003").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000004").exists());
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A5,B3) + (A6,B3) [PURGE]
		clientA.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000005").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000006").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000007").exists());
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A6,B4) + (A6,B5) [PURGE]
		clientB.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000004").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000005").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000006").exists());

		
		/*
		 * For some reason, this chunk checksum in the following commit is the reason for the exception.
		 * So we record it here to see where it vanishes from the databases.
		 */
		
		clientA.down();
		clientA.changeFile("A-file.jpg");

		String fileAndChunkChecksumThatRaisesException = StringUtil.toHex(TestFileUtil.createChecksum(clientA.getLocalFile("A-file.jpg")));
		System.out.println("Chunk/File checksum that raises the issue: "+fileAndChunkChecksumThatRaisesException);

		clientA.createNewFile("ADDED_IN_DBV_A7_B5");		
		clientA.up(upOperationOptionsWithCleanupForce); // (A7,B5) + (A8,B5) [PURGE]
		clientA.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000007").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000008").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000009").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A8,B6) + (A8,B7) [PURGE]
		clientB.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000006").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000007").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000008").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));
		
		clientA.down();
		clientA.changeFile("A-file.jpg");		
		clientA.up(upOperationOptionsWithCleanupForce); // (A9,B7) + (A10,B7) [PURGE]
		clientA.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000009").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000010").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A10,B8) + (A10,B9) [PURGE]
		clientB.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000008").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000009").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000010").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));
				
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A10,B10) + (A10,B11) [PURGE]
		clientB.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000010").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000011").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000012").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A11,B11) + (A12,B11) [PURGE]
		clientA.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000012").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000013").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));

		// ^^^ Old chunk deleted!
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A13,B11) + (A14,B11) [PURGE]
		clientA.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000013").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000014").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000015").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A14,B12) + (A14,B13) [PURGE]
		clientB.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000012").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000013").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000014").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A15,B13) + (A16,B13) [PURGE]
		clientA.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000015").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000016").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000017").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A17,B13) + (A18,B13) [PURGE]
		clientA.cleanup();
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000017").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000018").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000019").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));		
		
		// Sync them up
		clientA.down();
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));
		
		clientB.down();
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));
		
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());		
		
		// Run
		clientC.down(); // <<< Here is/was the issue: Client C failed when downloading 
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientC.getDatabaseFile());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
	
	@Test
	public void testIssue58_3() throws Exception {		
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		CleanupOperationOptions cleanupOptionsKeep1 = new CleanupOperationOptions();
		cleanupOptionsKeep1.setMergeRemoteFiles(true);
		cleanupOptionsKeep1.setRemoveOldVersions(true);
		cleanupOptionsKeep1.setRepackageMultiChunks(false);
		cleanupOptionsKeep1.setKeepVersionsCount(1);		
		
		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);		

		UpOperationOptions upNoCleanupForceChecksum = new UpOperationOptions();
		upNoCleanupForceChecksum.setCleanupEnabled(false);
		upNoCleanupForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		UpOperationOptions upWithCleanupKeep1ForceChecksum = new UpOperationOptions();
		upWithCleanupKeep1ForceChecksum.setCleanupEnabled(true);
		upWithCleanupKeep1ForceChecksum.setCleanupOptions(cleanupOptionsKeep1);
		upWithCleanupKeep1ForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		clientA.createNewFile("fileA");
		clientB.createNewFile("fileB");
		
		clientA.up(upNoCleanupForceChecksum);
		clientB.down();
		
		TestFileUtil.copyFile(clientA.getLocalFile("fileA"), clientB.getLocalFile("fileB"));
		String problemChecksum = StringUtil.toHex(FileUtil.createChecksum(clientA.getLocalFile("fileA"), "SHA1"));
		clientB.up(upNoCleanupForceChecksum);
		
		for (int i=0; i<20; i++) {
			clientA.down();
			clientA.changeFile("fileA");
			clientA.up(upNoCleanupForceChecksum);

			clientB.down();
			clientB.changeFile("fileB");
			clientB.up(upNoCleanupForceChecksum);
		}

		System.out.println("Problem checksum: " + problemChecksum);
		
		clientB.cleanup();
		
		clientA.down();
		
		clientA.cleanup();
		
		clientA.down();
		clientC.down();

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
	
	@Test
	public void testIssue58_4() throws Exception {		
		// Test for https://github.com/syncany/syncany/issues/58#issuecomment-43472118
		
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		CleanupOperationOptions cleanupOptionsKeep1 = new CleanupOperationOptions();
		cleanupOptionsKeep1.setMergeRemoteFiles(true);
		cleanupOptionsKeep1.setRemoveOldVersions(true);
		cleanupOptionsKeep1.setRepackageMultiChunks(false);
		cleanupOptionsKeep1.setKeepVersionsCount(1);		
		
		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);		

		UpOperationOptions upNoCleanupForceChecksum = new UpOperationOptions();
		upNoCleanupForceChecksum.setCleanupEnabled(false);
		upNoCleanupForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		UpOperationOptions upWithCleanupKeep1ForceChecksum = new UpOperationOptions();
		upWithCleanupKeep1ForceChecksum.setCleanupEnabled(true);
		upWithCleanupKeep1ForceChecksum.setCleanupOptions(cleanupOptionsKeep1);
		upWithCleanupKeep1ForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		clientB.createNewFile("fileB");		
		clientB.up(upNoCleanupForceChecksum);
		
		clientA.down();
		TestFileUtil.copyFile(clientB.getLocalFile("fileB"), clientA.getLocalFile("fileBcopy"));
		clientA.up(upNoCleanupForceChecksum);
		
		for (int i=0; i<30; i++) {
			clientB.down();
			clientB.changeFile("fileB");
			clientB.up(upNoCleanupForceChecksum);
		}
		
		FileUtils.copyDirectory(testConnection.getRepositoryPath(), new File(testConnection.getRepositoryPath()+"_1_before_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "1_before_cleanup"));
		
		CleanupOperationOptions cleanupMergeAndRemoveOldFiles = new CleanupOperationOptions();
		cleanupMergeAndRemoveOldFiles.setMergeRemoteFiles(true);
		cleanupMergeAndRemoveOldFiles.setRemoveOldVersions(true);
		clientB.cleanup(cleanupMergeAndRemoveOldFiles);
		
		FileUtils.copyDirectory(testConnection.getRepositoryPath(), new File(testConnection.getRepositoryPath()+"_2_after_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "2_after_cleanup"));

		clientC.down(); // <<< "Cannot determine file content for checksum X"
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
	
	@Test
	public void testIssue58_5() throws Exception {		
		// Test for https://github.com/syncany/syncany/issues/58#issuecomment-43472118
		
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		TestClient clientD = new TestClient("D", testConnection);
		TestClient clientE = new TestClient("E", testConnection);
		
		CleanupOperationOptions cleanupOptionsKeep1 = new CleanupOperationOptions();
		cleanupOptionsKeep1.setMergeRemoteFiles(true);
		cleanupOptionsKeep1.setRemoveOldVersions(true);
		cleanupOptionsKeep1.setRepackageMultiChunks(false);
		cleanupOptionsKeep1.setKeepVersionsCount(1);		
		
		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);		

		UpOperationOptions upNoCleanupForceChecksum = new UpOperationOptions();
		upNoCleanupForceChecksum.setCleanupEnabled(false);
		upNoCleanupForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		UpOperationOptions upWithCleanupKeep1ForceChecksum = new UpOperationOptions();
		upWithCleanupKeep1ForceChecksum.setCleanupEnabled(true);
		upWithCleanupKeep1ForceChecksum.setCleanupOptions(cleanupOptionsKeep1);
		upWithCleanupKeep1ForceChecksum.setStatusOptions(statusOptionsForceChecksum);
		
		// First round
		
		clientB.createNewFile("fileA");		
		clientB.up(upNoCleanupForceChecksum);
		
		clientA.down();
		TestFileUtil.copyFile(clientA.getLocalFile("fileA"), clientA.getLocalFile("fileAcopy"));
		clientA.up(upNoCleanupForceChecksum);
		
		clientA.down();
		
		for (int i=0; i<30; i++) {
			clientA.down();
			clientA.changeFile("fileA");
			clientA.up(upNoCleanupForceChecksum);
		}
		
		// First cleanup
		
		FileUtils.copyDirectory(testConnection.getRepositoryPath(), new File(testConnection.getRepositoryPath()+"_1_before_cleanup"));
		FileUtils.copyDirectory(clientA.getConfig().getDatabaseDir(), new File(clientA.getConfig().getAppDir(), "1_before_cleanup"));
		
		CleanupOperationOptions cleanupMergeAndRemoveOldFiles = new CleanupOperationOptions();
		cleanupMergeAndRemoveOldFiles.setMergeRemoteFiles(true);
		cleanupMergeAndRemoveOldFiles.setRemoveOldVersions(true);
		clientA.cleanup(cleanupMergeAndRemoveOldFiles);
		
		FileUtils.copyDirectory(testConnection.getRepositoryPath(), new File(testConnection.getRepositoryPath()+"_2_after_cleanup"));
		FileUtils.copyDirectory(clientA.getConfig().getDatabaseDir(), new File(clientA.getConfig().getAppDir(), "2_after_cleanup"));

		clientC.down(); // If this doesn't crash that's a win!
		
		// Second round
		
		for (int i=0; i<30; i++) {
			clientB.down();
			clientB.changeFile("fileA");
			clientB.up(upNoCleanupForceChecksum);
		}
		
		// Second cleanup
		
		FileUtils.copyDirectory(testConnection.getRepositoryPath(), new File(testConnection.getRepositoryPath()+"_3_before_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "3_before_cleanup"));
		
		clientB.cleanup(cleanupMergeAndRemoveOldFiles);
		
		FileUtils.copyDirectory(testConnection.getRepositoryPath(), new File(testConnection.getRepositoryPath()+"_4_after_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "4_after_cleanup"));
		
		clientD.down(); // If this doesn't crash that's a win!
		
		// Third round
		
		for (int i=0; i<30; i++) {
			clientB.down();
			clientB.changeFile("fileA");
			clientB.up(upNoCleanupForceChecksum);
		}
		
		clientB.deleteFile("fileAcopy"); // < Remove original checksum from first DBV
		clientB.up(upNoCleanupForceChecksum);
		
		// Third cleanup

		FileUtils.copyDirectory(testConnection.getRepositoryPath(), new File(testConnection.getRepositoryPath()+"_5_before_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "5_before_cleanup"));
		
		clientB.cleanup(cleanupMergeAndRemoveOldFiles);
		
		FileUtils.copyDirectory(testConnection.getRepositoryPath(), new File(testConnection.getRepositoryPath()+"_6_after_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "6_after_cleanup"));
		
		clientE.down(); // If this doesn't crash that's a win!
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
}
