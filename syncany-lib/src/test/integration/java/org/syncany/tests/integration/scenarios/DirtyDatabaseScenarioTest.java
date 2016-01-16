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
import static org.syncany.tests.util.TestAssertUtil.assertConflictingFileExists;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.dao.ChunkSqlDao;
import org.syncany.database.dao.DatabaseVersionSqlDao;
import org.syncany.database.dao.FileContentSqlDao;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.database.dao.MultiChunkSqlDao;
import org.syncany.operations.ChangeSet;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.operations.status.StatusOperationResult;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestCollectionUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlUtil;

public class DirtyDatabaseScenarioTest {
	@Test
	public void testDirtyDatabase() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run
		UpOperationOptions upOptionsForceEnabled = new UpOperationOptions();
		upOptionsForceEnabled.setForceUploadEnabled(true);

		clientA.createNewFile("A-file1.jpg", 50 * 1024);
		clientA.up(upOptionsForceEnabled);

		clientB.createNewFile("A-file1.jpg", 51 * 1024);
		clientB.up(upOptionsForceEnabled);

		clientB.down(); // This creates a dirty database

		// Test (for dirty database existence)
		Config configB = clientB.getConfig();
		java.sql.Connection databaseConnectionB = configB.createDatabaseConnection();

		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnectionB);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnectionB);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnectionB);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnectionB, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnectionB);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnectionB, chunkDao, fileContentDao, fileVersionDao,
				fileHistoryDao, multiChunkDao);

		Iterator<DatabaseVersion> databaseVersionsDirtyB = databaseVersionDao.getDirtyDatabaseVersions();
		List<DatabaseVersion> databaseVersionsDirtyListB = TestCollectionUtil.toList(databaseVersionsDirtyB);

		assertEquals(1, databaseVersionsDirtyListB.size());

		DatabaseVersion dirtyDatabaseVersionB = databaseVersionsDirtyListB.get(0);
		assertNotNull(dirtyDatabaseVersionB);
		assertEquals(1, dirtyDatabaseVersionB.getFileHistories().size());

		PartialFileHistory fileHistoryFile1B = dirtyDatabaseVersionB.getFileHistories().iterator().next();
		assertNotNull(fileHistoryFile1B);
		assertEquals(1, fileHistoryFile1B.getFileVersions().size());
		assertEquals("A-file1.jpg", fileHistoryFile1B.getLastVersion().getPath());

		assertFileEquals(clientA.getLocalFile("A-file1.jpg"), clientB.getLocalFile("A-file1.jpg"));
		assertConflictingFileExists("A-file1.jpg", clientB.getLocalFilesExcludeLockedAndNoRead());

		// Run (part 2)
		clientB.up(); // This deletes the dirty database file

		Iterator<DatabaseVersion> databaseVersionsDirtyB2 = databaseVersionDao.getDirtyDatabaseVersions();
		List<DatabaseVersion> databaseVersionsDirtyListB2 = TestCollectionUtil.toList(databaseVersionsDirtyB2);

		assertEquals(0, databaseVersionsDirtyListB2.size());

		// Run (part 3)
		clientA.down(); // This pulls down the conflicting file
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		assertConflictingFileExists("A-file1.jpg", clientA.getLocalFilesExcludeLockedAndNoRead());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testDirtyCleanupDirty() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		TestClient clientD = new TestClient("D", testConnection);

		StatusOperationOptions statusOptions = new StatusOperationOptions();
		statusOptions.setForceChecksum(true);

		UpOperationOptions upOptionsForceEnabled = new UpOperationOptions();
		upOptionsForceEnabled.setStatusOptions(statusOptions);
		upOptionsForceEnabled.setForceUploadEnabled(true);

		CleanupOperationOptions cleanupOptions = new CleanupOperationOptions();
		cleanupOptions.setMinSecondsBetweenCleanups(0);
		cleanupOptions.setForce(true);

		// Run

		//// 1. CREATE FIRST DIRTY VERSION

		clientA.createNewFile("A-file1.jpg", 50 * 1024);
		clientA.up(upOptionsForceEnabled); // (A1)

		clientB.down();
		clientB.changeFile("A-file1.jpg");

		clientB.up(upOptionsForceEnabled); // (A1,B1)

		clientA.down();

		clientA.changeFile("A-file1.jpg"); // conflict (winner)
		clientA.up(upOptionsForceEnabled); // (A2,B1)

		clientB.changeFile("A-file1.jpg"); // conflict (loser)
		clientB.up(upOptionsForceEnabled);

		clientA.createNewFolder("new folder at A"); // don't care about the conflict, just continue
		clientA.up(upOptionsForceEnabled); // (A3,B1)

		clientB.createNewFolder("new folder at B"); // don't care about the conflict, just continue
		clientB.up(upOptionsForceEnabled);

		clientA.down(); // resolve conflict (wins, no DIRTY)

		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile(), false);
		assertEquals("0", TestSqlUtil.runSqlSelect("select count(*) from databaseversion where status='DIRTY'", databaseConnectionA));

		clientB.down(); // resolve conflict (loses, creates DIRTY version)

		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile(), false);
		assertEquals("2", TestSqlUtil.runSqlSelect("select count(*) from databaseversion where status='DIRTY'", databaseConnectionB));
		assertEquals("(A1,B2)\n(A1,B3)",
				TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion where status='DIRTY' order by id", databaseConnectionB));
		assertEquals("(A1)\n(A1,B1)\n(A2,B1)\n(A3,B1)",
				TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion where status<>'DIRTY' order by id", databaseConnectionB));

		StatusOperationResult statusResultBAfterDirty = clientB.status();
		assertNotNull(statusResultBAfterDirty);

		ChangeSet changeSetBAfterDirty = statusResultBAfterDirty.getChangeSet();
		assertEquals(2, changeSetBAfterDirty.getNewFiles().size());
		TestAssertUtil.assertConflictingFileExists("A-file1.jpg", clientB.getLocalFiles());

		clientB.up(upOptionsForceEnabled); // (A3,B2)
		assertEquals("0", TestSqlUtil.runSqlSelect("select count(*) from databaseversion where status='DIRTY'", databaseConnectionB));

		assertEquals("4", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA)); // (A1), (A1,B1), (A2,B1), (A3,B1)
		assertEquals("(A1)\n(A1,B1)\n(A2,B1)\n(A3,B1)",
				TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion order by id", databaseConnectionA));

		assertEquals("5", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionB));
		assertEquals("(A1)\n(A1,B1)\n(A2,B1)\n(A3,B1)\n(A3,B4)",
				TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion order by id", databaseConnectionB));

		//// 2. NOW THAT CLIENT B RESOLVED IT, A GETS DIRTY

		clientA.changeFile("A-file1.jpg"); // No 'down'! This version will become DIRTY
		clientA.createNewFile("dirty1");
		clientA.up(upOptionsForceEnabled);

		clientA.changeFile("A-file1.jpg"); // No 'down'! This version will become DIRTY
		clientA.createNewFile("dirty2");
		clientA.up(upOptionsForceEnabled);

		clientA.changeFile("A-file1.jpg"); // No 'down'! This version will become DIRTY
		clientA.createNewFile("dirty3");
		clientA.up(upOptionsForceEnabled);

		clientA.changeFile("A-file1.jpg"); // No 'down'! This version will become DIRTY
		clientA.createNewFile("dirty4");
		clientA.up(upOptionsForceEnabled);

		clientA.changeFile("A-file1.jpg"); // No 'down'! This version will become DIRTY
		clientA.createNewFile("dirty5");
		clientA.up(upOptionsForceEnabled);

		clientA.changeFile("A-file1.jpg"); // No 'down'! This version will become DIRTY
		clientA.createNewFile("dirty6");
		clientA.up(upOptionsForceEnabled);

		clientA.changeFile("A-file1.jpg"); // No 'down'! This version will become DIRTY
		clientA.createNewFile("dirty7");
		clientA.up(upOptionsForceEnabled);

		assertEquals("11", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));

		clientA.down();
		assertEquals("12", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));
		assertEquals("7", TestSqlUtil.runSqlSelect("select count(*) from databaseversion where status='DIRTY'", databaseConnectionA));
		assertEquals("5", TestSqlUtil.runSqlSelect("select count(*) from databaseversion where status<>'DIRTY'", databaseConnectionA));
		assertEquals("(A1)\n(A1,B1)\n(A2,B1)\n(A3,B1)\n(A3,B4)",
				TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion where status<>'DIRTY' order by id", databaseConnectionA));

		clientB.down(); // Does nothing; A versions lose against (A3,B2) // same as above!
		assertEquals("5", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionB));
		assertEquals("(A1)\n(A1,B1)\n(A2,B1)\n(A3,B1)\n(A3,B4)",
				TestSqlUtil.runSqlSelect("select vectorclock_serialized from databaseversion order by id", databaseConnectionB));

		//// 3. NEW CLIENT JOINS

		clientC.down();
		TestAssertUtil.assertSqlDatabaseEquals(clientB.getDatabaseFile(), clientC.getDatabaseFile());

		//// 4. FORCE MERGE DATABASES ON CLIENT A

		clientA.deleteFile("dirty1");
		clientA.deleteFile("dirty2");

		clientA.up(upOptionsForceEnabled); // upload DIRTY version
		assertEquals("6", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));
		assertEquals("0", TestSqlUtil.runSqlSelect("select count(*) from databaseversion where status='DIRTY'", databaseConnectionA));
		assertEquals("6", TestSqlUtil.runSqlSelect("select count(*) from databaseversion where status<>'DIRTY'", databaseConnectionA));

		clientA.createNewFile("A-file2.jpg");

		int cleanupEveryXUps = 7; // For every X up's call 'cleanup' ("X" is larger than the max. length of file versions in a history)

		for (int i = 1; i <= 21; i++) {
			clientA.changeFile("A-file2.jpg");

			clientA.up(upOptionsForceEnabled);

			if (i % cleanupEveryXUps == 0) {
				clientA.cleanup(cleanupOptions);
			}
		}

		clientA.cleanup(cleanupOptions);

		clientB.down();
		clientC.down();
		clientD.down();

		TestAssertUtil.assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		TestAssertUtil.assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientC.getDatabaseFile());
		TestAssertUtil.assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientD.getDatabaseFile());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
		clientD.deleteTestData();
	}
}
