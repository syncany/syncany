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
package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlResultEquals;

import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.down.DownOperationResult.DownResultCode;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ThreeClientsOneLoserScenarioTest {
	@Test
	public void testThreeClientsOneLoser() throws Exception {		
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);

		UpOperationOptions cUpOptionsWithForce = new UpOperationOptions();
		cUpOptionsWithForce.setForceUploadEnabled(true);
		
		// Run 
		clientA.createNewFile("file1.jpg");
		clientA.upWithForceChecksum();
		
		clientB.down();		
		clientB.createNewFile("file2.jpg");
		clientB.upWithForceChecksum();
		
		clientC.createNewFile("file3.jpg"); // Client C: No down!
		clientC.up(cUpOptionsWithForce);		
		
		// A tries to upload, this fails due to C's unknown database
		clientA.createNewFile("file4.jpg");
		UpOperationResult aUpResult = clientA.upWithForceChecksum(); // 
		assertEquals("Expected to fail, because db-C-1 has not been looked at", UpResultCode.NOK_UNKNOWN_DATABASES, aUpResult.getResultCode());
		assertFalse(clientA.getLocalFile("file2.jpg").exists());
		assertFalse(clientA.getLocalFile("file3.jpg").exists());
		
		// A downloads C's changes, no file changes are expected
		DownOperationResult aDownResult = clientA.down(); 
		assertEquals("Expected to succeed with remote changes (a new database file, but no file changes!).", DownResultCode.OK_WITH_REMOTE_CHANGES, aDownResult.getResultCode());
		assertTrue(clientA.getLocalFile("file2.jpg").exists());
		assertFalse(clientA.getLocalFile("file3.jpg").exists());
		
		// TODO [low] Add assert: "no file changes are expected"
		
		// A uploads again, this time it should succeed, because C's file is in knowndbs.list
		aUpResult = clientA.upWithForceChecksum(); 
		assertEquals("Expected to succeed, because db-C-1 has already been looked at", UpResultCode.OK_APPLIED_CHANGES, aUpResult.getResultCode());
		
		// C calls down and up, to sync its changes
		clientC.down(); // Adds dirty database
		assertSqlResultEquals(clientC.getDatabaseFile(), "select count(*) from databaseversion where status='DIRTY'", "1");
		
		clientC.upWithForceChecksum(); 
		assertSqlResultEquals(clientC.getDatabaseFile(), "select count(*) from databaseversion where status='DIRTY'", "0");
		
		clientA.down();
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientC.getLocalFilesExcludeLockedAndNoRead());		
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
}
