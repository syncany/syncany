package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class FilenameCapitalizationWindowsScenarioTest {
	// TODO [medium] Windows: LARGE/small capitalization --> Dropbox makes a file "name (Case Conflict 1)"; define expected/desired behavior	
	
	@Test
	public void testFilenameCapitalizationWindows() throws Exception {		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		clientA.createNewFile("FILENAME-FOR-WINDOWS");
		clientA.createNewFile("filename-for-windows");
		clientA.createNewFile("Filename-For-Windows");
		clientA.upWithForceChecksum();
		assertEquals("There should be three files.", 3, clientA.getLocalFiles().size());
		
		clientB.down();
		assertEquals("There should be three files.", 3, clientB.getLocalFiles().size());
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());	
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}		
}
