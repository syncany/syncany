package org.syncany.tests.scenarios;

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
		clientA.createNewFile("newA1");
		clientA.createNewFile("newA2");
		clientA.up();
		clientA.moveFile("newA1", "newMovedA3");
		clientA.changeFile("newMovedA3");
		clientA.up();
		
		clientB.down();
		clientB.createNewFile("newB");
		clientB.up();
		
		clientC.createNewFile("newC1");
		clientC.createNewFile("newC2");
		clientC.up();
		clientC.createNewFile("newC3");
		clientC.up();
		
		clientA.down();
		clientB.down();
	}
}
