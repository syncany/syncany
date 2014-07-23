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
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.ls.LsOperationOptions;
import org.syncany.operations.ls.LsOperationResult;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
/**
 * This ScenarioTest exists because of issue #142. It concerns
 * the lack of a file being indexed if two identical files are moved
 * at the same time.
 * 
 * @author Pim Otte
 */
public class IdenticalFileMoveScenarioTest {
	@Test
	public void testIdenticalFileMove() throws Exception {
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		
		// Create folder structure
		clientA.createNewFolder("folder");
		clientA.createNewFolder("folder/subfolder1");
		clientA.createNewFolder("folder/subfolder2");
		
		// Create identical files
		clientA.createNewFile("folder/subfolder1/A-file1.jpg", 100);
		clientA.copyFile("folder/subfolder1/A-file1.jpg", "folder/subfolder2/A-file1.jpg");
		
		clientA.up();		
		
		// Move the two identical files
		clientA.moveFile("folder", "moved_folder");
		clientA.up();
		
		// A just upped, so there should not be changes.
		assertFalse(clientA.status().getChangeSet().hasChanges());
		
		// Check if the file histories were based on the correct originals
		LsOperationOptions options = new LsOperationOptions();
		options.setFetchHistories(true);
		options.setPathExpression("moved_folder/subfolder1/");
		
		LsOperationResult lsOperationResult = clientA.ls(options);
		
		for (PartialFileHistory fileHistory : lsOperationResult.getFileVersions().values()) {
			assertTrue(fileHistory.getFileVersion(2).getPath().endsWith(fileHistory.getFileVersion(1).getPath()));
		}
		
		options.setPathExpression("moved_folder/subfolder2/");
		
		lsOperationResult = clientA.ls(options);
		
		for (PartialFileHistory fileHistory : lsOperationResult.getFileVersions().values()) {
			assertTrue(fileHistory.getFileVersion(2).getPath().endsWith(fileHistory.getFileVersion(1).getPath()));
		}

		// Tear down
		clientA.deleteTestData();
	}
}
