package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.*;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class RenameToExistingFileScenarioTest {	
	@Test
	public void testRenameFileButDestinationExists() throws Exception {
		// Scenario: A moves a file, but B creates another file at the same destination
		
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
		clientA.moveFile("A-original", "A-moved");
		clientA.up();
		
		// B creates file at same location
		clientB.createNewFile("A-moved"); // << same as above
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
