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
import static org.junit.Assert.assertNotNull;
import static org.syncany.tests.util.TestAssertUtil.assertConflictingFileExists;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.dao.ChunkSqlDao;
import org.syncany.database.dao.DatabaseVersionSqlDao;
import org.syncany.database.dao.FileContentSqlDao;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.database.dao.MultiChunkSqlDao;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestCollectionUtil;
import org.syncany.tests.util.TestConfigUtil;

public class DirtyDatabaseScenarioTest {
	@Test
	public void testDirtyDatabase() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// Run 
		UpOperationOptions upOptionsForceEnabled = new UpOperationOptions();
		upOptionsForceEnabled.setForceUploadEnabled(true);
		
		clientA.createNewFile("A-file1.jpg", 50*1024);
		clientA.up(upOptionsForceEnabled);
				
		clientB.createNewFile("A-file1.jpg", 51*1024);
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
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnectionB, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
		
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
				
		assertFileEquals("Files should be identical", clientA.getLocalFile("A-file1.jpg"), clientB.getLocalFile("A-file1.jpg"));
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
}
