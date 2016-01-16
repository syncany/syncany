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

import static org.junit.Assert.assertFalse;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class SingleFolderNoConflictsScenarioTest {	
	@Test
	public void testFolderEmptyNewNoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 
		
		// Create files and upload
		clientA.createNewFolder("folder");		
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("folder"), clientB.getLocalFile("folder"));		
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}		
	
	@Test
	public void testFolderEmptyMoveNoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFolder("folder");		
		clientA.up();
		
		clientB.down();
		clientB.moveFile("folder", "moved");
		clientB.up();
		
		clientA.down();
		assertFileEquals(clientA.getLocalFile("moved"), clientB.getLocalFile("moved"));	
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}			
	
	@Test
	public void testFolderEmptyDeleteNoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFolder("folder");		
		clientA.up();
		
		clientB.down();
		clientB.deleteFile("folder");
		clientB.up();
		
		clientA.down();	
		assertFalse("Deleted folder should not exist.", clientA.getLocalFile("folder").exists());		
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}	
	
	@Test
	public void testFolderNonEmptyNewNoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFolder("folder");		
		clientA.createNewFile("folder/file");
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("folder"), clientB.getLocalFile("folder"));
		assertFileEquals(clientA.getLocalFile("folder/file"), clientB.getLocalFile("folder/file"));
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}		
	
	@Test
	public void testFolderNonEmptyMove1NoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFolder("folder");		
		clientA.createNewFile("folder/file");
		clientA.up();
		
		clientB.down();
		clientB.moveFile("folder", "moved");
		clientB.up();
		  
		clientA.down();
		assertFalse("Deleted folder should not exist.", clientA.getLocalFile("folder").exists());
		assertFalse("Deleted file should not exist.", clientA.getLocalFile("folder/file").exists());
		assertFileEquals(clientA.getLocalFile("moved"), clientB.getLocalFile("moved"));
		assertFileEquals(clientA.getLocalFile("moved/file"), clientB.getLocalFile("moved/file"));
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}		
	
	@Test
	public void testFolderNonEmptyMove2NoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFolder("folder");		
		clientA.createNewFile("folder/file");
		clientA.createNewFolder("folder/folder2");
		clientA.createNewFile("folder/folder2/file2");
		clientA.up();
		
		clientB.down();
		clientB.moveFile("folder", "moved");
		clientB.up();
		  
		clientA.down();
		assertFalse("Deleted folder should not exist.", clientA.getLocalFile("folder").exists());
		assertFalse("Deleted file should not exist.", clientA.getLocalFile("folder/file").exists());
		assertFileEquals(clientA.getLocalFile("moved"), clientB.getLocalFile("moved"));
		assertFileEquals(clientA.getLocalFile("moved/file"), clientB.getLocalFile("moved/file"));
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}			
}
