/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.operations;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.operations.CleanupOperation.CleanupOperationOptions;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class CleanupOperationTest {
	@Test
	public void testEasyCleanup() throws Exception {
		// Setup
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setMergeRemoteFiles(false);
		options.setRemoveOldVersions(true);
		options.setRepackageMultiChunks(false);
		options.setKeepVersionsCount(2);

		// Run
		
		// A: Create some file versions
		clientA.createNewFile("file.jpg");

		for (int i=1; i<=10; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();			
		}
		
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile());		
		assertEquals("10", TestAssertUtil.runSqlQuery("select count(*) from fileversion", databaseConnectionA));

		// B: Sync down by other client
		clientB.down();
		
		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile());		
		assertEquals("10", TestAssertUtil.runSqlQuery("select count(*) from fileversion", databaseConnectionB));
		
		// A: Cleanup this mess (except for two)     <<<< This is the interesting part!!!
		clientA.cleanup(options);		
		assertEquals("2", TestAssertUtil.runSqlQuery("select count(*) from fileversion", databaseConnectionA));
		
		// B: Sync down cleanup
		clientB.down();
		assertEquals("2", TestAssertUtil.runSqlQuery("select count(*) from fileversion", databaseConnectionB));

		// Test the repo
		assertEquals(2, new File(testConnection.getRepositoryPath()+"/multichunks/").list().length);
		assertEquals(4, new File(testConnection.getRepositoryPath()+"/databases/").list().length); 
		
		// Tear down
		clientA.deleteTestData();		
	}
}
