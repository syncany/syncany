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
package org.syncany.tests.integration.plugins.unreliable_local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.TreeMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.cleanup.CleanupOperationOptions.TimeUnit;
import org.syncany.operations.cleanup.CleanupOperationResult;
import org.syncany.operations.cleanup.CleanupOperationResult.CleanupResultCode;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferManagerFactory;
import org.syncany.plugins.transfer.features.TransactionAware;
import org.syncany.plugins.transfer.files.ActionRemoteFile;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class CleanupInterruptedTest {
	@BeforeClass
	public static void setUp() {
		//RetriableTransferManager.RETRY_SLEEP_MILLIS = 50;
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
		cleanupOptions.setMinKeepSeconds(0);
		boolean cleanupFailed = false;
		try {
			clientA.cleanup(cleanupOptions);
		}
		catch (StorageException e) {
			cleanupFailed = true;
		}

		assertTrue(cleanupFailed); 
		TransferManager transferManagerA = TransferManagerFactory.build(clientA.getConfig()).withFeature(TransactionAware.class).asDefault();
		assertEquals(2, transferManagerA.list(MultichunkRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "multichunks").list().length);
		assertEquals(2, transferManagerA.list(DatabaseRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "databases").list().length);
		assertEquals(1, transferManagerA.list(TransactionRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "transactions").list().length);
		assertEquals(1, transferManagerA.list(ActionRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "actions").list().length);

		clientA.cleanup(cleanupOptions);
		assertEquals(1, transferManagerA.list(MultichunkRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "multichunks").list().length);
		assertEquals(1, transferManagerA.list(DatabaseRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "databases").list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("database-");
			}
		}).length);
		assertEquals(0, transferManagerA.list(TransactionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "transactions").list().length);
		assertEquals(0, transferManagerA.list(ActionRemoteFile.class).size());
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
		cleanupOptions.setPurgeFileVersionSettings(new TreeMap<Long, TimeUnit>());
		boolean cleanupFailed = false;
		try {
			clientA.cleanup(cleanupOptions);
		}
		catch (StorageException e) {
			cleanupFailed = true;
		}

		assertTrue(cleanupFailed);
		TransferManager transferManagerA = TransferManagerFactory.build(clientA.getConfig()).withFeature(TransactionAware.class).asDefault();
		assertEquals(2, transferManagerA.list(MultichunkRemoteFile.class).size());
		assertEquals(2, new File(testConnection.getPath(), "multichunks").list().length);

		// Note that the list here differs from the actual files, because the transaction fails
		// while deletions have been done
		assertEquals(2, transferManagerA.list(DatabaseRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "databases").list().length);

		assertEquals(1, transferManagerA.list(TransactionRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "transactions").list().length);

		assertEquals(1, transferManagerA.list(ActionRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "actions").list().length);

		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		File tempFile = File.createTempFile("multichunk", "", tempDir);
		for (RemoteFile remoteFile : transferManagerA.list(DatabaseRemoteFile.class).values()) {
			transferManagerA.download(remoteFile, tempFile);
			assertTrue(tempFile.exists());
			tempFile.delete();
		}

		// Cleanup should have merged the two files.
		CleanupOperationResult result = clientA.cleanup(cleanupOptions);
		assertEquals(CleanupResultCode.OK, result.getResultCode());
		assertEquals(2, result.getMergedDatabaseFilesCount());

		assertEquals(2, transferManagerA.list(MultichunkRemoteFile.class).size());
		assertEquals(2, new File(testConnection.getPath(), "multichunks").list().length);

		assertEquals(1, transferManagerA.list(DatabaseRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "databases").list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("database-");
			}
		}).length);

		assertEquals(0, transferManagerA.list(TransactionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "transactions").list().length);

		assertEquals(0, transferManagerA.list(ActionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "actions").list().length);

		clientA.deleteTestData();
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
		cleanupOptions.setMinSecondsBetweenCleanups(0);
		cleanupOptions.setMinKeepSeconds(0);
		clientA.cleanup(cleanupOptions);

		TransferManager transferManagerA = TransferManagerFactory.build(clientA.getConfig()).withFeature(TransactionAware.class).asDefault();

		assertEquals(1, transferManagerA.list(MultichunkRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "multichunks").list().length);

		assertEquals(1, transferManagerA.list(DatabaseRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "databases").list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("database-");
			}
		}).length);

		assertEquals(0, transferManagerA.list(TransactionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "transactions").list().length);

		assertEquals(0, transferManagerA.list(ActionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "actions").list().length);

		// One deletion failed
		assertEquals(1, transferManagerA.list(TempRemoteFile.class).size());
		assertEquals(1, new File(testConnection.getPath(), "temporary").list().length);

		// Change something to trigger cleanup
		clientA.changeFile("file");
		clientA.up();

		CleanupOperationResult result = clientA.cleanup(cleanupOptions);

		// Functional cleanup results in removal of action file and unreferenced files
		assertEquals(0, transferManagerA.list(ActionRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "actions").list().length);

		assertEquals(0, transferManagerA.list(TempRemoteFile.class).size());
		assertEquals(0, new File(testConnection.getPath(), "temporary").list().length);

		clientA.deleteTestData();
	}

	@Test
	public void testUnreliableCleanup_failBlocksOtherClients() throws Exception {
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>
						"rel=(12|13|14).+upload.+database", // << 3 retries!!
				}
						));

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		clientA.createNewFile("file");

		clientA.up();

		clientA.changeFile("file");
		clientA.upWithForceChecksum();
		clientB.down();

		CleanupOperationOptions cleanupOptions = new CleanupOperationOptions();
		cleanupOptions.setMinKeepSeconds(0);
		boolean cleanupFailed = false;
		try {
			clientA.cleanup(cleanupOptions);
		}
		catch (StorageException e) {
			cleanupFailed = true;
		}

		assertTrue(cleanupFailed);

		// Pretend time has passed by deleting the action file:
		TestFileUtil.deleteFile(new File(testConnection.getPath(), "/actions/").listFiles()[0]);

		CleanupOperationResult cleanupResult = clientB.cleanup(cleanupOptions);
		assertEquals(CleanupResultCode.NOK_REPO_BLOCKED, cleanupResult.getResultCode());

		clientB.createNewFile("file2");
		UpOperationResult upResult = clientB.up();
		assertEquals(UpResultCode.NOK_REPO_BLOCKED, upResult.getResultCode());

		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
