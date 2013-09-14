package org.syncany.tests.scenarios;

import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.*;

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
		
		String similarFileName = "A-file1.jpg";
		
		int fileLengthA = 100;
		int fileLengthB = 150;
		
		// Run 
		clientA.createNewFile(similarFileName, fileLengthA);
		clientB.createNewFile(similarFileName, fileLengthB);

		clientA.up();		
		clientB.up();
		
		clientA.down();
		
		//A should win
		assertTrue(clientA.getLocalFile(similarFileName).length() == fileLengthA);

		clientB.down();
		
		//B should have conflicting file and updated on A's file
		assertConflictingFileExists(similarFileName, clientB.getLocalFiles());
		
	
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
