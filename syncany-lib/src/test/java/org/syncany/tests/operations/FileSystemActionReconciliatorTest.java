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
package org.syncany.tests.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.down.FileSystemActionReconciliator;
import org.syncany.operations.down.actions.FileSystemAction;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDatabaseUtil;
import org.syncany.tests.util.TestSqlDatabase;

public class FileSystemActionReconciliatorTest {
	@Test
	public void testFileSystemActionReconDeleteNonExistingFolder() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		Config testConfigA = clientA.getConfig();
		
		// - Create first database version
		clientA.createNewFolder("new folder/some subfolder");
		clientA.upWithForceChecksum();
		
		clientA.deleteFile("new folder/some subfolder"); // Delete this!
		
		// - Create new version (delete folder)
		TestSqlDatabase sqlDatabaseA = new TestSqlDatabase(testConfigA);
		PartialFileHistory folderFileHistoryWithLastVersion = sqlDatabaseA.getFileHistoryWithLastVersion("new folder/some subfolder");

		FileVersion deletedFolderVersion = folderFileHistoryWithLastVersion.getLastVersion().clone();
		deletedFolderVersion.setStatus(FileStatus.DELETED);
		deletedFolderVersion.setVersion(deletedFolderVersion.getVersion()+1);
		
		PartialFileHistory deletedFolderVersionHistory = new PartialFileHistory(folderFileHistoryWithLastVersion.getFileHistoryId());
		deletedFolderVersionHistory.addFileVersion(deletedFolderVersion);
		
		DatabaseVersion winnersDatabaseVersion = TestDatabaseUtil.createDatabaseVersion(sqlDatabaseA.getLastDatabaseVersionHeader());		
		winnersDatabaseVersion.addFileHistory(deletedFolderVersionHistory);
		
		// - Create memory database with this version
		MemoryDatabase winnersDatabase = new MemoryDatabase();
		winnersDatabase.addDatabaseVersion(winnersDatabaseVersion);
		
		// Run! Finally!
		DownOperationResult outDownOperationResult = new DownOperationResult();
		FileSystemActionReconciliator fileSystemActionReconciliator = new FileSystemActionReconciliator(testConfigA, outDownOperationResult.getChangeSet());
		List<FileSystemAction> fileSystemActions = fileSystemActionReconciliator.determineFileSystemActions(winnersDatabase);
		
		assertNotNull(fileSystemActions);
		assertEquals(0, fileSystemActions.size());		
		
		// Tear down
		clientA.deleteTestData();
	}
}
