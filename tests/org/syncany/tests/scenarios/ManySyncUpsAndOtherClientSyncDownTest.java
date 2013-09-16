package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ManySyncUpsAndOtherClientSyncDownTest {
	@Test
	public void testManySyncUpsAndOtherClientSyncDown() throws Exception {
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();		
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// ROUND 1: many sync up (cleanups expected)
		for (int i=1; i<=50; i++) {
			clientA.createNewFile("file"+i, 1);
			clientA.up();		
		}
		
		// ROUND 2: sync down by B
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());		
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
