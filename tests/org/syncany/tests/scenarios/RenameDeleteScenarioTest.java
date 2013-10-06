package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.*;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class RenameDeleteScenarioTest {	
	@Test
	public void testDeleteFileThatHasAlreadyMoved() throws Exception {
		// Scenario: A deletes a file that B has already moved
		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// A new/up
		clientA.createNewFile("A-original");
		clientA.up();
		
		// B down/move/up
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		// A moves, and up
		clientA.deleteFile("A-original");
		clientA.up();
		
		// B deletes, then down; this should not fail or throw exceptions
		clientB.moveFile("A-original", "B-moved");
		clientB.down(); 

		// Sync them
		clientB.sync();		
		clientA.sync();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());		
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
