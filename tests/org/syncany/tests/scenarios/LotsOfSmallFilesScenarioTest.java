package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class LotsOfSmallFilesScenarioTest {
	@Test
	public void testLotsOfSmallFiles() throws Exception {		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		for (int i=0; i<10000; i++) {
			clientA.createNewFile("file"+i, 100+i);
		}
		
		clientA.up(); // This has caused a heap space exception
		clientB.down(); 
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}	
}
