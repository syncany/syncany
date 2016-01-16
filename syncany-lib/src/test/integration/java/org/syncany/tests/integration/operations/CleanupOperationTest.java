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
package org.syncany.tests.integration.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.TreeMap;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.cleanup.CleanupOperationOptions.TimeUnit;
import org.syncany.operations.cleanup.CleanupOperationResult;
import org.syncany.operations.cleanup.CleanupOperationResult.CleanupResultCode;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.down.DownOperationResult.DownResultCode;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlUtil;

public class CleanupOperationTest {
	static {
		Logging.init();
	}

	@Test
	public void testEasyCleanup() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setRemoveOldVersions(true);
		options.setMinKeepSeconds(0);

		// Run

		// A: Create some file versions
		clientA.createNewFile("someotherfile.jpg"); // These two files' chunks will be in one multichunk
		clientA.createNewFile("file.jpg"); // Only one of the chunks will be needed after cleanup!
											// The multichunk will be 50% useless
		for (int i = 1; i <= 4; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		clientA.createNewFile("otherfile.txt");
		for (int i = 1; i <= 3; i++) {
			clientA.changeFile("otherfile.txt");
			clientA.upWithForceChecksum();
		}

		clientA.createNewFile("deletedfile.txt");
		for (int i = 1; i <= 3; i++) {
			clientA.changeFile("deletedfile.txt");
			clientA.upWithForceChecksum();
		}
		clientA.deleteFile("deletedfile.txt");
		clientA.upWithForceChecksum();

		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile(), false);
		assertEquals("12", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionA));
		assertEquals("11", TestSqlUtil.runSqlSelect("select count(*) from chunk", databaseConnectionA));
		assertEquals("10", TestSqlUtil.runSqlSelect("select count(*) from multichunk", databaseConnectionA));
		assertEquals("11", TestSqlUtil.runSqlSelect("select count(*) from filecontent", databaseConnectionA));
		assertEquals("4", TestSqlUtil.runSqlSelect("select count(distinct id) from filehistory", databaseConnectionA));

		// B: Sync down by other client
		clientB.down();

		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile(), false);
		assertEquals("12", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionB));
		assertEquals("11", TestSqlUtil.runSqlSelect("select count(*) from chunk", databaseConnectionB));
		assertEquals("10", TestSqlUtil.runSqlSelect("select count(*) from multichunk", databaseConnectionB));
		assertEquals("11", TestSqlUtil.runSqlSelect("select count(*) from filecontent", databaseConnectionB));
		assertEquals("4", TestSqlUtil.runSqlSelect("select count(distinct id) from filehistory", databaseConnectionB));

		// A: Cleanup this mess (except for two) <<<< This is the interesting part!!! <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
		CleanupOperationResult cleanupOperationResult = clientA.cleanup(options);
		assertEquals(CleanupResultCode.OK, cleanupOperationResult.getResultCode());
		assertEquals(11, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(7, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(4, cleanupOperationResult.getRemovedOldVersionsCount());

		// 1 version for "file.jpg", 1 versions for "otherfile.txt" and 1 version for "someotherfile.jpg"
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionA));
		assertEquals("4", TestSqlUtil.runSqlSelect("select sum(version) from fileversion where path='file.jpg'", databaseConnectionA)); // 4
		assertEquals("3", TestSqlUtil.runSqlSelect("select sum(version) from fileversion where path='otherfile.txt'", databaseConnectionA)); // 3
		assertEquals("1", TestSqlUtil.runSqlSelect("select sum(version) from fileversion where path='someotherfile.jpg'", databaseConnectionA));

		// 3 chunks remain; one was obsolete so we removed it!
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(*) from chunk", databaseConnectionA));

		// 3 chunks in 3 multichunks
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(*) from multichunk", databaseConnectionA));
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(*) from filecontent", databaseConnectionA));
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(distinct id) from filehistory", databaseConnectionA));

		// Test the repo
		assertEquals(3, new File(testConnection.getPath() + "/multichunks/").list().length);
		assertEquals(1, new File(testConnection.getPath() + "/databases/").list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("database-");
			}
		}).length);

		// B: Sync down cleanup
		clientB.down();
		TestAssertUtil.assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testCleanupFailsBecauseOfLocalChanges() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		StatusOperationOptions statusOptions = new StatusOperationOptions();
		statusOptions.setForceChecksum(true);

		CleanupOperationOptions cleanupOptions = new CleanupOperationOptions();
		cleanupOptions.setStatusOptions(statusOptions);
		cleanupOptions.setRemoveOldVersions(true);

		// Run

		// A: Create some file versions
		clientA.createNewFile("file.jpg");
		for (int i = 1; i <= 4; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		// B: Sync down, add something
		clientB.down();

		clientB.changeFile("file.jpg");

		CleanupOperationResult cleanupOperationResult = clientB.cleanup(cleanupOptions);
		assertEquals(CleanupResultCode.NOK_LOCAL_CHANGES, cleanupOperationResult.getResultCode());
		assertEquals(0, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(0, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(0, cleanupOperationResult.getRemovedOldVersionsCount());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testCleanupFailsBecauseOfRemoteChanges() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setRemoveOldVersions(true);

		// Run

		// A: Create some file versions
		clientA.createNewFile("file.jpg");
		for (int i = 1; i <= 4; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		// B: Sync down, add something
		clientB.down();

		// A: Add something
		clientA.changeFile("file.jpg");
		clientA.upWithForceChecksum();

		// B: Cleanup
		CleanupOperationResult cleanupOperationResult = clientB.cleanup(options);
		assertEquals(CleanupResultCode.NOK_REMOTE_CHANGES, cleanupOperationResult.getResultCode());
		assertEquals(0, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(0, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(0, cleanupOperationResult.getRemovedOldVersionsCount());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testCleanupNoChanges() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setRemoveOldVersions(true);
		options.setPurgeFileVersionSettings(new TreeMap<Long, TimeUnit>());

		// Run

		// A: Create some file versions
		clientA.createNewFile("file.jpg");
		for (int i = 1; i <= 4; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		// B: Sync down, add something
		clientB.down();

		// B: Cleanup
		CleanupOperationResult cleanupOperationResult = clientB.cleanup(options);
		assertEquals(CleanupResultCode.OK_NOTHING_DONE, cleanupOperationResult.getResultCode());
		assertEquals(0, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(0, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(0, cleanupOperationResult.getRemovedOldVersionsCount());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testCleanupManyUpsAfterCleanup() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setRemoveOldVersions(true);
		options.setMinKeepSeconds(0);

		// Run

		// A: Create some file versions
		clientA.createNewFile("file.jpg");
		for (int i = 1; i <= 4; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		// B: Sync down
		clientB.down();

		// A: Cleanup
		CleanupOperationResult cleanupOperationResult = clientA.cleanup(options);
		assertEquals(CleanupResultCode.OK, cleanupOperationResult.getResultCode());
		assertEquals(4, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(3, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(1, cleanupOperationResult.getRemovedOldVersionsCount());

		// A: Continue to upload stuff ! <<<<<<<<<<<<<<<<<<<<<
		for (int i = 1; i <= 4; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		clientA.createNewFile("file2.jpg");
		for (int i = 1; i <= 4; i++) {
			clientA.changeFile("file2.jpg");
			clientA.upWithForceChecksum();
		}

		// B: Sync down
		clientB.down();
		TestAssertUtil.assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testCleanupNoChangeBecauseDirty() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		CleanupOperationOptions removeOldCleanupOperationOptions = new CleanupOperationOptions();
		removeOldCleanupOperationOptions.setRemoveOldVersions(true);

		StatusOperationOptions forceChecksumStatusOperationOptions = new StatusOperationOptions();
		forceChecksumStatusOperationOptions.setForceChecksum(true);

		UpOperationOptions noCleanupAndForceUpOperationOptions = new UpOperationOptions();
		noCleanupAndForceUpOperationOptions.setForceUploadEnabled(true);
		noCleanupAndForceUpOperationOptions.setStatusOptions(forceChecksumStatusOperationOptions);

		// Run

		// A: Create some file versions
		clientA.createNewFile("file.jpg");
		for (int i = 1; i <= 4; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		// B: Sync down, add something
		clientB.down();

		// A: Change file.jpg (first step in creating a conflict)
		clientA.changeFile("file.jpg");
		clientA.up(noCleanupAndForceUpOperationOptions);

		// B: Change file.jpg (second step in creating a conflict)
		clientB.changeFile("file.jpg");
		clientB.up(noCleanupAndForceUpOperationOptions); // << creates conflict

		// B: Sync down (creates a local conflict file and marks local changes as DRITY)
		clientB.down(); // << creates DIRTY database entries

		// B: Cleanup
		CleanupOperationResult cleanupOperationResult = clientB.cleanup(removeOldCleanupOperationOptions);
		assertEquals(CleanupResultCode.NOK_DIRTY_LOCAL, cleanupOperationResult.getResultCode());
		assertEquals(0, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(0, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(0, cleanupOperationResult.getRemovedOldVersionsCount());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testCleanupAfterFailedUpOperation() throws Exception {
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(Arrays.asList(new String[] {
				// List of failing operations (regex)
				// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

				"rel=[456].+upload.+multichunk" // << 3 retries!
		}));

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		StatusOperationOptions forceChecksumStatusOperationOptions = new StatusOperationOptions();
		forceChecksumStatusOperationOptions.setForceChecksum(true);

		UpOperationOptions noCleanupAndForceUpOperationOptions = new UpOperationOptions();
		noCleanupAndForceUpOperationOptions.setForceUploadEnabled(true);
		noCleanupAndForceUpOperationOptions.setStatusOptions(forceChecksumStatusOperationOptions);

		// Run

		// 1. Call A.up(); this fails AFTER the first multichunk
		clientA.createNewFile("A-file1", 5 * 1024 * 1024);
		boolean operationFailed = false;

		try {
			clientA.up();
		}
		catch (Exception e) {
			operationFailed = true; // That is supposed to happen!
		}

		File repoMultiChunkDir = new File(testConnection.getPath() + "/multichunks");
		File repoActionsDir = new File(testConnection.getPath() + "/actions");

		assertTrue(operationFailed);
		// Atomic operation, so multichunk is not yet present at location
		assertEquals(0, repoMultiChunkDir.listFiles().length);
		assertEquals(1, repoActionsDir.listFiles().length);

		// 2. Call A.cleanup(); this does not run, because there are local changes
		CleanupOperationResult cleanupOperationResultA = clientA.cleanup();
		assertEquals(CleanupResultCode.NOK_LOCAL_CHANGES, cleanupOperationResultA.getResultCode());

		// 3. Call B.cleanup(); this does not run, because of the leftover 'action' file
		CleanupOperationResult cleanupOperationResultB = clientB.cleanup();
		assertEquals(CleanupResultCode.NOK_OTHER_OPERATIONS_RUNNING, cleanupOperationResultB.getResultCode());

		// 4. Call B.down(); this does not deliver any results, because no databases have been uploaded
		DownOperationResult downOperationResult = clientB.down();
		assertEquals(DownResultCode.OK_NO_REMOTE_CHANGES, downOperationResult.getResultCode());

		// 5. Call 'up' again, this uploads previously crashed stuff, and then runs cleanup.
		// The cleanup then removes the old multichunk and the old action files.

		UpOperationResult secondUpResult = clientA.up();
		assertEquals(UpResultCode.OK_CHANGES_UPLOADED, secondUpResult.getResultCode());
		assertEquals(2, repoMultiChunkDir.listFiles().length);
		assertEquals(0, repoActionsDir.listFiles().length);

		// 6. Call 'cleanup' manually (Nothing happens, since transaction was cleaned on second up)
		CleanupOperationResult cleanupOperationResult = clientA.cleanup();
		assertEquals(CleanupOperationResult.CleanupResultCode.OK_NOTHING_DONE, cleanupOperationResult.getResultCode());
		assertEquals(0, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(0, repoActionsDir.listFiles().length);

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testCleanupMaxDatabaseFiles() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setMinSecondsBetweenCleanups(0);
		options.setPurgeFileVersionSettings(new TreeMap<Long, TimeUnit>());
		options.setRemoveOldVersions(true);
		options.setMaxDatabaseFiles(3);

		// Run

		// A: Create some file versions
		clientA.createNewFile("file.jpg");
		for (int i = 1; i <= 4; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		// B: Cleanup
		CleanupOperationResult cleanupOperationResult = clientA.cleanup(options);
		assertEquals(CleanupResultCode.OK, cleanupOperationResult.getResultCode());
		assertEquals(4, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(0, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(0, cleanupOperationResult.getRemovedOldVersionsCount());

		TestClient clientB = new TestClient("B", testConnection);
		clientB.down();

		// B: Create some file versions
		clientB.createNewFile("file-B.jpg");
		for (int i = 1; i <= 6; i++) {
			clientB.changeFile("file-B.jpg");
			clientB.upWithForceChecksum();
		}

		// B: Cleanup (2 clients, so 7 databases is too much)
		cleanupOperationResult = clientB.cleanup(options);
		assertEquals(CleanupResultCode.OK, cleanupOperationResult.getResultCode());
		assertEquals(7, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(0, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(0, cleanupOperationResult.getRemovedOldVersionsCount());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testQuickDoubleCleanup() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setRemoveOldVersions(false);
		options.setRemoveVersionsByInterval(false);
		options.setMinSecondsBetweenCleanups(40000000);

		// Run

		// A: Create some file versions
		clientA.createNewFile("file.jpg");
		for (int i = 1; i <= 16; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		// B: Cleanup
		CleanupOperationResult cleanupOperationResult = clientA.cleanup(options);
		assertEquals(CleanupResultCode.OK, cleanupOperationResult.getResultCode());
		assertEquals(16, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(0, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(0, cleanupOperationResult.getRemovedOldVersionsCount());

		for (int i = 1; i <= 15; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();
		}

		// Because of minimum timeout, this cleanup should not do anything
		cleanupOperationResult = clientA.cleanup(options);
		assertEquals(CleanupResultCode.NOK_RECENTLY_CLEANED, cleanupOperationResult.getResultCode());

		// When force is on, the cleanup should go through
		options.setForce(true);

		cleanupOperationResult = clientA.cleanup(options);
		assertEquals(CleanupResultCode.OK, cleanupOperationResult.getResultCode());
		assertEquals(16, cleanupOperationResult.getMergedDatabaseFilesCount());
		assertEquals(0, cleanupOperationResult.getRemovedMultiChunksCount());
		assertEquals(0, cleanupOperationResult.getRemovedOldVersionsCount());

		// Tear down
		clientA.deleteTestData();
	}

	@Test
	public void testCleanupFailsMidCommit() throws Exception {
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(Arrays.asList(new String[] {
				// List of failing operations (regex)
				// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

				"rel=(13|14|15).+move" // << 3 retries!
		}));

		TestClient clientA = new TestClient("A", testConnection);
		java.sql.Connection databaseConnectionA = clientA.getConfig().createDatabaseConnection();

		StatusOperationOptions forceChecksumStatusOperationOptions = new StatusOperationOptions();
		forceChecksumStatusOperationOptions.setForceChecksum(true);

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setStatusOptions(forceChecksumStatusOperationOptions);
		options.setRemoveOldVersions(true);
		options.setMinKeepSeconds(0);
		options.setMinSecondsBetweenCleanups(40000000);
		options.setForce(true);

		File repoDir = testConnection.getPath();
		File repoMultiChunkDir = new File(testConnection.getPath() + "/multichunks");
		File repoActionsDir = new File(testConnection.getPath() + "/actions");
		File repoDatabasesDir = new File(testConnection.getPath() + "/databases");
		File repoTransactionsDir = new File(testConnection.getPath() + "/transactions");
		File repoTemporaryDir = new File(testConnection.getPath() + "/temporary");

		// Run

		clientA.createNewFile("A-file1", 5 * 1024);
		clientA.up();

		for (int i = 0; i < 5; i++) {
			clientA.changeFile("A-file1");
			clientA.upWithForceChecksum();
		}

		assertEquals(6, repoDatabasesDir.listFiles().length);
		assertEquals(6, repoMultiChunkDir.listFiles().length);
		assertEquals(0, repoActionsDir.listFiles().length);
		assertEquals("6", TestSqlUtil.runSqlSelect("select count(*) from multichunk", databaseConnectionA));

		// Run cleanup, fails mid-move!
		boolean operationFailed = false;

		try {
			clientA.cleanup(options);
		}
		catch (Exception e) {
			operationFailed = true; // That is supposed to happen!
			e.printStackTrace();
		}

		assertTrue(operationFailed);
		assertEquals(1, repoTransactionsDir.listFiles().length);
		assertEquals(0, repoTemporaryDir.listFiles().length);
		assertEquals(6, repoDatabasesDir.listFiles().length);
		assertEquals(6, repoMultiChunkDir.listFiles().length);
		assertEquals("6", TestSqlUtil.runSqlSelect("select count(*) from multichunk", databaseConnectionA));

		// Retry
		clientA.cleanup(options);

		assertEquals(1, repoDatabasesDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("database-");
			}
		}).length);
		assertEquals(1, repoMultiChunkDir.listFiles().length);
		assertEquals(0, repoActionsDir.listFiles().length);
		assertEquals(0, repoDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("transaction-");
			}
		}).length);
		assertEquals(0, repoDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("temp-");
			}
		}).length);
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from multichunk", databaseConnectionA));

		// Tear down
		clientA.deleteTestData();
	}
	
	@Test
	public void testFullyDeletingDeletedFiles() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		java.sql.Connection databaseConnectionA = clientA.getConfig().createDatabaseConnection();

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setRemoveOldVersions(true);
		options.setMinSecondsBetweenCleanups(0);
		options.setPurgeFileVersionSettings(new TreeMap<Long, TimeUnit>());
		options.setMinKeepSeconds(2);
		
		clientA.createNewFile("file.jpg");
		clientA.up();
		clientA.deleteFile("file.jpg");
		clientA.up();
		clientA.cleanup(options);
		assertEquals("2", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionA));

		Thread.sleep(3000);

		clientA.cleanup(options);
		assertEquals("0", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionA));

		// Tear down
		clientA.deleteTestData();
	}

	@Test
	public void testDefaultFileVersionDeletion() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		java.sql.Connection databaseConnectionA = clientA.getConfig().createDatabaseConnection();

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setRemoveOldVersions(true);
		options.setMinSecondsBetweenCleanups(0);

		// More than a month back
		clientA.createNewFile("file.jpg", 1024);
		clientA.upWithForceChecksum();
		// Less than a month back
		clientA.changeFile("file.jpg");
		clientA.upWithForceChecksum();		
		// Less than a month back, same day as above
		clientA.changeFile("file.jpg");
		clientA.upWithForceChecksum();
		
		// Less than 3 days back
		clientA.changeFile("file.jpg");
		clientA.upWithForceChecksum();
		// Less than 3 days back, same hour as above
		clientA.changeFile("file.jpg");
		clientA.upWithForceChecksum();
		
		// Less than 1 hour back
		clientA.changeFile("file.jpg");
		clientA.upWithForceChecksum();
		// Less than 1 hour back, same minute
		clientA.changeFile("file.jpg");
		clientA.upWithForceChecksum();

		long curTime = System.currentTimeMillis() / 1000L;
		
		long[] times = new long[]{curTime - 31L*24L*3600L, 
				curTime - 20L * 24L * 3600L - 1L, curTime - 20L * 24L * 3600L,
				curTime - 24 * 3600L - 1L, curTime - 24L * 3600L,
				curTime - 500L, curTime - 499L
		};

		int i = 0;
		try (PreparedStatement preparedStatement = databaseConnectionA.prepareStatement("select * from fileversion")) {
			ResultSet res = preparedStatement.executeQuery();
			while (res.next()) {
				int version = res.getInt("version");
				try (PreparedStatement preparedUpdate = databaseConnectionA.prepareStatement("update fileversion set updated = ? where version = ?")) {
					System.out.println(new Timestamp(times[i] * 1000L));
					preparedUpdate.setTimestamp(1, new Timestamp(times[i] * 1000L));
					preparedUpdate.setInt(2, version);
					assertEquals(1, preparedUpdate.executeUpdate());
				}
				i++;
			}
		}

		databaseConnectionA.commit();
		assertEquals("7", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionA));

		clientA.cleanup(options);
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionA));
		assertEquals("3\n5\n7", TestSqlUtil.runSqlSelect("select version from fileversion", databaseConnectionA));

		// Tear down
		clientA.deleteTestData();
	}

	@Test
	public void testFullFileVersionDeletion() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		java.sql.Connection databaseConnectionA = clientA.getConfig().createDatabaseConnection();

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setRemoveOldVersions(true);
		options.setPurgeFileVersionSettings(new TreeMap<Long, TimeUnit>());
		options.setMinKeepSeconds(0);
		options.setMinSecondsBetweenCleanups(0);

		// More than a month back
		clientA.createNewFile("file.jpg", 1024);
		clientA.upWithForceChecksum();
		// Less than a month back
		clientA.changeFile("file.jpg");
		clientA.upWithForceChecksum();

		clientA.cleanup(options);
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionA));

		// Tear down
		clientA.deleteTestData();
	}
}
