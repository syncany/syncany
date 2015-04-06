package org.syncany.tests.integration.scenarios;

import org.junit.Test;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.unreliable_local.UnreliableLocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

import java.io.File;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test is designed for the scenario where a sync up consisting of multiple transactions fails during the
 * second mulitchunk of the first transaction. Expected outcome is that transaction and database files are
 * written to the state folder for all remaining transactions, and that the first transaction is resumed from
 * the second multichunk onwards.
 *
 * @author Tim Hegeman
 */
public class FailedSplitSyncUpScenarioTest {

	@Test
	public void testUpFailsOnFirstTransaction() throws Exception {

		// Inject failure for the second multichunk
		UnreliableLocalTransferSettings testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
				Arrays.asList("rel=[4567].+upload.+multichunk"));

		TestClient clientA = new TestClient("A", testConnection);
		UpOperationOptions options = new UpOperationOptions();
		options.setTransactionSizeLimit(0L);

		// Write three files (three transactions), with the first file spanning two multichunks
		clientA.createNewFile("file1", 5 * 1024 * 1024);
		clientA.createNewFile("file2", 1024);
		clientA.createNewFile("file3", 1024);

		// 1. Attempt upload, should fail
		boolean operationFailed = false;
		try {
			clientA.up(options);
		}
		catch (Exception ex) {
			operationFailed = true;
		}
		assertTrue(operationFailed);

		// 2. Verify local state
		File stateDir = clientA.getConfig().getStateDir();
		File cacheDir = clientA.getConfig().getCacheDir();
		// Expecting: 3 transactions + 3 databases + transaction list + in-progress transaction
		assertEquals(8, stateDir.listFiles().length);
		// Expecting: 3 databases + 4 multichunks + in-progress transaction
		assertEquals(8, cacheDir.listFiles().length);

		// 3. Verify remote state
		File repoActionsDir = new File(testConnection.getPath() + "/actions");
		File repoDatabasesDir = new File(testConnection.getPath() + "/databases");
		File repoMultichunksDir = new File(testConnection.getPath() + "/multichunks");
		File repoTemporaryDir = new File(testConnection.getPath() + "/temporary");
		File repoTransactionsDir = new File(testConnection.getPath() + "/transactions");
		// Expecting that no databases/multichunks have been committed, 1 multichunk is temporary, 1 action and transaction are pending
		assertEquals("One pending action should exist in repo", 1, repoActionsDir.listFiles().length);
		assertEquals("No database should be committed in repo", 0, repoDatabasesDir.listFiles().length);
		assertEquals("No multichunk should be committed in repo", 0, repoMultichunksDir.listFiles().length);
		assertEquals("One multichunk should exist in repo as temporary", 1, repoTemporaryDir.listFiles().length);
		assertEquals("One pending transaction should exist in repo", 1, repoTransactionsDir.listFiles().length);

		// 4. Resume operation
		clientA.up();

		// 5. Final state should be as if no failure occurred; three database versions, three complete files
		assertEquals("Three databases should be committed in repo", 3, repoDatabasesDir.listFiles().length);
		for (int fileNumber = 1; fileNumber <= 3; fileNumber++) {
			DatabaseRemoteFile databaseRemoteFile = new DatabaseRemoteFile("A", fileNumber);
			File databaseFile = new File(testConnection.getPath() + "/databases/" + databaseRemoteFile.getName());

			assertTrue("Database file should exist: " + databaseFile, databaseFile.exists());
		}

		assertEquals("Four multichunks should be committed in repo", 4, repoMultichunksDir.listFiles().length);

		// Tear down
		clientA.deleteTestData();
	}

}
