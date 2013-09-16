package org.syncany.tests.scenarios;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ManySyncUpsAndDatabaseFileCleanupTest {
	@Test
	public void testManySyncUpsAndDatabaseFileCleanup() throws Exception {
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		
		// ROUND 1: many sync up (no cleanup expected here)		
		for (int i=1; i<=15; i++) {
			clientA.createNewFile("file"+i, 1);
			clientA.up();		
		}
		
		for (int i=1; i<=15; i++) {
			File expectedDatabaseFile = new File(testConnection.getRepositoryPath()+"/db-A-"+i);
			assertTrue("Database file SHOULD exist: "+expectedDatabaseFile, expectedDatabaseFile.exists());
		}

		// ROUND 2: 1x sync up (cleanup expected!)
		clientA.createNewFile("file16", 1);
		clientA.up();		

		for (int i=1; i<=10; i++) {
			File expectedDatabaseFile = new File(testConnection.getRepositoryPath()+"/db-A-"+i);
			assertTrue("Database file should NOT exist: "+expectedDatabaseFile, !expectedDatabaseFile.exists());
		}
		
		for (int i=11; i<=16; i++) {
			File expectedDatabaseFile = new File(testConnection.getRepositoryPath()+"/db-A-"+i);
			assertTrue("Database file SHOULD exist: "+expectedDatabaseFile, expectedDatabaseFile.exists());
		}
		
		// ROUND 3: many sync up (no cleanup expected here)		
		for (int i=17; i<=25; i++) {
			clientA.createNewFile("file"+i, 1);
			clientA.up();		
		}
		
		for (int i=1; i<=10; i++) {
			File expectedDatabaseFile = new File(testConnection.getRepositoryPath()+"/db-A-"+i);
			assertTrue("Database file should NOT exist: "+expectedDatabaseFile, !expectedDatabaseFile.exists());
		}
		
		for (int i=11; i<=25; i++) {
			File expectedDatabaseFile = new File(testConnection.getRepositoryPath()+"/db-A-"+i);
			assertTrue("Database file SHOULD exist: "+expectedDatabaseFile, expectedDatabaseFile.exists());
		}		
		
		// ROUND 4: 1x sync up (cleanup expected!)
		clientA.createNewFile("file26", 1);
		clientA.up();		

		for (int i=1; i<=20; i++) {
			File expectedDatabaseFile = new File(testConnection.getRepositoryPath()+"/db-A-"+i);
			assertTrue("Database file should NOT exist: "+expectedDatabaseFile, !expectedDatabaseFile.exists());
		}
		
		for (int i=21; i<=25; i++) {
			File expectedDatabaseFile = new File(testConnection.getRepositoryPath()+"/db-A-"+i);
			assertTrue("Database file SHOULD exist: "+expectedDatabaseFile, expectedDatabaseFile.exists());
		}	
		
		// Tear down
		clientA.cleanup();		
	}
}
