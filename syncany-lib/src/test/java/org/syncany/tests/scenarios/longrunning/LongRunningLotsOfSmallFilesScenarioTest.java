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
package org.syncany.tests.scenarios.longrunning;

import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class LongRunningLotsOfSmallFilesScenarioTest {
	@Test
	public void testLotsOfSmallFiles() throws Exception {		
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		for (int i=0; i<10000; i++) {
			clientA.createNewFile("file"+i, 100+i);
		}
		
		clientA.up(); // This has caused a heap space exception
		clientB.down(); 
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}	
}
