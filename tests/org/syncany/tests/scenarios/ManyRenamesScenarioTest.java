package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.*;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ManyRenamesScenarioTest {	
	@Test
	public void testManyRenames() throws Exception {
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
		
		clientB.moveFile("A-original", "B-moved");
		clientB.up();
		
		// A down/move/up
		clientA.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		clientA.moveFile("B-moved", "A-moved");
		clientA.up();
		
		// B down only
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		// A down/move/up
		clientA.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		clientA.moveFile("A-moved", "A-moved-again");
		clientA.up();
		
		// B down/move/up
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		clientB.moveFile("A-moved-again", "B-moved"); // same filename as above!
		clientB.up();
		
		// A down/move/up
		clientA.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		clientA.moveFile("B-moved", "A-moved");  // same filename as above!
		clientA.up();
		
		// B down only
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
