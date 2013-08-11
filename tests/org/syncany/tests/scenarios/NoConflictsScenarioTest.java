package org.syncany.tests.scenarios;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class NoConflictsScenarioTest {
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
	public void testNoConflicts() throws Exception {
		clientA.createNewFile("1");
		clientA.up();
		
		clientB.down();
		
		clientA.moveFile("1", "2");
		clientA.up();
		clientA.up();
		
		clientB.down();
		
		clientC.down();
		clientC.createNewFile("3");
		clientC.changeFile("2");
		clientC.up();
		
		clientA.down();
		
		clientB.down();
		
		clientC.down();
		
		fail("No asserts yet.");
	}
}
