package org.syncany.tests.scenarios;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class EvilCScenarioTest {	
	@Test
	public void testEvilC() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		// Run 
		clientA.createNewFile("newA-somefile.txt");
		clientA.up();
		clientA.moveFile("newA-somefile.txt", "newA-moved-somefile.txt");
		clientA.up();		
		clientA.changeFile("newA-moved-somefile.txt");
		clientA.createNewFile("newA-otherfile.txt");
		clientA.up();
		clientA.deleteFile("newA-otherfile.txt");
		clientA.up();
		
		clientB.down();
		clientB.createNewFile("newB-a-file.txt");
		clientB.up();
		
		clientC.createNewFile("newC");
		clientC.changeFile("newC");
		clientC.up();
		clientC.changeFile("newC");
		clientC.up();
		clientC.changeFile("newC");
		clientC.up();
		
		clientA.down();
		clientB.down();
		clientC.down();
		
		fail("No asserts yet.");
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
		clientC.cleanup();
	}
}
