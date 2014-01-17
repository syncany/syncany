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
package org.syncany.tests.database.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlDatabaseUtil;

public class FileHistoryDaoTest {	
	@Test
	public void testGetFileHistoryWithLastVersionByIdAndPath() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();
				
		// Run
		// TODO [low] This set is identical to test.fileversion.insert.getFileTreeAtDate.sql -- make new set!
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.filehistory.insert.set1.sql"); 

		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		
		PartialFileHistory fileHistory1Deleted = fileHistoryDao.getFileHistoryWithLastVersion(FileHistoryId.parseFileId("851c441915478a539a5bab2b263ffa4cc48e282f"));
		PartialFileHistory fileHistory1ByIdNew = fileHistoryDao.getFileHistoryWithLastVersion(FileHistoryId.parseFileId("abcdeffaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
		PartialFileHistory fileHistory2ById = fileHistoryDao.getFileHistoryWithLastVersion(FileHistoryId.parseFileId("c021aecb2ae36f2a8430eb10309923454b93b61e"));
		PartialFileHistory fileHistory3ById = fileHistoryDao.getFileHistoryWithLastVersion(FileHistoryId.parseFileId("4fef2d605640813464792b18b16e1a5e07aa4e53"));
		
		PartialFileHistory fileHistory1ByPathNew = fileHistoryDao.getFileHistoryWithLastVersion("file1");
		PartialFileHistory fileHistory2ByPath = fileHistoryDao.getFileHistoryWithLastVersion("file2");
		PartialFileHistory fileHistory3ByPath = fileHistoryDao.getFileHistoryWithLastVersion("file3");
				
		// Test
		
		// - File 1 (deleted)
		assertNull(fileHistory1Deleted);
		
		// - File 1 (new)
		assertNotNull(fileHistory1ByPathNew);
		assertEquals(1, fileHistory1ByPathNew.getFileVersions().size());		
		assertNotNull(fileHistory1ByPathNew.getLastVersion().getChecksum());
		assertEquals("ffffffffffffffffffffffffffffffffffffffff", fileHistory1ByPathNew.getLastVersion().getChecksum().toString());		
		assertEquals("rw-r--r--", fileHistory1ByPathNew.getLastVersion().getPosixPermissions());
		assertNull(fileHistory1ByPathNew.getLastVersion().getDosAttributes());		

		assertEquals(fileHistory1ByIdNew, fileHistory1ByPathNew);
		
		// - File 2 
		assertNotNull(fileHistory2ByPath);
		assertEquals(1, fileHistory2ByPath.getFileVersions().size());		
		assertNotNull(fileHistory2ByPath.getLastVersion().getChecksum());
		assertEquals("bf8b4530d8d246dd74ac53a13471bba17941dff7", fileHistory2ByPath.getLastVersion().getChecksum().toString());
		
		assertEquals(fileHistory2ById, fileHistory2ByPath);

		// - File 3 
		assertNotNull(fileHistory3ByPath);
		assertEquals(1, fileHistory3ByPath.getFileVersions().size());		
		assertNotNull(fileHistory3ByPath.getLastVersion().getChecksum());
		assertEquals("8ce24fc0ea8e685eb23bf6346713ad9fef920425", fileHistory3ByPath.getLastVersion().getChecksum().toString());
		
		assertEquals(fileHistory3ById, fileHistory3ByPath);
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}	
	
	/*
	    // TODO [medium] Implement tests for other PartialFileHistory methods
	 
	  	fileHistoryDao.getFileHistoriesWithFileVersions()
		fileHistoryDao.getFileHistoriesWithLastVersion()
		fileHistoryDao.getFileHistoriesWithLastVersionByChecksum(fileContentChecksum)				
	 */
}
