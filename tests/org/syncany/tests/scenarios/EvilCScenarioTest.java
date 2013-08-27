package org.syncany.tests.scenarios;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class EvilCScenarioTest {
	private TestClient clientA;
	private TestClient clientB;
	private TestClient clientC;
	 
	@Before
	public void setUp() throws Exception {
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		clientA = new TestClient("A", testConnection);
		clientB = new TestClient("B", testConnection);
		clientC = new TestClient("C", testConnection);
	} 
	
	@After 
	public void tearDown() {
		/*
		clientA.cleanup();
		clientB.cleanup();
		clientC.cleanup();*/
	}
		 
	@Test
	public void testEvilC() throws Exception {
		clientA.createNewFile("newA-somefile.txt");
		clientA.up();
		clientA.moveFile("newA-somefile.txt", "newA-moved-somefile.txt");
		clientA.changeFile("newA-moved-somefile.txt");
		clientA.up();
		clientA.createNewFile("newA-otherfile.txt");
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
		clientC.deleteFile("newC");
		
		clientA.down();
		clientB.down();
		clientC.down();
		
		fail("No asserts yet.");
	}
}
