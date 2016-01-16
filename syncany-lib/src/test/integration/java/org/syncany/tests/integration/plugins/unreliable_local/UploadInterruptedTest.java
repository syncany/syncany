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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.simpleframework.xml.core.Persister;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferManagerFactory;
import org.syncany.plugins.transfer.features.TransactionAware;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.to.TransactionTO;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class UploadInterruptedTest {
	private static final Logger logger = Logger.getLogger(UploadInterruptedTest.class.getSimpleName());

	@BeforeClass
	public static void setUp() {
		// RetriableTransferManager.RETRY_SLEEP_MILLIS = 50;
	}

	@Test
	public void testUnreliableUpload_Test1_WithRetryFailsManyTimes() throws Exception {
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

						// 1st upload (= multichunk) fails
						"rel=[345] .+upload.+multichunk", // << 3 retries!!

						// Make fourth upload fail
						"rel=(8|9|10) .+upload" // << 3 retries!!
				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		int i = 0;
		while (i++ < 5) {
			clientA.createNewFile("A-original-" + i, 50 * 1024);

			try {
				Thread.sleep(100);
				clientA.up();
				clientA.down();
			}
			catch (StorageException e) {
				logger.log(Level.INFO, e.getMessage());
			}
		}

		assertTrue(new File(testConnection.getPath() + "/databases/database-A-0000000001").exists());
		assertTrue(new File(testConnection.getPath() + "/databases/database-A-0000000002").exists());
		assertTrue(new File(testConnection.getPath() + "/databases/database-A-0000000003").exists());
		assertFalse(new File(testConnection.getPath() + "/databases/database-A-0000000004").exists());
		assertFalse(new File(testConnection.getPath() + "/databases/database-A-0000000005").exists());

		// Tear down
		clientA.deleteTestData();
	}

	@Test
	public void testUnreliableUpload_Test2_FailsAtMultichunk() throws Exception {
		/*
		 * This test fails when trying to upload the first multichunk.
		 */

		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

						".+upload.+multichunk",
				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		clientA.createNewFile("A-original", 10);

		boolean upFailed = false;

		try {
			clientA.up();
		}
		catch (StorageException e) {
			upFailed = true;
			logger.log(Level.INFO, e.getMessage());
		}

		assertTrue(upFailed);
		assertEquals(0, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/actions/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/transactions/").listFiles().length);

		File transactionFile = new File(testConnection.getPath() + "/transactions/").listFiles()[0];
		TransactionTO transactionTO = new Persister().read(TransactionTO.class, transactionFile);

		assertEquals(2, transactionTO.getActions().size());
		assertTrue(transactionTO.getActions().get(0).getRemoteFile().getName().contains("multichunk-"));
		assertTrue(transactionTO.getActions().get(1).getRemoteFile().getName().contains("database-"));

		// Tear down
		clientA.deleteTestData();
	}

	@Test
	public void testUnreliableUpload_Test3_FailsAtDatabaseFile() throws Exception {
		/*
		 * This test fails when trying to upload the first database file, but succeeds on retry
		 *
		 * 1. upload(action-up-987, actions/action-up-987)
		 * 2. upload(transaction-123, transactions/transaction-123)
		 * 3. upload(multichunk-1, temp-1)
		 * 5. upload(database-123, temp-2) <<< FAILS HERE
		 * 6. move(temp-1, multichunks/multichunk-1)
		 * 8. move(temp-2, databases/database-123)
		 */

		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

						"rel=[456].+upload.+database",
				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		// 1. First upload fails
		clientA.createNewFile("A-original", 10);

		boolean upFailed = false;

		try {
			clientA.up();
		}
		catch (StorageException e) {
			upFailed = true;
			logger.log(Level.INFO, e.getMessage());
		}

		assertTrue(upFailed);
		assertEquals(0, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/actions/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/transactions/").listFiles().length);

		assertEquals(1, new File(testConnection.getPath() + "/temporary/").listFiles().length);

		File transactionFile = new File(testConnection.getPath() + "/transactions/").listFiles()[0];
		TransactionTO transactionTO = new Persister().read(TransactionTO.class, transactionFile);

		assertEquals(2, transactionTO.getActions().size());
		assertTrue(transactionTO.getActions().get(0).getRemoteFile().getName().contains("multichunk-"));
		assertTrue(transactionTO.getActions().get(1).getRemoteFile().getName().contains("database-"));

		// 2. Second try succeeds and must clean up the transactions
		clientA.up();

		assertEquals(1, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/actions/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/transactions/").listFiles().length);

		assertEquals(0, new File(testConnection.getPath() + "/temporary/").listFiles().length);

		// Tear down
		clientA.deleteTestData();
	}

	@Test
	public void testUnreliableUpload_Test4_1_FailsAtSecondMultiChunkUpload() throws Exception {
		/*
		 * This test fails when trying to upload the second multichunk, but succeeds on retry
		 *
		 * 1. upload(action-up-987, actions/action-up-987)
		 * 2. upload(transaction-123, transactions/transaction-123)
		 * 3. upload(multichunk-1, temp-1)
		 * 4. upload(multichunk-2, temp-2) <<< FAILS HERE
		 * 5. upload(database-123, temp-3) 6. move(temp-1, multichunks/multichunk-1)
		 * 7. move(temp-2, multichunks/multichunk-2)
		 * 8. move(temp-3, databases/database-123)
		 */

		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

						"rel=[456].+upload.+multichunk",
				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		clientA.createNewFile("A-original", 5 * 1024 * 1024); // << larger than one multichunk!

		boolean upFailed = false;

		try {
			clientA.up();
		}
		catch (StorageException e) {
			upFailed = true;
			logger.log(Level.INFO, e.getMessage());
		}

		assertTrue(upFailed);
		assertEquals(0, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/actions/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/transactions/").listFiles().length);

		File[] tempFiles = new File(testConnection.getPath() + "/temporary/").listFiles();

		assertEquals(1, tempFiles.length);
		assertTrue(tempFiles[0].length() > 500 * 1024); // 1 MC with 1 MB, 1 with 4 MB; must be larger than 500 KB

		File transactionFile = new File(testConnection.getPath() + "/transactions/").listFiles()[0];
		TransactionTO transactionTO = new Persister().read(TransactionTO.class, transactionFile);

		assertEquals(3, transactionTO.getActions().size());
		assertTrue(transactionTO.getActions().get(0).getRemoteFile().getName().contains("multichunk-"));
		assertTrue(transactionTO.getActions().get(1).getRemoteFile().getName().contains("multichunk-"));
		assertTrue(transactionTO.getActions().get(2).getRemoteFile().getName().contains("database-"));

		// 2. Second try succeeds and must clean up the transactions
		clientA.up();

		assertEquals(1, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(2, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/actions/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/transactions/").listFiles().length);

		assertEquals(0, new File(testConnection.getPath() + "/temporary/").listFiles().length);

		// Tear down
		clientA.deleteTestData();
	}

	@Test
	public void testUnreliableUpload_Test4_2_FailsAtTXCommitDuring2ndMultiChunkMove() throws Exception {
		/*
		 * First run "Client A": This test fails when trying to execute the TX.commit() when moving the second multichunk. So the first multichunk was
		 * moved successfully.
		 *
		 * Double check by "Client B": Client B should not see this multichunk on TM.list()
		 *
		 * Second run "Client A": The second up() from Client A should revert the transaction. To verify this, we let the second run fail at the
		 * transaction file upload
		 *
		 * 1. upload(action-up-987, actions/action-up-987)
		 * 2. upload(transaction-123, transactions/transaction-123) <<< FAILS HERE (second run)
		 * 3. upload(multichunk-1, temp-1)
		 * 4. upload(multichunk-2, temp-2)
		 * 5. upload(database-123, temp-3)
		 * 6. move(temp-1, multichunks/multichunk-1)
		 * 7. move(temp-2, multichunks/multichunk-2) <<< FAILS HERE (first run)
		 * 8. move(temp-3, databases/database-123)
		 */

		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

						"rel=[234].+move.+multichunk",
						"rel=(7|8|9).+upload.+transaction",
				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		clientA.createNewFile("A-original", 5 * 1024 * 1024); // << larger than one multichunk!

		boolean firstUpFailed = false;

		try {
			clientA.up();
		}
		catch (StorageException e) {
			firstUpFailed = true;
			logger.log(Level.INFO, e.getMessage());
		}

		assertTrue(firstUpFailed);
		assertEquals(0, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/actions/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/transactions/").listFiles().length);

		File[] tempFiles = new File(testConnection.getPath() + "/temporary/").listFiles();

		assertEquals(2, tempFiles.length);
		assertTrue(tempFiles[0].length() > 500 * 1024 || tempFiles[1].length() > 500 * 1024); // The second multichunk should be >500 KB
		assertTrue(tempFiles[0].length() < 100 * 1024 || tempFiles[1].length() < 100 * 1024); // The database file should be <100 KB

		File transactionFile = new File(testConnection.getPath() + "/transactions/").listFiles()[0];
		TransactionTO transactionTO = new Persister().read(TransactionTO.class, transactionFile);

		assertEquals(3, transactionTO.getActions().size());
		assertTrue(transactionTO.getActions().get(0).getRemoteFile().getName().contains("multichunk-"));
		assertTrue(transactionTO.getActions().get(1).getRemoteFile().getName().contains("multichunk-"));
		assertTrue(transactionTO.getActions().get(2).getRemoteFile().getName().contains("database-"));

		// 2. Double check if list() does not return the multichunk
		TransferManager transferManager = TransferManagerFactory.build(clientA.getConfig()).withFeature(TransactionAware.class).asDefault();
		Map<String, MultichunkRemoteFile> multiChunkList = transferManager.list(MultichunkRemoteFile.class);
		assertEquals(0, multiChunkList.size());

		// 3. Second try fails in the beginning, to see if cleanTransactions was successful
		boolean secondUpFailed = false;

		// Do not resume, since we want to clean transactions.
		UpOperationOptions upOptions = new UpOperationOptions();
		upOptions.setResume(false);
		try {
			clientA.up(upOptions);
		}
		catch (StorageException e) {
			secondUpFailed = true;
			logger.log(Level.INFO, e.getMessage());
		}

		assertTrue(secondUpFailed);
		assertEquals(0, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(2, new File(testConnection.getPath() + "/actions/").listFiles().length); // Shouldn't this be 1
		assertEquals(0, new File(testConnection.getPath() + "/transactions/").listFiles().length);

		assertEquals(0, new File(testConnection.getPath() + "/").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.contains("temp-");
			}
		}).length);

		// Tear down
		clientA.deleteTestData();
	}

	@Test
	public void testUnreliableUpload_Test4_3_FailsAtTXCommitDuring2ndMultiChunkMoveAndDuringTXRollback() throws Exception {
		/*
		 * 1. upload(action-up-987, actions/action-up-987)
		 * 2. upload(transaction-123, transactions/transaction-123)
		 * 3. upload(multichunk-1, temp-1)
		 * 4. upload(multichunk-2, temp-2)
		 * 5. upload(database-123, temp-3)
		 * 6. move(temp-1, multichunks/multichunk-1)
		 * 7. move(temp-2, multichunks/multichunk-2) <<< FAILS HERE (first run)
		 * 8. move(temp-3, databases/database-123)
		 *
		 * 1. upload(action-up-987, actions/action-up-987)
		 * 2. list(databases/*)
		 * 3. list(transactions/*)
		 * 4. upload(transaction-345, transactions/transaction-345) (rollback TX)
		 * 5. move(multichunks/multichunk-1, temp-80)
		 * 6. move(temp-1, temp-81) (silently fails, b/c temp-1 does not exist)
		 * 7. move(multichunks/multichunk-2, temp-82) (silently fails, b/c multichunk-2 does not exist)
		 * 8. move(temp-2, temp-83)
		 * 9. move(databases/database-123, temp-84) (silently fails, b/c database-123 does not exist)
		 * 10. move(temp-3, temp-85)
		 * 10 move(transactions-345, temp-86)
		 * 11. delete(temp-80)
		 * 12. delete(temp-83) <<< FAILS HERE (second run)
		 *
		 * Expected: temp-(83,85,86)
		 */

		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

						"rel=[234].+move.+multichunk",
						"rel=[567].+delete.+database",
				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		clientA.createNewFile("A-original", 5 * 1024 * 1024); // << larger than one multichunk!

		boolean firstUpFailed = false;

		try {
			clientA.up();
		}
		catch (StorageException e) {
			firstUpFailed = true;
			logger.log(Level.INFO, e.getMessage());
		}

		assertTrue(firstUpFailed);
		assertEquals(0, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/actions/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/transactions/").listFiles().length);

		File[] tempFiles = new File(testConnection.getPath() + "/temporary").listFiles();

		assertEquals(2, tempFiles.length);
		assertTrue(tempFiles[0].length() > 500 * 1024 || tempFiles[1].length() > 500 * 1024); // The second multichunk should be >500 KB
		assertTrue(tempFiles[0].length() < 100 * 1024 || tempFiles[1].length() < 100 * 1024); // The database file should be <100 KB

		File transactionFile = new File(testConnection.getPath() + "/transactions/").listFiles()[0];
		TransactionTO transactionTO = new Persister().read(TransactionTO.class, transactionFile);

		assertEquals(3, transactionTO.getActions().size());
		assertTrue(transactionTO.getActions().get(0).getRemoteFile().getName().contains("multichunk-"));
		assertTrue(transactionTO.getActions().get(1).getRemoteFile().getName().contains("multichunk-"));
		assertTrue(transactionTO.getActions().get(2).getRemoteFile().getName().contains("database-"));

		// 2. Double check if list() does not return the multichunk
		TransferManager transferManager = TransferManagerFactory.build(clientA.getConfig()).withFeature(TransactionAware.class).asDefault();
		Map<String, MultichunkRemoteFile> multiChunkList = transferManager.list(MultichunkRemoteFile.class);
		assertEquals(0, multiChunkList.size());

		// 3. Second try fails in the beginning, to see if cleanTransactions was successful
		boolean secondUpFailed = false;

		// Do not resume, since we want to clean transactions.
		UpOperationOptions upOptions = new UpOperationOptions();
		upOptions.setResume(false);

		try {
			clientA.up(upOptions);
		}
		catch (StorageException e) {
			secondUpFailed = true;
			logger.log(Level.INFO, e.getMessage());
		}

		assertTrue(secondUpFailed);
		assertEquals(0, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(2, new File(testConnection.getPath() + "/actions/").listFiles().length); // left over, 2 failed ops
		assertEquals(1, new File(testConnection.getPath() + "/transactions/").listFiles().length);
		assertEquals(1, new File(testConnection.getPath() + "/temporary/").listFiles().length);

		// 4. Third try; this should finally succeed
		clientA.up(upOptions);

		assertEquals(1, new File(testConnection.getPath() + "/databases/").listFiles().length);
		assertEquals(2, new File(testConnection.getPath() + "/multichunks/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/actions/").listFiles().length); // cleaned
		assertEquals(0, new File(testConnection.getPath() + "/transactions/").listFiles().length);
		assertEquals(0, new File(testConnection.getPath() + "/temporary/").listFiles().length); // cleaned

		// Tear down
		clientA.deleteTestData();
	}

	@Test
	public void testUnreliableUpload_FailOnceThenResume() throws Exception {
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

						// 1st upload (= multichunk) fails
						"rel=[345] .+upload.+multichunk", // << 3 retries!!

				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		clientA.createNewFile("file-1.txt");

		boolean failed = false;
		try {
			clientA.up();
		}
		catch (StorageException e) {
			failed = true;
		}

		assertTrue(failed);

		TestClient clientB = new TestClient("B", testConnection);
		clientB.down();

		assertEquals(0, clientB.getLocalFiles().size());

		UpOperationOptions upOptions = new UpOperationOptions();
		upOptions.setResume(true);
		upOptions.setForceUploadEnabled(true);
		clientA.up(upOptions);

		clientB.down();

		assertEquals(1, clientB.getLocalFiles().size());
		assertTrue(clientB.getLocalFile("file-1.txt").exists());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testUnreliableUpload_FailResume() throws Exception {
		// Setup
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList(new String[] {
						// List of failing operations (regex)
						// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>

						// 1st upload (= multichunk) fails
						"rel=[345] .+upload.+multichunk", // << 3 retries!!

						// In second up, the database upload fails
						"rel=(8|9|10) .+upload.+database", // << 3 retries!!

				}
						));

		TestClient clientA = new TestClient("A", testConnection);

		clientA.createNewFile("file-1.txt");

		boolean failed = false;
		try {
			clientA.up();
		}
		catch (StorageException e) {
			failed = true;
		}

		assertTrue(failed);

		UpOperationOptions upOptions = new UpOperationOptions();
		upOptions.setResume(true);
		upOptions.setForceUploadEnabled(true);

		// Try to resume and fail
		failed = false;
		try {
			clientA.up(upOptions);
		}
		catch (StorageException e) {
			failed = true;
		}

		assertTrue(failed);

		TestClient clientB = new TestClient("B", testConnection);
		clientB.down();

		assertEquals(0, clientB.getLocalFiles().size());

		clientA.up(upOptions);

		clientB.down();

		assertEquals(1, clientB.getLocalFiles().size());
		assertTrue(clientB.getLocalFile("file-1.txt").exists());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
