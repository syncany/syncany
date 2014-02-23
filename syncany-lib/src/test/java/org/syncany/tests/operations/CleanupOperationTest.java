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
		options.setRemoveDeletedVersions(true);
		options.setKeepVersionsCount(2);

		// Run
		
		// A: Create some file versions
		clientA.createNewFile("someotherfile.jpg");	// These two files' chunks will be in one multichunk	
		clientA.createNewFile("file.jpg");		    // Only one of the chunks will be needed after cleanup!
		                                            // The multichunk will be 50% useless
		for (int i=1; i<=4; i++) {
			clientA.changeFile("file.jpg");
			clientA.upWithForceChecksum();			
		}
		
		clientA.createNewFile("otherfile.txt");
		for (int i=1; i<=3; i++) {
			clientA.changeFile("otherfile.txt");
			clientA.upWithForceChecksum();			
		}
		
		clientA.createNewFile("deletedfile.txt");
		for (int i=1; i<=3; i++) {
			clientA.changeFile("deletedfile.txt");
			clientA.upWithForceChecksum();			
		}		
		clientA.deleteFile("deletedfile.txt");
		clientA.upWithForceChecksum();			
		
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile());		
		assertEquals("12", TestAssertUtil.runSqlQuery("select count(*) from fileversion", databaseConnectionA));
		assertEquals("11", TestAssertUtil.runSqlQuery("select count(*) from chunk", databaseConnectionA));
		assertEquals("10", TestAssertUtil.runSqlQuery("select count(*) from multichunk", databaseConnectionA));
		assertEquals("11", TestAssertUtil.runSqlQuery("select count(*) from filecontent", databaseConnectionA));
		assertEquals("4", TestAssertUtil.runSqlQuery("select count(distinct id) from filehistory", databaseConnectionA));

		// B: Sync down by other client
		clientB.down();
		
		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile());		
		assertEquals("12", TestAssertUtil.runSqlQuery("select count(*) from fileversion", databaseConnectionB));
		assertEquals("11", TestAssertUtil.runSqlQuery("select count(*) from chunk", databaseConnectionB));
		assertEquals("10", TestAssertUtil.runSqlQuery("select count(*) from multichunk", databaseConnectionB));
		assertEquals("11", TestAssertUtil.runSqlQuery("select count(*) from filecontent", databaseConnectionB));
		assertEquals("4", TestAssertUtil.runSqlQuery("select count(distinct id) from filehistory", databaseConnectionB));
		
		// A: Cleanup this mess (except for two)     <<<< This is the interesting part!!! <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
		clientA.cleanup(options);		
		
		// 2 versions for "file.jpg", 2 versions for "otherfile.txt" and one version for "someotherfile.jpg"
		assertEquals("5", TestAssertUtil.runSqlQuery("select count(*) from fileversion", databaseConnectionA));
		assertEquals("7", TestAssertUtil.runSqlQuery("select sum(version) from fileversion where path='file.jpg'", databaseConnectionA)); // 3+4
		assertEquals("5", TestAssertUtil.runSqlQuery("select sum(version) from fileversion where path='otherfile.txt'", databaseConnectionA)); // 2+3
		assertEquals("1", TestAssertUtil.runSqlQuery("select sum(version) from fileversion where path='someotherfile.jpg'", databaseConnectionA));
				
		// Normally this should be 5, but because the chunk of "someotherfile.jpg" and "file.jpg" (version 1) 
		// are in the same multichunk, the chunk of "file1.jpg" (version 1) cannot be deleted
		// --> So "6" is correct
		assertEquals("6", TestAssertUtil.runSqlQuery("select count(*) from chunk", databaseConnectionA));
		
		// 6 chunks in 5 multichunks
		assertEquals("5", TestAssertUtil.runSqlQuery("select count(*) from multichunk", databaseConnectionA));
		assertEquals("5", TestAssertUtil.runSqlQuery("select count(*) from filecontent", databaseConnectionA));
		assertEquals("3", TestAssertUtil.runSqlQuery("select count(distinct id) from filehistory", databaseConnectionA));
		
		// Test the repo
		assertEquals(5, new File(testConnection.getRepositoryPath()+"/multichunks/").list().length);
		assertEquals(4, new File(testConnection.getRepositoryPath()+"/databases/").list().length); 

		// B: Sync down cleanup
		clientB.down();
		TestAssertUtil.assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// Tear down
		clientA.deleteTestData();		
	}
}
