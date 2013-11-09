/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
