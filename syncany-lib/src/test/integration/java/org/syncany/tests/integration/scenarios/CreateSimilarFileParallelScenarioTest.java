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
import static org.syncany.tests.util.TestAssertUtil.assertConflictingFileExists;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class CreateSimilarFileParallelScenarioTest {
	@Test
	public void testEmptyFileCreateAndSync() throws Exception {
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// A
		clientA.createNewFile("A-file1.jpg", 100);
		clientA.up();		

		// B
		clientB.createNewFile("A-file1.jpg", 150);
		clientB.up();
		
		// A, A should win
		clientA.down();
		assertEquals(clientA.getLocalFile("A-file1.jpg").length(), 100);

		// B, B should have conflicting file and updated on A's file
		clientB.down();
		assertConflictingFileExists("A-file1.jpg", clientB.getLocalFilesExcludeLockedAndNoRead());
		assertFileEquals(clientA.getLocalFile("A-file1.jpg"), clientB.getLocalFile("A-file1.jpg"));
		assertEquals(clientB.getLocalFile("A-file1.jpg").length(), 100);
		
		// B
		clientB.up();
				
		// A, should retrieve B's conflicting copy
		clientA.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());				
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
