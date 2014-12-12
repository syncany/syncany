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
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertConflictingFileNotExists;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.tests.util.TestSqlUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class CleanupMergeDatabaseFilesScenarioTest {
	@Test
	public void testCleanupMergeDatabaseFilesScenario1() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);

		CleanupOperationOptions cleanupOptionsOnlyMergeDatabases = new CleanupOperationOptions();
		cleanupOptionsOnlyMergeDatabases.setRemoveOldVersions(false);

		UpOperationOptions upOperationOptionsNoCleanup = new UpOperationOptions();
		upOperationOptionsNoCleanup.setForceUploadEnabled(true);

		// Run preparations
		int[] clientUpSequence = new int[] {
				// Modeled after a crashing real-world scenario
				// 1 = A down+up, 2 = B down+up

				// The actual sequence was:
				// 1, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 1, 2, 1, 1, 1, 2,
				// 2, 2, 2, 1, 1, 1, 2, 1, 1, 1, 2, 2, 2, 1, 1, 2, 1, 1

				// This simplified sequence also crashes/crashed
				// 16x "1", merge happens after 15!
				1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
		};

		for (int i = 0; i < clientUpSequence.length; i++) {
			if (clientUpSequence[i] == 1) {
				clientA.down();

				clientA.createNewFile("A-file" + i + ".jpg", i);
				clientA.up(upOperationOptionsNoCleanup);
			}
			else {
				clientB.down();

				clientB.createNewFile("B-file" + i + ".jpg", i);
				clientB.up(upOperationOptionsNoCleanup);
			}
		}

		clientA.cleanup();

		clientA.down();
		clientB.down();

		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());

		// Make sure the "merge" process actually happened
		assertFalse(new File(testConnection.getPath(), "databases/database-A-0000000001").exists());
		assertFalse(new File(testConnection.getPath(), "databases/database-A-0000000005").exists());
		assertFalse(new File(testConnection.getPath(), "databases/database-A-0000000010").exists());
		assertFalse(new File(testConnection.getPath(), "databases/database-A-0000000030").exists());
		assertFalse(new File(testConnection.getPath(), "databases/database-A-0000000031").exists());
		assertTrue(new File(testConnection.getPath(), "databases/database-A-0000000032").exists());

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
		 * This is the attempt to reproduce issue #58 https://github.com/syncany/syncany/issues/58
		 */

		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);

		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile());
		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile());

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setRemoveOldVersions(true);
		options.setKeepVersionsCount(5);
		options.setMinSecondsBetweenCleanups(0);
		options.setMaxDatabaseFiles(7);

		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);

		UpOperationOptions upOperationOptionsWithCleanupForce = new UpOperationOptions();
		upOperationOptionsWithCleanupForce.setStatusOptions(statusOptionsForceChecksum);
		upOperationOptionsWithCleanupForce.setForceUploadEnabled(true);

		// Run preparations

		clientA.down();
		clientA.createNewFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A1)

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A2)

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A2,B1)

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A3,B1)

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A4,B1)

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A4,B2) + (A4,B3) [PURGE]
		clientB.cleanup(options);

		clientC.down();

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A5,B3) + (A6,B3) [PURGE]
		clientA.cleanup(options);

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A6,B4) + (A6,B5) [PURGE]
		clientB.cleanup(options);

		/*
		 * For some reason, this chunk checksum in the following commit is the reason for the exception. So we record it here to see where it vanishes
		 * from the databases.
		 */

		clientA.down();
		clientA.changeFile("A-file.jpg");

		String fileAndChunkChecksumThatRaisesException = StringUtil.toHex(TestFileUtil.createChecksum(clientA.getLocalFile("A-file.jpg")));
		System.out.println("Chunk/File checksum that raises the issue: " + fileAndChunkChecksumThatRaisesException);

		clientA.createNewFile("ADDED_IN_DBV_A7_B5");
		clientA.up(upOperationOptionsWithCleanupForce); // (A7,B5) + (A8,B5) [PURGE]
		clientA.cleanup(options);
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException + "'",
				databaseConnectionA));

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A8,B6) + (A8,B7) [PURGE]
		clientB.cleanup(options);
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException + "'",
				databaseConnectionB));

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A9,B7) + (A10,B7) [PURGE]
		clientA.cleanup(options);
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException + "'",
				databaseConnectionA));

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A10,B8) + (A10,B9) [PURGE]
		clientB.cleanup(options);
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException + "'",
				databaseConnectionB));

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A10,B10) + (A10,B11) [PURGE]
		clientB.cleanup(options);
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException + "'",
				databaseConnectionB));

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A11,B11) + (A12,B11) [PURGE]
		clientA.cleanup(options);

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A13,B11) + (A14,B11) [PURGE]
		clientA.cleanup(options);

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A14,B12) + (A14,B13) [PURGE]
		clientB.cleanup(options);

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A15,B13) + (A16,B13) [PURGE]
		clientA.cleanup(options);

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A17,B13) + (A18,B13) [PURGE]
		clientA.cleanup(options);

		// Sync them up
		clientA.down();

		clientB.down();

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
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);

		CleanupOperationOptions cleanupOptionsKeep1 = new CleanupOperationOptions();
		cleanupOptionsKeep1.setRemoveOldVersions(true);
		cleanupOptionsKeep1.setKeepVersionsCount(1);

		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);

		UpOperationOptions upNoCleanupForceChecksum = new UpOperationOptions();
		upNoCleanupForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		UpOperationOptions upWithCleanupKeep1ForceChecksum = new UpOperationOptions();
		upWithCleanupKeep1ForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		clientA.createNewFile("fileA");
		clientB.createNewFile("fileB");

		clientA.up(upNoCleanupForceChecksum);
		clientB.down();

		TestFileUtil.copyFile(clientA.getLocalFile("fileA"), clientB.getLocalFile("fileB"));
		String problemChecksum = StringUtil.toHex(FileUtil.createChecksum(clientA.getLocalFile("fileA"), "SHA1"));
		clientB.up(upNoCleanupForceChecksum);

		for (int i = 0; i < 20; i++) {
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
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);

		CleanupOperationOptions cleanupOptionsKeep1 = new CleanupOperationOptions();
		cleanupOptionsKeep1.setRemoveOldVersions(true);
		cleanupOptionsKeep1.setKeepVersionsCount(1);

		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);

		UpOperationOptions upNoCleanupForceChecksum = new UpOperationOptions();
		upNoCleanupForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		UpOperationOptions upWithCleanupKeep1ForceChecksum = new UpOperationOptions();
		upWithCleanupKeep1ForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		clientB.createNewFile("fileB");
		clientB.up(upNoCleanupForceChecksum);

		clientA.down();
		TestFileUtil.copyFile(clientB.getLocalFile("fileB"), clientA.getLocalFile("fileBcopy"));
		clientA.up(upNoCleanupForceChecksum);

		for (int i = 0; i < 30; i++) {
			clientB.down();
			clientB.changeFile("fileB");
			clientB.up(upNoCleanupForceChecksum);
		}

		FileUtils.copyDirectory(testConnection.getPath(), new File(testConnection.getPath() + "_1_before_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "1_before_cleanup"));

		CleanupOperationOptions cleanupMergeAndRemoveOldFiles = new CleanupOperationOptions();
		cleanupMergeAndRemoveOldFiles.setRemoveOldVersions(true);
		clientB.cleanup(cleanupMergeAndRemoveOldFiles);

		FileUtils.copyDirectory(testConnection.getPath(), new File(testConnection.getPath() + "_2_after_cleanup"));
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
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		TestClient clientD = new TestClient("D", testConnection);
		TestClient clientE = new TestClient("E", testConnection);

		CleanupOperationOptions cleanupOptionsKeep1 = new CleanupOperationOptions();
		cleanupOptionsKeep1.setRemoveOldVersions(true);
		cleanupOptionsKeep1.setKeepVersionsCount(1);

		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);

		UpOperationOptions upNoCleanupForceChecksum = new UpOperationOptions();
		upNoCleanupForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		UpOperationOptions upWithCleanupKeep1ForceChecksum = new UpOperationOptions();
		upWithCleanupKeep1ForceChecksum.setStatusOptions(statusOptionsForceChecksum);

		// First round

		clientB.createNewFile("fileA");
		clientB.up(upNoCleanupForceChecksum);

		clientA.down();
		TestFileUtil.copyFile(clientA.getLocalFile("fileA"), clientA.getLocalFile("fileAcopy"));
		clientA.up(upNoCleanupForceChecksum);

		clientA.down();

		for (int i = 0; i < 30; i++) {
			clientA.down();
			clientA.changeFile("fileA");
			clientA.up(upNoCleanupForceChecksum);
		}

		// First cleanup

		FileUtils.copyDirectory(testConnection.getPath(), new File(testConnection.getPath() + "_1_before_cleanup"));
		FileUtils.copyDirectory(clientA.getConfig().getDatabaseDir(), new File(clientA.getConfig().getAppDir(), "1_before_cleanup"));

		CleanupOperationOptions cleanupMergeAndRemoveOldFiles = new CleanupOperationOptions();
		cleanupMergeAndRemoveOldFiles.setRemoveOldVersions(true);
		clientA.cleanup(cleanupMergeAndRemoveOldFiles);

		FileUtils.copyDirectory(testConnection.getPath(), new File(testConnection.getPath() + "_2_after_cleanup"));
		FileUtils.copyDirectory(clientA.getConfig().getDatabaseDir(), new File(clientA.getConfig().getAppDir(), "2_after_cleanup"));

		clientC.down(); // If this doesn't crash that's a win!

		// Second round

		for (int i = 0; i < 30; i++) {
			clientB.down();
			clientB.changeFile("fileA");
			clientB.up(upNoCleanupForceChecksum);
		}

		// Second cleanup

		FileUtils.copyDirectory(testConnection.getPath(), new File(testConnection.getPath() + "_3_before_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "3_before_cleanup"));

		clientB.cleanup(cleanupMergeAndRemoveOldFiles);

		FileUtils.copyDirectory(testConnection.getPath(), new File(testConnection.getPath() + "_4_after_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "4_after_cleanup"));

		clientD.down(); // If this doesn't crash that's a win!

		// Third round

		for (int i = 0; i < 30; i++) {
			clientB.down();
			clientB.changeFile("fileA");
			clientB.up(upNoCleanupForceChecksum);
		}

		clientB.deleteFile("fileAcopy"); // < Remove original checksum from first DBV
		clientB.up(upNoCleanupForceChecksum);

		// Third cleanup

		FileUtils.copyDirectory(testConnection.getPath(), new File(testConnection.getPath() + "_5_before_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "5_before_cleanup"));

		clientB.cleanup(cleanupMergeAndRemoveOldFiles);

		FileUtils.copyDirectory(testConnection.getPath(), new File(testConnection.getPath() + "_6_after_cleanup"));
		FileUtils.copyDirectory(clientB.getConfig().getDatabaseDir(), new File(clientB.getConfig().getAppDir(), "6_after_cleanup"));

		clientE.down(); // If this doesn't crash that's a win!

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}

	@Test
	public void testIssue266_EmptyDatabaseAfterCleanup() throws Exception {
		// Test for https://github.com/syncany/syncany/issues/266#issuecomment-64472059

		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		TestClient clientD = new TestClient("D", testConnection);
		TestClient clientE = new TestClient("E", testConnection);

		CleanupOperationOptions cleanupOptionsKeepOneForce = new CleanupOperationOptions();
		cleanupOptionsKeepOneForce.setRemoveOldVersions(true);
		cleanupOptionsKeepOneForce.setKeepVersionsCount(1);
		cleanupOptionsKeepOneForce.setForce(true);

		// Create a couple of files, then delete them and do a cleanup

		clientA.createNewFile("fileA");
		clientA.upWithForceChecksum();

		clientB.down();
		clientB.createNewFile("fileB");
		clientB.upWithForceChecksum();

		clientC.down();
		clientC.createNewFile("fileC");
		clientC.upWithForceChecksum();

		clientD.down();
		clientD.deleteFile("fileA");
		clientD.deleteFile("fileB");
		clientD.deleteFile("fileC");
		clientD.upWithForceChecksum();
		clientD.cleanup(cleanupOptionsKeepOneForce);

		java.sql.Connection databaseConnectionD = DatabaseConnectionFactory.createConnection(clientD.getDatabaseFile());
		assertEquals("A,2\nB,2\nC,2\nD,2",
				TestSqlUtil.runSqlSelect("select client, filenumber from known_databases order by client, filenumber", databaseConnectionD));
		assertEquals("", TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion", databaseConnectionD));

		// Now the remote databases are completely empty (no files, no histories, no database versions!)

		/* Case 1: 
		 * 
		 * Client A now knows "fileA" and must react on the cleanup by client D.
		 * The remote databases do NOT contain any trace of "fileA" anymore, so
		 * client A has to detect the deletion by comparing the local database with 
		 * the winner database. "fileA" should be deleted after the next 'down'.
		 */

		clientA.down(); // Existing client  << This created a NullPointerException in #266
		assertFalse("File 'fileA' should have been deleted.", clientA.getLocalFile("fileA").exists());
		assertFalse("File 'fileB' should not have been created.", clientA.getLocalFile("fileB").exists());
		assertFalse("File 'fileC' should not have been created.", clientA.getLocalFile("fileC").exists());
		assertConflictingFileNotExists("fileA", clientA.getLocalFiles());
		assertConflictingFileNotExists("fileB", clientA.getLocalFiles());
		assertConflictingFileNotExists("fileC", clientA.getLocalFiles());
		assertSqlDatabaseEquals(clientD.getDatabaseFile(), clientA.getDatabaseFile());
		assertFileListEquals(clientD.getLocalFiles(), clientA.getLocalFiles());

		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile());
		assertEquals("A,2\nB,2\nC,2\nD,2",
				TestSqlUtil.runSqlSelect("select client, filenumber from known_databases order by client, filenumber", databaseConnectionA));

		/*
		 * Case 2:
		 * 
		 * Client E is a completely new client. It's the first time downloading anything, so
		 * it has no local database, and (in this case), the remote/winner database is completely
		 * empty!
		 */

		clientE.down(); // Empty/new client << This created a NullPointerException 
		assertFalse("File 'fileA' should not have been created.", clientE.getLocalFile("fileA").exists());
		assertFalse("File 'fileB' should not have been created.", clientE.getLocalFile("fileB").exists());
		assertFalse("File 'fileC' should not have been created.", clientE.getLocalFile("fileC").exists());
		assertConflictingFileNotExists("fileA", clientA.getLocalFiles());
		assertConflictingFileNotExists("fileB", clientA.getLocalFiles());
		assertConflictingFileNotExists("fileC", clientA.getLocalFiles());
		assertSqlDatabaseEquals(clientD.getDatabaseFile(), clientE.getDatabaseFile());
		assertFileListEquals(clientD.getLocalFiles(), clientE.getLocalFiles());

		java.sql.Connection databaseConnectionE = DatabaseConnectionFactory.createConnection(clientE.getDatabaseFile());
		assertEquals("A,2\nB,2\nC,2\nD,2",
				TestSqlUtil.runSqlSelect("select client, filenumber from known_databases order by client, filenumber", databaseConnectionE));

		// After a successful down, create a new database version (continue numbering!)

		clientA.createNewFile("fileA");
		UpOperationResult upResult = clientA.upWithForceChecksum();
		assertEquals(UpResultCode.OK_CHANGES_UPLOADED, upResult.getResultCode());
		assertEquals("(A3,B2,C2,D2)", TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion", databaseConnectionA));

		// Check if E applies everything correctly and check E's numbering

		clientE.down();
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientE.getDatabaseFile());
		assertFileListEquals(clientA.getLocalFiles(), clientE.getLocalFiles());
		assertEquals("A,2\nA,3\nB,2\nC,2\nD,2",
				TestSqlUtil.runSqlSelect("select client, filenumber from known_databases order by client, filenumber", databaseConnectionE));

		clientE.changeFile("fileA");
		upResult = clientE.upWithForceChecksum();
		assertEquals(UpResultCode.OK_CHANGES_UPLOADED, upResult.getResultCode());
		assertEquals("(A3,B2,C2,D2)\n(A3,B2,C2,D2,E1)",
				TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion", databaseConnectionE));

		// And with D ...

		clientD.down();
		assertSqlDatabaseEquals(clientE.getDatabaseFile(), clientD.getDatabaseFile());
		assertFileListEquals(clientE.getLocalFiles(), clientD.getLocalFiles());
		assertEquals(
				"A,2\nA,3\nB,2\nC,2\nD,2\nE,1",
				TestSqlUtil.runSqlSelect("select client, filenumber from known_databases order by client, filenumber", databaseConnectionD));
		assertEquals("(A3,B2,C2,D2)\n(A3,B2,C2,D2,E1)",
				TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion", databaseConnectionD));

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
		clientD.deleteTestData();
		clientE.deleteTestData();
	}

	@Test
	public void testDeleteFileAndCleanup() throws Exception {
		// Test if a deleted file is deleted remotely even after a cleanup

		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		CleanupOperationOptions cleanupOptionsKeepOneForce = new CleanupOperationOptions();
		cleanupOptionsKeepOneForce.setRemoveOldVersions(true);
		cleanupOptionsKeepOneForce.setKeepVersionsCount(1);
		cleanupOptionsKeepOneForce.setForce(true);

		// Create a couple of files, then delete them and do a cleanup

		clientA.createNewFile("fileA1");
		clientA.createNewFile("fileA2");
		clientA.upWithForceChecksum();

		clientB.down();
		clientB.deleteFile("fileA1");
		clientB.upWithForceChecksum();
		clientB.cleanup(cleanupOptionsKeepOneForce); // <<< This accidentally(?) deletes file histories marked DELETED

		clientA.down();
		assertFalse("Deleted file still exists.", clientA.getLocalFile("fileA1").exists());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
