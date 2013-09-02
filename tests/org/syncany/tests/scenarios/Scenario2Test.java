package org.syncany.tests.scenarios;

import static org.junit.Assert.*;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class Scenario2Test {	
	@Test
	public void testManyRenames() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		fail("Not done yet");
		// Run 
		clientA.createNewFile("A1");
		clientA.up();
		clientA.moveFile("A1", "A2");
		clientA.up();		
		clientA.changeFile("A2");
		clientA.createNewFile("A3");
		clientA.up();
		clientA.deleteFile("A3");
		clientA.up();
		
		clientB.down();
		assertFileListEquals(clientA.getLocalFiles(), clientB.getLocalFiles());
		
		clientB.createNewFile("A4,B1");
		clientB.up();
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}
}
