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

import static org.junit.Assert.assertFalse;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class SingleFileNoConflictsScenarioTest {
	@Test
	public void testSingleClientLocalBackupAndRestoreNoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA1 = new TestClient("A", testConnection);
		TestClient clientA2 = new TestClient("A", testConnection); // same client!

		// Create files and upload
		clientA1.createNewFiles();		
		clientA1.up();
		
		// Download and reconstruct
		clientA2.down();		
		assertFileListEquals(clientA1.getLocalFilesExcludeLockedAndNoRead(), clientA2.getLocalFilesExcludeLockedAndNoRead());
		
		// Cleanup
		clientA1.deleteTestData();
		clientA2.deleteTestData();
	}
	
	@Test
	public void testSingleFileNewNoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFile("file");		
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("file"), clientB.getLocalFile("file"));		
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}		
	
	@Test
	public void testSingleFileMoveNoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFile("file");		
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("file"), clientB.getLocalFile("file"));
		
		clientB.moveFile("file", "moved");
		clientB.up();
		
		clientA.down();
		assertFalse("Originally moved file should not exist.", clientA.getLocalFile("file").exists());
		assertFileEquals(clientA.getLocalFile("moved"), clientB.getLocalFile("moved"));
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}	
	
	@Test
	public void testSingleFileDeleteNoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFile("file");		
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("file"), clientB.getLocalFile("file"));
		
		clientB.deleteFile("file");
		clientB.up();
		
		clientA.down();
		assertFalse("Deleted file should not exist.", clientA.getLocalFile("file").exists());
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}		
	
	@Test
	public void testSingleFileChangeNoConflicts() throws Exception {		
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 		

		// Create files and upload
		clientA.createNewFile("file");		
		clientA.upWithForceChecksum();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("file"), clientB.getLocalFile("file"));
		
		clientB.changeFile("file");
		clientB.upWithForceChecksum();
		
		clientA.down();
		assertFileEquals(clientA.getLocalFile("file"), clientB.getLocalFile("file"));
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());		
		
		// Cleanup
		clientA.deleteTestData();
		clientB.deleteTestData();
	}			
	
	@Test
	public void testComplexNoConflicts() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);		
		
		// Test
		clientA.createNewFile("1");
		clientA.upWithForceChecksum();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("1"), clientB.getLocalFile("1"));
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		clientA.moveFile("1", "2");
		assertFileEquals(clientA.getLocalFile("2"), clientB.getLocalFile("1"));
		
		clientA.upWithForceChecksum();
		clientA.upWithForceChecksum();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("2"), clientB.getLocalFile("2"));
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		clientC.down();
		assertFileEquals(clientA.getLocalFile("2"), clientC.getLocalFile("2"));
		assertFileEquals(clientB.getLocalFile("2"), clientC.getLocalFile("2"));
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientC.getLocalFilesExcludeLockedAndNoRead());
		assertFileListEquals(clientB.getLocalFilesExcludeLockedAndNoRead(), clientC.getLocalFilesExcludeLockedAndNoRead());
		
		clientC.createNewFile("3");
		clientC.changeFile("2");
		clientC.upWithForceChecksum();
		
		clientA.down();
		assertFileEquals(clientC.getLocalFile("3"), clientA.getLocalFile("3"));
		assertFileListEquals(clientC.getLocalFilesExcludeLockedAndNoRead(), clientA.getLocalFilesExcludeLockedAndNoRead());
		
		clientB.down();
		assertFileEquals(clientC.getLocalFile("3"), clientB.getLocalFile("3"));
		assertFileListEquals(clientC.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		
		clientC.down();		
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientC.getLocalFilesExcludeLockedAndNoRead());
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}
}
