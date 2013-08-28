package org.syncany.tests.scenarios;

import static org.junit.Assert.*;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class NoConflictsScenarioTest {
	@Test
	public void testSingleClientLocalBackupAndRestoreNoConflicts() throws Exception {
		// Setup
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA1 = new TestClient("A", testConnection);
		TestClient clientA2 = new TestClient("A", testConnection); // same client!

		// Create files and upload
		clientA1.createNewFiles();		
		clientA1.up();
		
		// Download and reconstruct
		clientA2.down();		
		assertFileListEquals(clientA1.getLocalFiles(), clientA2.getLocalFiles());
		
		// Cleanup
		clientA1.cleanup();
		clientA2.cleanup();
	}
	
	@Test
	public void testNewSingleFileNoConflicts() throws Exception {
		// Setup
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFile("file");		
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("file"), clientB.getLocalFile("file"));		
		
		// Cleanup
		clientA.cleanup();
		clientB.cleanup();
	}		
	
	@Test
	public void testMoveSingleFileNoConflicts() throws Exception {
		// Setup
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
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
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		// Cleanup
		clientA.cleanup();
		clientB.cleanup();
	}	
	
	@Test
	public void testDeleteSingleFileNoConflicts() throws Exception {
		// Setup
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
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
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		// Cleanup
		clientA.cleanup();
		clientB.cleanup();
	}		
	
	@Test
	public void testChangeSingleFileNoConflicts() throws Exception {
		// Setup
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFile("file");		
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("file"), clientB.getLocalFile("file"));
		
		clientB.changeFile("file");
		clientB.up();
		
		clientA.down();
		assertFileEquals(clientA.getLocalFile("file"), clientB.getLocalFile("file"));
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());		
		
		// Cleanup
		clientA.cleanup();
		clientB.cleanup();
	}		
	
	@Test
	public void testNewSingleFolderNoConflicts() throws Exception {
		// Setup
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection); 

		// Create files and upload
		clientA.createNewFolder("folder");		
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("folder"), clientB.getLocalFile("folder"));		
		
		// Cleanup
		clientA.cleanup();
		clientB.cleanup();
	}		
	
	@Test
	public void testComplexNoConflicts() throws Exception {
		// Setup
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		// Test
		clientA.createNewFile("1");
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("1"), clientB.getLocalFile("1"));
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		clientA.moveFile("1", "2");
		assertFileEquals(clientA.getLocalFile("2"), clientB.getLocalFile("1"));
		
		clientA.up();
		clientA.up();
		
		clientB.down();
		assertFileEquals(clientA.getLocalFile("2"), clientB.getLocalFile("2"));
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		clientC.down();
		assertFileEquals(clientA.getLocalFile("2"), clientC.getLocalFile("2"));
		assertFileEquals(clientB.getLocalFile("2"), clientC.getLocalFile("2"));
		assertFileListEquals(clientA.getLocalFiles(), clientC.getLocalFiles());
		assertFileListEquals(clientB.getLocalFiles(), clientC.getLocalFiles());
		
		clientC.createNewFile("3");
		clientC.changeFile("2");
		clientC.up();
		
		clientA.down();
		assertFileEquals(clientC.getLocalFile("3"), clientA.getLocalFile("3"));
		assertFileListEquals(clientC.getLocalFiles(), clientA.getLocalFiles());
		
		clientB.down();
		assertFileEquals(clientC.getLocalFile("3"), clientB.getLocalFile("3"));
		assertFileListEquals(clientC.getLocalFiles(), clientB.getLocalFiles());
		
		clientC.down();
		
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertFileListEquals(clientA.getLocalFiles(), clientC.getLocalFiles());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
		clientC.cleanup();
	}
}
