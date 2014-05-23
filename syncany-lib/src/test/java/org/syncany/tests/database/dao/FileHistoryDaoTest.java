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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDatabaseUtil;
import org.syncany.tests.util.TestSqlDatabaseUtil;

public class FileHistoryDaoTest {	
	@Test
	public void testGetFileHistoriesWithFileVersionByVectorClock() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();
				
		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql"); 

		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		
		List<PartialFileHistory> historiesFromA1 = fileHistoryDao.getFileHistoriesWithFileVersions(TestDatabaseUtil.createVectorClock("A1"));
		List<PartialFileHistory> historiesFromA2 = fileHistoryDao.getFileHistoriesWithFileVersions(TestDatabaseUtil.createVectorClock("A2"));
		List<PartialFileHistory> historiesFromA3 = fileHistoryDao.getFileHistoriesWithFileVersions(TestDatabaseUtil.createVectorClock("A3"));
		List<PartialFileHistory> historiesFromA4 = fileHistoryDao.getFileHistoriesWithFileVersions(TestDatabaseUtil.createVectorClock("A4"));
		List<PartialFileHistory> historiesFromA5 = fileHistoryDao.getFileHistoriesWithFileVersions(TestDatabaseUtil.createVectorClock("A5"));
		List<PartialFileHistory> historiesFromB1 = fileHistoryDao.getFileHistoriesWithFileVersions(TestDatabaseUtil.createVectorClock("B1"));
		List<PartialFileHistory> historiesFromDoesNotExist = fileHistoryDao.getFileHistoriesWithFileVersions(TestDatabaseUtil.createVectorClock("DoesNotExist1"));
		
		// Test		
		assertNotNull(historiesFromA1);
		assertEquals(1, historiesFromA1.size());
		assertEquals("851c441915478a539a5bab2b263ffa4cc48e282f", historiesFromA1.get(0).getFileHistoryId().toString());
		assertEquals("fe83f217d464f6fdfa5b2b1f87fe3a1a47371196", historiesFromA1.get(0).getLastVersion().getChecksum().toString());
		
		assertNotNull(historiesFromA2);
		assertEquals(1, historiesFromA2.size());
		assertEquals("c021aecb2ae36f2a8430eb10309923454b93b61e", historiesFromA2.get(0).getFileHistoryId().toString());
		assertEquals("bf8b4530d8d246dd74ac53a13471bba17941dff7", historiesFromA2.get(0).getLastVersion().getChecksum().toString());
		
		assertNotNull(historiesFromA3);
		assertEquals(1, historiesFromA3.size());
		assertEquals("4fef2d605640813464792b18b16e1a5e07aa4e53", historiesFromA3.get(0).getFileHistoryId().toString());
		assertEquals("8ce24fc0ea8e685eb23bf6346713ad9fef920425", historiesFromA3.get(0).getLastVersion().getChecksum().toString());
		
		assertNotNull(historiesFromB1);
		assertEquals(2, historiesFromB1.size());
		
		PartialFileHistory file1 = historiesFromB1.get(0);
		PartialFileHistory file2 = historiesFromB1.get(1);
		
		assertNotNull(file1);
		assertEquals("851c441915478a539a5bab2b263ffa4cc48e282f", file1.getFileHistoryId().toString());
		assertEquals("fe83f217d464f6fdfa5b2b1f87fe3a1a47371196", file1.getLastVersion().getChecksum().toString());
		assertEquals(2, (long) file1.getLastVersion().getVersion());

		assertNotNull(file2);
		assertEquals("beef111111111111111111111111111111111111", file2.getFileHistoryId().toString());
		assertEquals("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef", file2.getLastVersion().getChecksum().toString());
		assertEquals(1, (long) file2.getLastVersion().getVersion());		

		assertNotNull(historiesFromA4);
		assertEquals(1, historiesFromA4.size());
		assertEquals("851c441915478a539a5bab2b263ffa4cc48e282f", historiesFromA4.get(0).getFileHistoryId().toString());
		assertEquals("fe83f217d464f6fdfa5b2b1f87fe3a1a47371196", historiesFromA4.get(0).getLastVersion().getChecksum().toString());
		assertEquals(2, (long) historiesFromA4.get(0).getLastVersion().getVersion());
		assertFalse(historiesFromA4.get(0).getLastVersion().equals(historiesFromB1.get(0).getLastVersion()));
		
		assertNotNull(historiesFromA5);
		assertEquals(1, historiesFromA5.size());
		assertEquals("abcdeffaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", historiesFromA5.get(0).getFileHistoryId().toString());
		assertEquals("ffffffffffffffffffffffffffffffffffffffff", historiesFromA5.get(0).getLastVersion().getChecksum().toString());
				
		assertNotNull(historiesFromDoesNotExist);
		assertEquals(0, historiesFromDoesNotExist.size());
		
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
