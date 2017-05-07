/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.integration.scenarios;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlUtil;

public class FirstVersionDirtyScenarioTest {
	@Test
	public void testFirstVersionDirty() throws Exception {
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		UpOperationOptions forceUpOperationOptions = new UpOperationOptions();
		forceUpOperationOptions.setForceUploadEnabled(true);
		
		// Run 
		clientA.createNewFile("A-file1.jpg", 1*1024*1024);
		clientA.up(forceUpOperationOptions);	
		
		Thread.sleep(200); // B's timestamp must be later
		
		TestFileUtil.copyFile(clientA.getLocalFile("A-file1.jpg"), clientB.getLocalFile("A-file1.jpg"));
		clientB.up(forceUpOperationOptions);	

		// Client B loses		
		clientB.down();  // <<<<<<< This used to throw all sorts of PRIMARY KEY CONSTRAINT errors
		
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile(), false);
		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile(), false);
		
		TestAssertUtil.assertSqlDatabaseTablesEqual(clientA.getDatabaseFile(), clientB.getDatabaseFile(), new String[] {
			"chunk", "multichunk", "filecontent", "filecontent_chunk"
		});
		
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));
		assertEquals("2", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionB));		
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from databaseversion where status='DIRTY'", databaseConnectionB));
		
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from filehistory", databaseConnectionA));
		assertEquals("2", TestSqlUtil.runSqlSelect("select count(*) from filehistory", databaseConnectionB));		
		
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionA));
		assertEquals("2", TestSqlUtil.runSqlSelect("select count(*) from fileversion", databaseConnectionB));		

		// The table "multichunk_chunk" has 4 entries in B, but only 2 in A (!)
		// This is because the same file (= same chunks) are uploaded twice. 
		
		// TODO [medium] The cleanup operation does not clean multichunks with redundant chunks, or does it?!
				
		assertEquals(
				2*Integer.parseInt(TestSqlUtil.runSqlSelect("select count(*) from multichunk_chunk", databaseConnectionA)),
				Integer.parseInt(TestSqlUtil.runSqlSelect("select count(*) from multichunk_chunk", databaseConnectionB)));		
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
