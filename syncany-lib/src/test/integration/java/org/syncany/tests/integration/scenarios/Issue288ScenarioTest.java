/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlUtil;

public class Issue288ScenarioTest {
	@Test
	public void testIssue288() throws Exception {	
		/*
		 * This tests issue #288, an issue in which a file with duplicate chunks are created
		 * incorrectly, because the cleanup throws away too many entries in the filecontent_chunks
		 * database table. 
		 * 
		 * The test first creates a file with duplicate chunks, then syncs this file, and then 
		 * moves this file on both clients -- that forces the other client to recreate the file 
		 * from scratch.
		 */
		
		// Setup 
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();		
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile());
		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile());
		
		CleanupOperationOptions cleanupOptionsKeepOne = new CleanupOperationOptions();
		cleanupOptionsKeepOne.setForce(true);
		
		// Create file content with two duplicate chunks
		// The file has 4 chunks (4 * 512 bytes), the middle chunks are identical
		
		byte[] fileContentA = new byte[2*1024*1024]; // 1 MB
		
		for (int i=0; i<512*1024; i++) { // First chunk
			fileContentA[i] = (byte) i;
		}
		
		for (int i=512*1024; i<1536*1024; i++) { // Two identical middle chunks
			fileContentA[i] = 99;
		}
		
		for (int i=1536*1024; i<2*1024*1024; i++) { // Last chunk
			fileContentA[i] = (byte) (i+i);
		}
		
		FileUtils.writeByteArrayToFile(clientA.getLocalFile("fileA"), fileContentA);
		
		clientA.upWithForceChecksum();		
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(*) from chunk", databaseConnectionA));		
		assertEquals("4", TestSqlUtil.runSqlSelect("select count(*) from filecontent_chunk", databaseConnectionA));		
		
		// Sync file to client B
		clientB.down();
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(*) from chunk", databaseConnectionB));		
		assertEquals("4", TestSqlUtil.runSqlSelect("select count(*) from filecontent_chunk", databaseConnectionB));		
		
		// Move file, sync again and perform cleanup (wipe everything but one file version)
		clientA.moveFile("fileA", "fileA-moved");
		clientA.upWithForceChecksum();
		clientA.cleanup(cleanupOptionsKeepOne);
		
		// Delete file locally and sync down
		clientB.deleteFile("fileA");
		clientB.down(); // <<<< This throws an exception!		
		
		// Tear down
		clientB.deleteTestData();
		clientA.deleteTestData();
	}		
}
