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
package org.syncany.tests.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader.DatabaseVersionType;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.SqlDatabase;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.operations.up.UpOperation;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.CollectionUtil;

public class SyncUpOperationTest {
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
		UpOperation op = new UpOperation(testConfig);		
		op.execute();

		// Get databases (for comparison)
		LocalConnection localConnection = (LocalConnection) testConfig.getConnection();
		
		File localDatabaseDir = testConfig.getDatabaseDir();
		File remoteDatabaseFile = new File(localConnection.getRepositoryPath() + "/databases/db-" + testConfig.getMachineName()+"-0000000001");
		
		assertNotNull(localDatabaseDir.listFiles());
		assertTrue(localDatabaseDir.listFiles().length > 0);
		assertTrue(remoteDatabaseFile.exists());
		
		// - Memory database
		DatabaseXmlSerializer dDAO = new DatabaseXmlSerializer(testConfig.getTransformer());
		
		MemoryDatabase remoteDatabase = new MemoryDatabase();		
		dDAO.load(remoteDatabase, remoteDatabaseFile, DatabaseVersionType.DEFAULT);
		
		DatabaseVersion remoteDatabaseVersion = remoteDatabase.getLastDatabaseVersion();
		
		// - Sql Database
		SqlDatabase localDatabase = new SqlDatabase(testConfig);
		Collection<PartialFileHistory> localFileHistories = localDatabase.getFileHistoriesWithFileVersions();
		
		// Compare!
		assertEquals(localDatabase.getLastDatabaseVersionHeader(), remoteDatabaseVersion.getHeader());

		assertEquals(localFileHistories.size(), fileAmount);
		assertEquals(localDatabase.getFileHistoriesWithFileVersions().size(), remoteDatabaseVersion.getFileHistories().size());
		
		Collection<PartialFileHistory> remoteFileHistories = remoteDatabaseVersion.getFileHistories();
	
		List<FileVersion> remoteFileVersions = new ArrayList<FileVersion>(); 
		List<FileVersion> localFileVersions = new ArrayList<FileVersion>();
		
		for (PartialFileHistory partialFileHistory : remoteFileHistories) {
			remoteFileVersions.add(partialFileHistory.getLastVersion());
			assertTrue(localFileHistories.contains(partialFileHistory));
		}
		
		for (PartialFileHistory partialFileHistory : localFileHistories) {
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
				
				if(fileVersionFileName.equals(originalFileName)) {
					toFind--;
				}
			}
		}
		assertEquals(0, toFind);
	}
}
