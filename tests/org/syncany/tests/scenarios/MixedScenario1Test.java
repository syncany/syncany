package org.syncany.tests.scenarios;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class MixedScenario1Test {
	@Test
	public void testMixedScenario1() throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		// Run 
		clientC.createNewFile("C1");
		clientC.up();
		clientC.createNewFile("C2");
		clientC.up();
		clientC.createNewFile("C3");
		clientC.up();
		
		clientB.down(); // NO CONFLICT

		clientC.createNewFile("C4");
		clientC.up();
		
		clientA.down(); // NO CONFLICT
		
		clientB.createNewFile("B1,C3");
		clientB.up();
		
		clientA.createNewFile("A1,C4");
		clientA.up();
		clientA.createNewFile("A2,C4");
		clientA.up();
		clientA.createNewFile("A3,C4");
		clientA.up();
		
		clientB.down(); // CONFLICT 1
		clientA.down(); // CONFLICT 2
		clientC.down(); // CONFLICT 3
		
		fail("xx");
		
		clientA.up();
		clientC.up();
		clientB.up();
		clientA.up();
		clientB.up();
		clientC.up();
		
		clientB.down(); // CONFLICT 4
		clientA.down(); // CONFLICT 5
		clientC.down(); // CONFLICT 6
		
		clientC.up();
		clientB.up();
		
		clientC.down(); // CONFLICT 7
		clientB.down(); // CONFLICT 8
		clientA.down(); // CONFLICT 9
		
		
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
