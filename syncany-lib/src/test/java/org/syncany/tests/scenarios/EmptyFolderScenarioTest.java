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
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.SqlDatabase;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class EmptyFolderScenarioTest {
	@Test
	public void testEmptyFolderCreateAndSync() throws Exception {
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// Run 
		clientA.createNewFolder("A-folder1");
		clientA.createNewFolder("A-folder2");
		clientA.createNewFolder("A-folder3");
		clientA.up();		
		
		clientB.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		clientB.createNewFolder("B-folder4");
		clientB.createNewFolder("B-folder5");
		clientB.up();
		
		SqlDatabase databaseB = clientB.loadLocalDatabase();
		DatabaseVersionHeader beforeDatabaseVersionHeader = databaseB.getLastDatabaseVersionHeader();
		
		clientB.up(); // double-up, has caused problems
		
		DatabaseVersionHeader afterDatabaseVersionHeader = databaseB.getLastDatabaseVersionHeader();
		assertEquals("Nothing changed. Local database file should not change.", beforeDatabaseVersionHeader, afterDatabaseVersionHeader);
		
		clientA.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		Map<String, File> beforeSyncDownFileList = clientB.getLocalFilesExcludeLockedAndNoRead();
		clientA.down(); // double-down, has caused problems		
		assertFileListEquals("No change in file lists expected. Nothing changed", beforeSyncDownFileList, clientA.getLocalFilesExcludeLockedAndNoRead()); 				
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
