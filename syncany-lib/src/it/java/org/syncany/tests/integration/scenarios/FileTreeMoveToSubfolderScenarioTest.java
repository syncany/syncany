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

import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class FileTreeMoveToSubfolderScenarioTest {
	@Test
	public void testCreateFileTreeAndMoveSubfolder() throws Exception {		
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
				
		// Run 
		clientA.createNewFolder("A-folder1");
		clientA.createNewFolder("A-folder1/A-subfolder1");
		clientA.createNewFolder("A-folder1/A-subfolder2");
		clientA.createNewFolder("A-folder1/A-subfolder2/A-subsubfolder1");
		clientA.createNewFolder("A-folder1/A-subfolder2/A-subsubfolder2");
		TestFileUtil.createRandomFilesInDirectory(clientA.getLocalFile("A-folder1/A-subfolder1"), 500*1024, 15);
		clientA.up();		
		
		clientB.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		clientB.moveFile("A-folder1/A-subfolder1", "A-folder1/A-subfolder2/B-subsubfolder1");
		clientB.up();
		
		clientA.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
