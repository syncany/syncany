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
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import org.junit.Test;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class RenameNoDownloadMultiChunksScenarioTest {
	@Test
	public void testRenameAndCheckIfMultiChunksAreDownloaded() throws Exception {
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		
		// Prepare, create file at A, sync it to B
		clientA.createNewFile("A-file1");
		clientA.sync();		
		clientB.sync();
				
		// Now move file, and sync
		clientA.moveFile("A-file1", "A-file-moved1");
		clientA.up();
		
		DownOperationResult downOperationResult = clientB.down();		
		assertEquals("No multichunks should have been downloaded.", 0, downOperationResult.getDownloadedMultiChunks().size());
		assertTrue("Moved files should exist.", clientB.getLocalFile("A-file-moved1").exists());		
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}	
}
