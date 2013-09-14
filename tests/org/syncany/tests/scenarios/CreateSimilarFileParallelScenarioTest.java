package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.syncany.tests.util.TestAssertUtil.assertConflictingFileExists;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class CreateSimilarFileParallelScenarioTest {
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
		clientB.createNewFile("A-file1.jpg", 150);
		clientB.up();
		
		// A, A should win
		clientA.down();
		assertEquals(clientA.getLocalFile("A-file1.jpg").length(), 100);

		// B, B should have conflicting file and updated on A's file
		clientB.down();
		assertConflictingFileExists("A-file1.jpg", clientB.getLocalFiles());
		assertFileEquals(clientA.getLocalFile("A-file1.jpg"), clientB.getLocalFile("A-file1.jpg"));
		assertEquals(clientB.getLocalFile("A-file1.jpg").length(), 100);
		
		// B
		clientB.up();
				
		// A, should retrieve B's conflicting copy
		clientA.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());				
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
