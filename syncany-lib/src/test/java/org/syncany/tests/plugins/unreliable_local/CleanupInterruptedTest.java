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
package org.syncany.tests.plugins.unreliable_local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.transfer.RetriableTransferManager;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class CleanupInterruptedTest {
	private static final Logger logger = Logger.getLogger(CleanupInterruptedTest.class.getSimpleName());

	@BeforeClass
	public static void setUp() {
		RetriableTransferManager.RETRY_SLEEP_MILLIS = 50;
	}

	@Test
	public void testUnreliableCleanup_Test1_oldVersionRemoval() throws Exception {
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>
						"rel=(11|12|13).+upload.+database", // << 3 retries!!
				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		clientA.createNewFile("file");

		clientA.up();

		clientA.changeFile("file");
		clientA.up();

		CleanupOperationOptions cleanupOptions = new CleanupOperationOptions();
		cleanupOptions.setKeepVersionsCount(1);
		boolean cleanupFailed = false;
		try {
			clientA.cleanup(cleanupOptions);
		}
		catch (StorageException e) {
			cleanupFailed = true;
		}

		assertTrue(cleanupFailed);
		assertEquals(2, new File(testConnection.getRepositoryPath(), "multichunks").list().length);
		assertEquals(2, new File(testConnection.getRepositoryPath(), "databases").list().length);
		assertEquals(1, new File(testConnection.getRepositoryPath(), "transactions").list().length);
		assertEquals(1, new File(testConnection.getRepositoryPath(), "actions").list().length);

		clientA.cleanup(cleanupOptions);

		assertEquals(1, new File(testConnection.getRepositoryPath(), "multichunks").list().length);
		assertEquals(3, new File(testConnection.getRepositoryPath(), "databases").list().length);
		assertEquals(0, new File(testConnection.getRepositoryPath(), "transactions").list().length);
		assertEquals(0, new File(testConnection.getRepositoryPath(), "actions").list().length);
	}
}
