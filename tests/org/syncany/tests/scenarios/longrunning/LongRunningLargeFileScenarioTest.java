package org.syncany.tests.scenarios.longrunning;

import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class LongRunningLargeFileScenarioTest {
	private static final Logger logger = Logger.getLogger(LongRunningLargeFileScenarioTest.class.getSimpleName());
	private static final int SIZE_800_MB = 800*1024*1024;

	@Test
	public void testEmptyFileCreateAndSync() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// A		
		logger.log(Level.INFO, "Creating large file for scenario ...");
		clientA.createNewFile("A-large-file.zip", SIZE_800_MB);
		
		logger.log(Level.INFO, "NOTE: This test can take several minutes!");
		logger.log(Level.INFO, "Disabling console logging for this test; Too much output/overhead.");

		Logging.disableLogging();
		
		clientA.up();		

		// B
		clientB.down(); 
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
