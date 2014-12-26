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

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.cleanup.CleanupOperationResult;
import org.syncany.operations.cleanup.CleanupOperationResult.CleanupResultCode;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class Issue316ScenarioTest {
	@Test
	public void testIssue316CleanupThenDeleteFile() throws Exception {			
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(Arrays.asList(new String[] {
				// List of failing operations (regex)
				// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

				"rel=(5|6|7) .+download.+multichunk" // << 3 retries!
		}));

		TestClient clientA = new TestClient("A", testConnection);
		java.sql.Connection databaseConnectionA = clientA.getConfig().createDatabaseConnection();

		TestClient clientB = new TestClient("B", testConnection);
		
		CleanupOperationOptions cleanupOptionsKeepOne = new CleanupOperationOptions();
		cleanupOptionsKeepOne.setKeepVersionsCount(1);
		cleanupOptionsKeepOne.setMaxDatabaseFiles(1);
		cleanupOptionsKeepOne.setForce(true);	
				
		clientA.createNewFile("Kazam_screencast_00010.mp4");
		clientA.upWithForceChecksum();
				
		clientB.down();
		
		clientA.createNewFile("SomeFileTOIncreaseTheDatabaseFileCount");
		clientA.upWithForceChecksum();
		
		CleanupOperationResult cleanupResult = clientA.cleanup(cleanupOptionsKeepOne);
		assertEquals(CleanupResultCode.OK, cleanupResult.getResultCode());
		
		clientA.deleteFile("Kazam_screencast_00010.mp4");
		clientA.upWithForceChecksum();
		
		boolean downFailedAtB = false;
		
		try {
			clientB.down();
		}
		catch (Exception e) {
			downFailedAtB = true;
		}
		
		assertTrue(downFailedAtB);
		clientB.down();
		
		// Tear down
		clientB.deleteTestData();
		clientA.deleteTestData();
	}		
}
