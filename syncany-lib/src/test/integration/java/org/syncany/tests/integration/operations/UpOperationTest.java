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
package org.syncany.tests.integration.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.database.dao.DatabaseXmlSerializer.DatabaseReadType;
import org.syncany.operations.AbstractTransferOperation;
import org.syncany.operations.up.UpOperation;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.CollectionUtil;

public class UpOperationTest {
	private Config testConfig;

	@Before
	public void setUp() throws Exception {
		testConfig = TestConfigUtil.createTestLocalConfig();
	}

	@After
	public void tearDown() throws Exception {
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}

	@Test
	public void testUploadLocalDatabase() throws Exception {
		int fileSize = 1230 * 1024;
		int fileAmount = 3;

		List<File> originalFiles = TestFileUtil.createRandomFilesInDirectory(testConfig.getLocalDir(), fileSize,
				fileAmount);

		// Run!
		AbstractTransferOperation op = new UpOperation(testConfig);
		op.execute();

		// Get databases (for comparison)
		LocalTransferSettings localConnection = (LocalTransferSettings) testConfig.getConnection();

		File localDatabaseDir = testConfig.getDatabaseDir();
		File remoteDatabaseFile = new File(localConnection.getPath() + "/databases/database-" + testConfig.getMachineName() + "-0000000001");

		assertNotNull(localDatabaseDir.listFiles());
		assertTrue(localDatabaseDir.listFiles().length > 0);
		assertTrue(remoteDatabaseFile.exists());

		// - Memory database
		DatabaseXmlSerializer dDAO = new DatabaseXmlSerializer(testConfig.getTransformer());

		MemoryDatabase remoteDatabase = new MemoryDatabase();
		dDAO.load(remoteDatabase, remoteDatabaseFile, null, null, DatabaseReadType.FULL);

		DatabaseVersion remoteDatabaseVersion = remoteDatabase.getLastDatabaseVersion();

		// - Sql Database
		SqlDatabase localDatabase = new SqlDatabase(testConfig);
		Map<FileHistoryId, PartialFileHistory> localFileHistories = localDatabase.getFileHistoriesWithFileVersions();

		// Compare!
		assertEquals(localDatabase.getLastDatabaseVersionHeader(), remoteDatabaseVersion.getHeader());

		assertEquals(localFileHistories.size(), fileAmount);
		assertEquals(localDatabase.getFileHistoriesWithFileVersions().size(), remoteDatabaseVersion.getFileHistories().size());

		Collection<PartialFileHistory> remoteFileHistories = remoteDatabaseVersion.getFileHistories();

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
