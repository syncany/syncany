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
package org.syncany.tests.integration.scenarios;

import static org.junit.Assert.assertEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class EvilCUpWithoutDownScenarioTest {	
	@Test
	public void testEvilC() throws Exception {
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		// Run 
		clientA.createNewFile("A1");
		clientA.upWithForceChecksum();
		clientA.moveFile("A1", "A2");
		clientA.upWithForceChecksum();	
		clientA.changeFile("A2");
		clientA.createNewFile("A3");
		clientA.upWithForceChecksum();
		clientA.deleteFile("A3");
		clientA.upWithForceChecksum();
		
		clientB.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		clientB.createNewFile("A4,B1");
		clientB.up();
		
		clientC.createNewFile("C1"); // Evil. "up" without "down"
		clientC.changeFile("C1");
		clientC.up();
		clientC.changeFile("C1");
		clientC.up();
		clientC.changeFile("C1");
		clientC.up();
		
		clientA.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		Map<String, File> beforeSyncDownBFileList = clientB.getLocalFilesExcludeLockedAndNoRead();
		clientB.down();
		assertFileListEquals("No change in file lists expected. Nothing changed for 'B'.", beforeSyncDownBFileList, clientB.getLocalFilesExcludeLockedAndNoRead()); 
				
		clientC.down();
		assertEquals("Client C is expected to have one more local file (C1)", clientA.getLocalFilesExcludeLockedAndNoRead().size()+1, clientC.getLocalFilesExcludeLockedAndNoRead().size());
		
		clientA.up();
		clientB.up();
		clientC.up();
		
		clientA.down();
		clientB.down();
		clientC.down();		
		
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertFileListEquals(clientB.getLocalFilesExcludeLockedAndNoRead(), clientC.getLocalFilesExcludeLockedAndNoRead());		
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}
}
