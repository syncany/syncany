package org.syncany.tests.scenarios;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.tests.util.TestClient;

public class EvilCScenario {
	private TestClient clientA;
	private TestClient clientB;
	private TestClient clientC;
	
	@Before
	public void setUp() throws Exception {
		clientA = new TestClient();
		clientB = new TestClient();
		clientC = new TestClient();
	}
	
	@After
	public void tearDown() {
		clientA.cleanup();
		clientB.cleanup();
		clientC.cleanup();
	}
		
	@Test
	public void testEvilC() throws Exception {
		clientA.createNewFiles();
		clientA.up();
		
		clientB.down();
		clientB.createNewFile("newB");
		
		clientC.createNewFile("newC1");
		clientC.createNewFile("newC2");
		clientC.up();
		
		clientA.down();
		clientB.down();
	}
}
