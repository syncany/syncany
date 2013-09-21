package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ConflictDatabaseVersionNoFileConflictScenario {
	@Test
	public void testEmptyFileCreateAndSync() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// A
		clientA.createNewFile("A-file1.jpg", 100);
		clientA.up();		

		// B
		clientB.down();
		
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		clientA.createNewFile("A-file2.jpg", 100);
		clientA.up();
		
		clientB.createNewFile("B-NewIndipendentFile.txt",1000);
		clientB.up();
		
		//TODO [HIGH] Dirty-Database version for after-conflict re-upload and re-use of multichunks! 
	}
}
