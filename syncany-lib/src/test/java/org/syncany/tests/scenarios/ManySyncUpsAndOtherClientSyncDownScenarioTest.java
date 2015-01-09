/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import org.junit.Test;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ManySyncUpsAndOtherClientSyncDownScenarioTest {
	@Test
	public void testManySyncUpsAndOtherClientSyncDown() throws Exception {
		// Setup 
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();		
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// ROUND 1: many sync up (cleanups expected)
		for (int i=1; i<=50; i++) {
			clientA.createNewFile("file"+i, 1);
			clientA.up();		
		}
		
		// ROUND 2: sync down by B
		clientB.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());		
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
	
	@Test
	public void testManySyncUpsAndOtherClientSyncDownSameFileAddRemove() throws Exception {
		// Setup 
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();		
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		
		// TODO Test not finished
		
		// ROUND 1: many sync up (cleanups expected)
		String[] names = new String[] { "one", "two", "three", "four", "five" };
		int nameIndex= 0;
		
		for (int i=1; i<=20; i++) {
			String filename = names[nameIndex++ % names.length];
			
			if (clientA.getLocalFile(filename).exists()) {
				clientA.deleteFile(filename);
			}
			else {
				clientA.createNewFile(filename);
			}
			
			clientA.up();		
		}
		
		// ROUND 2: sync down by B
		clientB.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());		
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
