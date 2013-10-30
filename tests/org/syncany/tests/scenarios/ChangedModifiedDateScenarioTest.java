package org.syncany.tests.scenarios;

import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ChangedModifiedDateScenarioTest {
	@Test
	public void testChangeModifiedDate() throws Exception {		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		clientA.createNewFile("file1.jpg");
		clientA.upWithForceChecksum();
		
		FileUtils.copyFile(clientA.getLocalFile("file1.jpg"), clientB.getLocalFile("file1.jpg"));
		clientB.getLocalFile("file1.jpg").setLastModified(123456789);
		
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}	
}
