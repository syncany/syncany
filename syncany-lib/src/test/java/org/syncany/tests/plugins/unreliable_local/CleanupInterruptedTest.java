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

import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.plugins.transfer.RetriableTransferManager;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransactionAwareTransferManager;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.files.ActionRemoteFile;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferPlugin;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class CleanupInterruptedTest {
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
		clientA.upWithForceChecksum();

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
		TransferManager transferManager = new TransactionAwareTransferManager(
				new UnreliableLocalTransferPlugin().createTransferManager(testConnection, null), null);
		assertEquals(2, transferManager.list(MultichunkRemoteFile.class).size());
		assertEquals(2, new File(testConnection.getPath(), "multichunks").list().length);
		assertEquals(2, transferManager.list(DatabaseRemoteFile.class).size());
		assertEquals(2, new File(testConnection.getPath(), "databases").list().length);
		assertEquals(1, transferManager.list(TransactionRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "transactions").list().length);
		assertEquals(1, transferManager.list(ActionRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "actions").list().length);

		clientA.cleanup(cleanupOptions);
		assertEquals(1, transferManager.list(MultichunkRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "multichunks").list().length);
		assertEquals(3, transferManager.list(DatabaseRemoteFile.class).size());
		assertEquals(3, new File(testConnection.getPath(), "databases").list().length);
		assertEquals(0, transferManager.list(TransactionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "transactions").list().length);
		assertEquals(0, transferManager.list(ActionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "actions").list().length);

		clientA.deleteTestData();
	}

	@Test
	public void testUnreliableCleanup_Test2_databaseFileMerge() throws Exception {
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
		clientA.upWithForceChecksum();

		CleanupOperationOptions cleanupOptions = new CleanupOperationOptions();
		cleanupOptions.setMaxDatabaseFiles(1);
		boolean cleanupFailed = false;
		try {
			clientA.cleanup(cleanupOptions);
		}
		catch (StorageException e) {
			cleanupFailed = true;
		}

		assertTrue(cleanupFailed);
		TransferManager transferManager = new TransactionAwareTransferManager(
				new UnreliableLocalTransferPlugin().createTransferManager(testConnection, null), null);
		assertEquals(2, transferManager.list(MultichunkRemoteFile.class).size());
		assertEquals(2, new File(testConnection.getPath(), "multichunks").list().length);

		// Note that the list here differs from the actual files, because the transaction fails
		// while deletions have been done
		assertEquals(2, transferManager.list(DatabaseRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "databases").list().length);

		assertEquals(1, transferManager.list(TransactionRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "transactions").list().length);

		assertEquals(1, transferManager.list(ActionRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "actions").list().length);

		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		File tempFile = File.createTempFile("multichunk", "", tempDir);
		for (RemoteFile remoteFile : transferManager.list(DatabaseRemoteFile.class).values()) {
			transferManager.download(remoteFile, tempFile);
			assertTrue(tempFile.exists());
			tempFile.delete();
		}

		clientA.cleanup(cleanupOptions);

		assertEquals(2, transferManager.list(MultichunkRemoteFile.class).size());
		assertEquals(2, new File(testConnection.getPath(), "multichunks").list().length);

		assertEquals(1, transferManager.list(DatabaseRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "databases").list().length);

		assertEquals(0, transferManager.list(TransactionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "transactions").list().length);

		assertEquals(0, transferManager.list(ActionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "actions").list().length);
	}

	@Test
	public void testUnreliableCleanup_Test3_unreferencedTempFiles() throws Exception {
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>
						"rel=[678].+delete.+temp", // << 3 retries!!
				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		clientA.createNewFile("file");

		clientA.up();

		clientA.changeFile("file");
		clientA.upWithForceChecksum();

		CleanupOperationOptions cleanupOptions = new CleanupOperationOptions();
		cleanupOptions.setMaxDatabaseFiles(1);
		boolean cleanupFailed = false;
		try {
			clientA.cleanup(cleanupOptions);
		}
		catch (StorageException e) {
			cleanupFailed = true;
		}

		TransferManager transferManager = new TransactionAwareTransferManager(
				new UnreliableLocalTransferPlugin().createTransferManager(testConnection, null), null);

		assertTrue(cleanupFailed);
		assertEquals(2, transferManager.list(MultichunkRemoteFile.class).size());
		assertEquals(2, new File(testConnection.getPath(), "multichunks").list().length);

		assertEquals(1, transferManager.list(DatabaseRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "databases").list().length);

		assertEquals(0, transferManager.list(TransactionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "transactions").list().length);

		assertEquals(1, transferManager.list(ActionRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "actions").list().length);

		// Two temporary files left (first deletion failed)
		assertEquals(2, transferManager.list(TempRemoteFile.class).size());
		assertEquals(2, new File(testConnection.getPath(), "temporary").list().length);

		clientA.cleanup(cleanupOptions);

		// Functional cleanup results in removal of action file and unreferenced files
		assertEquals(0, transferManager.list(ActionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "actions").list().length);

		assertEquals(0, transferManager.list(TempRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "temporary").list().length);
	}
}
