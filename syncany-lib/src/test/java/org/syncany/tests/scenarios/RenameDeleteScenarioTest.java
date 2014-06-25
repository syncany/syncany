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

import static org.junit.Assert.assertFalse;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class RenameDeleteScenarioTest {	
	@Test
	public void testDeleteFileThatHasAlreadyMoved() throws Exception {
		// Scenario: A deletes a file that B has already moved
		
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// A new/up
		clientA.createNewFile("A-original");
		clientA.up();
		
		// B down/move/up
		clientB.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// A moves, and up
		clientA.deleteFile("A-original");
		clientA.up();
		
		// B deletes, then down; this should not fail or throw exceptions
		clientB.moveFile("A-original", "B-moved");
		clientB.down();
		assertFalse("File A-orginal should not be recreated.", clientB.getLocalFile("A-original").exists());

		// Sync them
		clientB.sync();		
		clientA.sync();
		assertFalse("File A-orginal should not be recreated.", clientA.getLocalFile("A-original").exists());
		assertFalse("File A-orginal should not be recreated.", clientB.getLocalFile("A-original").exists());
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());		
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
