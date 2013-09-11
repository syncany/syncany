package org.syncany.tests.scenarios;

import static org.junit.Assert.*;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class LongRunningNewAndDeleteScenarioTest {
	private static final Logger logger = Logger.getLogger(LongRunningNewAndDeleteScenarioTest.class.getSimpleName());
	
	@Test
	public void testLongRunningNewAndDeleteFilesNoConflicts() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// Disable logging
		logger.log(Level.INFO, "NOTE: This test can take several minutes!");
		logger.log(Level.INFO, "Disabling console logging for this test; Too much output/overhead.");
		
		while (Logger.getLogger("").getHandlers().length > 0) {
			Logger.getLogger("").removeHandler(Logger.getLogger("").getHandlers()[0]);
		}
		
		// Run 
		for (int round=1; round<30; round++) {
			
			// A
			for (int i=1; i<100; i++) { clientA.createNewFile("A-file-with-size-"+i+".jpg", i); }	
			clientA.up();	
			
			// B 
			clientB.down();						
			assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
			assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
			for (int i=1; i<100; i++) { clientB.changeFile("A-file-with-size-"+i+".jpg"); }
			clientB.up();	
			
			// A 
			clientA.down();						
			assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
			assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
			for (int i=1; i<100; i++) { clientA.deleteFile("A-file-with-size-"+i+".jpg"); }
			clientA.up();	
			
			// B 
			clientB.down();						
			assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
			assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());			
		}

		fail("No asserts yet.");
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
