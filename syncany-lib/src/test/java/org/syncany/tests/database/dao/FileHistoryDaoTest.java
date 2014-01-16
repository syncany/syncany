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

import static org.junit.Assert.*;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlDatabaseUtil;

public class FileHistoryDaoTest {	
	@Test
	public void testFileHistoriesForDatabase() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();
				
		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.fileversion.insert.getCurrentFileTree.sql");

		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		
		fail("Implement this");
		//fileHistoryDao.getFileHistoriesForDatabaseVersion(databaseVersionVectorClock);
	
		
		Map<String, FileVersion> currentFileTree = fileVersionDao.getCurrentFileTree();
		
		// Test
		assertEquals(50, currentFileTree.size());
		
		assertNotNull(currentFileTree.get("file1"));
		assertNotNull(currentFileTree.get("file1").getChecksum());
		assertEquals("fe83f217d464f6fdfa5b2b1f87fe3a1a47371196", currentFileTree.get("file1").getChecksum().toString());
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}	
	
	// TODO [medium] Implement others
	/*
	  	fileHistoryDao.getFileHistoriesWithFileVersions()
		fileHistoryDao.getFileHistoriesWithLastVersion()
		fileHistoryDao.getFileHistoriesWithLastVersionByChecksum(fileContentChecksum)
		fileHistoryDao.getFileHistoryWithLastVersion(fileHistoryId)
		fileHistoryDao.getFileHistoryWithLastVersion(relativePath)
	 */

	private Date toDate(String dateString) throws ParseException {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(dateString);
	}
}
