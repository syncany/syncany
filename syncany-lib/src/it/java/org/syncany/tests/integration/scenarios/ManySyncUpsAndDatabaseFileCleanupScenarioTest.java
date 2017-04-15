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

import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.io.File;

import org.junit.Test;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class ManySyncUpsAndDatabaseFileCleanupScenarioTest {
	@Test
	public void testManySyncUpsAndDatabaseFileCleanup() throws Exception {
		// Setup 
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);

		// ROUND 1: many sync up (no cleanup expected here)		
		for (int i = 1; i <= 15; i++) {
			clientA.createNewFile("file" + i, 1);
			clientA.up();
		}

		for (int i = 1; i <= 15; i++) {
			DatabaseRemoteFile expectedDatabaseRemoteFile = new DatabaseRemoteFile("A", i);
			File expectedDatabaseFile = new File(testConnection.getPath() + "/databases/" + expectedDatabaseRemoteFile.getName());

			assertTrue("Database file SHOULD exist: " + expectedDatabaseFile, expectedDatabaseFile.exists());
		}

		// ROUND 2: 1x sync up (cleanup expected!)
		clientA.createNewFile("file16", 1);
		clientA.up();

		clientA.cleanup(); // Force cleanup

		for (int i = 1; i <= 15; i++) {
			DatabaseRemoteFile expectedDatabaseRemoteFile = new DatabaseRemoteFile("A", i);
			File expectedDatabaseFile = new File(testConnection.getPath() + "/databases/" + expectedDatabaseRemoteFile.getName());

			assertTrue("Database file should NOT exist: " + expectedDatabaseFile, !expectedDatabaseFile.exists());
		}

		for (int i = 17; i <= 17; i++) {
			DatabaseRemoteFile expectedDatabaseRemoteFile = new DatabaseRemoteFile("A", i);
			File expectedDatabaseFile = new File(testConnection.getPath() + "/databases/" + expectedDatabaseRemoteFile.getName());

			assertTrue("Database file SHOULD exist: " + expectedDatabaseFile, expectedDatabaseFile.exists());
		}

		// ROUND 3: many sync up (no cleanup expected here)		
		for (int i = 17; i <= 30; i++) {
			clientA.createNewFile("file" + i, 1);
			clientA.up();
		}

		for (int i = 1; i <= 16; i++) {
			DatabaseRemoteFile expectedDatabaseRemoteFile = new DatabaseRemoteFile("A", i);
			File expectedDatabaseFile = new File(testConnection.getPath() + "/databases/" + expectedDatabaseRemoteFile.getName());

			assertTrue("Database file should NOT exist: " + expectedDatabaseFile, !expectedDatabaseFile.exists());
		}

		for (int i = 17; i <= 31; i++) {
			DatabaseRemoteFile expectedDatabaseRemoteFile = new DatabaseRemoteFile("A", i);
			File expectedDatabaseFile = new File(testConnection.getPath() + "/databases/" + expectedDatabaseRemoteFile.getName());

			assertTrue("Database file SHOULD exist: " + expectedDatabaseFile, expectedDatabaseFile.exists());
		}

		// ROUND 4: 1x sync up (cleanup expected!)
		clientA.createNewFile("file31", 1);
		clientA.up();

		CleanupOperationOptions options = new CleanupOperationOptions();

		options.setForce(true);

		clientA.cleanup(options); // Force cleanup 

		for (int i = 1; i <= 32; i++) {
			DatabaseRemoteFile expectedDatabaseRemoteFile = new DatabaseRemoteFile("A", i);
			File expectedDatabaseFile = new File(testConnection.getPath() + "/databases/" + expectedDatabaseRemoteFile.getName());

			assertTrue("Database file should NOT exist: " + expectedDatabaseFile, !expectedDatabaseFile.exists());
		}

		for (int i = 33; i <= 33; i++) {
			DatabaseRemoteFile expectedDatabaseRemoteFile = new DatabaseRemoteFile("A", i);
			File expectedDatabaseFile = new File(testConnection.getPath() + "/databases/" + expectedDatabaseRemoteFile.getName());

			assertTrue("Database file SHOULD exist: " + expectedDatabaseFile, expectedDatabaseFile.exists());
		}

		// Tear down
		TestClient clientB = new TestClient("B", testConnection);
		clientB.down();
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
