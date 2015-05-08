package org.syncany.tests.integration.operations;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.*;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.operations.AbstractTransferOperation;
import org.syncany.operations.up.UpOperation;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.CollectionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Tim Hegeman
 */
public class SplitSyncUpOperationTest {
	private Config testConfig;
	private UpOperationOptions opOptions;

	@Before
	public void setUp() throws Exception {
		testConfig = TestConfigUtil.createTestLocalConfig();
		opOptions = new UpOperationOptions();
	}

	@After
	public void tearDown() throws Exception {
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}

	@Test
	public void testUploadLocalDatabase_SingleTransactionPerFile() throws Exception {
		int fileSize = 1230 * 1024;
		int fileAmount = 3;
		int expectedTransactions = 3;

		opOptions.setTransactionSizeLimit(0L);
		testUploadLocalDatabase(fileSize, fileAmount, expectedTransactions, opOptions);
	}

	@Test
	public void testUploadLocalDatabase_MultipleTransactions() throws Exception {
		int fileSize = 1230 * 1024;
		int fileAmount = 6;
		int expectedTransactions = 3;

		opOptions.setTransactionSizeLimit(fileSize + 1);
		testUploadLocalDatabase(fileSize, fileAmount, expectedTransactions, opOptions);
	}

	private void testUploadLocalDatabase(int fileSize, int fileAmount, int expectedTransactions, UpOperationOptions options) throws Exception {
		List<File> originalFiles = TestFileUtil.createRandomFilesInDirectory(testConfig.getLocalDir(), fileSize,
				fileAmount);

		// Run!
		AbstractTransferOperation op = new UpOperation(testConfig, options);
		UpOperationResult opResult = (UpOperationResult)op.execute();

		// Ensure that the expected number of transactions has been completed to upload the files
		assertNotNull(opResult);
		assertTrue(opResult.getTransactionsCompleted() == expectedTransactions);

		// Get databases (for comparison)
		LocalTransferSettings localConnection = (LocalTransferSettings) testConfig.getConnection();

		File localDatabaseDir = testConfig.getDatabaseDir();
		assertNotNull(localDatabaseDir.listFiles());
		assertTrue(localDatabaseDir.listFiles().length > 0);

		List<File> remoteDatabaseFiles = new ArrayList<>();
		for (int transaction = 1; transaction <= expectedTransactions; transaction++) {
			String databaseVersion = String.format("%010d", transaction);
			File remoteDatabaseFile = new File(localConnection.getPath() + "/databases/database-" + testConfig.getMachineName() + "-" + databaseVersion);
			assertTrue(remoteDatabaseFile.exists());
			remoteDatabaseFiles.add(remoteDatabaseFile);
		}

		// Import remote databases into memory database
		DatabaseXmlSerializer dDAO = new DatabaseXmlSerializer(testConfig.getTransformer());
		MemoryDatabase remoteDatabase = new MemoryDatabase();
		for (File remoteDatabaseFile : remoteDatabaseFiles) {
			dDAO.load(remoteDatabase, remoteDatabaseFile, null, null, DatabaseXmlSerializer.DatabaseReadType.FULL);
		}

		// Open local SQL Database
		SqlDatabase localDatabase = new SqlDatabase(testConfig);

		// Compare!
		assertEquals(localDatabase.getLastDatabaseVersionHeader(), remoteDatabase.getLastDatabaseVersion().getHeader());

		Map<PartialFileHistory.FileHistoryId, PartialFileHistory> localFileHistories = localDatabase.getFileHistoriesWithFileVersions();
		Collection<PartialFileHistory> remoteFileHistories = remoteDatabase.getFileHistories();

		assertEquals(localDatabase.getCurrentFileTree().size(), fileAmount);
		assertEquals(localFileHistories.size(), remoteDatabase.getFileHistories().size());

		List<FileVersion> remoteFileVersions = new ArrayList<FileVersion>();
		List<FileVersion> localFileVersions = new ArrayList<FileVersion>();

		for (PartialFileHistory partialFileHistory : remoteFileHistories) {
			remoteFileVersions.add(partialFileHistory.getLastVersion());
			assertNotNull(localFileHistories.get(partialFileHistory.getFileHistoryId()));
		}

		for (PartialFileHistory partialFileHistory : localFileHistories.values()) {
			localFileVersions.add(partialFileHistory.getLastVersion());
		}

		assertTrue(CollectionUtil.containsExactly(localFileVersions, remoteFileVersions));

		compareFileVersionsAgainstOriginalFiles(originalFiles, localFileVersions);
		compareFileVersionsAgainstOriginalFiles(originalFiles, remoteFileVersions);
	}

	private void compareFileVersionsAgainstOriginalFiles(List<File> originalFiles, List<FileVersion> localFileVersions) throws Exception {
		int toFind = originalFiles.size();
		for (File originalFile : originalFiles) {
			String originalFileName = originalFile.getName();

			for (FileVersion fileVersion : localFileVersions) {
				String fileVersionFileName = fileVersion.getName();

				if (fileVersionFileName.equals(originalFileName)) {
					toFind--;
				}
			}
		}
		assertEquals(0, toFind);
	}
}
