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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.dao.ApplicationSqlDao;
import org.syncany.database.dao.ChunkSqlDao;
import org.syncany.database.dao.DatabaseVersionSqlDao;
import org.syncany.database.dao.FileContentSqlDao;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.database.dao.MultiChunkSqlDao;
import org.syncany.tests.util.TestCollectionUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlDatabaseUtil;
import org.syncany.util.CollectionUtil;

public class DatabaseVersionDaoTest {
	@Test
	public void testGetDatabaseVersionsMasterAndDirty() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set1.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileHistoryDao, multiChunkDao);
		
		Iterator<DatabaseVersion> databaseVersionsMaster = databaseVersionDao.getDatabaseVersions(DatabaseVersionStatus.MASTER);
		Iterator<DatabaseVersion> databaseVersionsDirty = databaseVersionDao.getDatabaseVersions(DatabaseVersionStatus.DIRTY);
		
		// Test
		
		// - MASTER
		assertNotNull(databaseVersionsMaster);
		
		List<DatabaseVersion> databaseVersionsMasterList = TestCollectionUtil.toList(databaseVersionsMaster);
		assertEquals(5, databaseVersionsMasterList.size());
		
		DatabaseVersion databaseVersionA1 = databaseVersionsMasterList.get(0);
		assertEquals("(A1)", databaseVersionA1.getVectorClock().toString());
		assertEquals(1, databaseVersionA1.getChunks().size());
		assertEquals(1, databaseVersionA1.getMultiChunks().size());
		assertEquals(1, databaseVersionA1.getFileContents().size());
		assertEquals(1, databaseVersionA1.getFileHistories().size());
		
		DatabaseVersion databaseVersionA2 = databaseVersionsMasterList.get(1);
		assertEquals("(A2)", databaseVersionA2.getVectorClock().toString());
		assertEquals(1, databaseVersionA2.getChunks().size());
		assertEquals(1, databaseVersionA2.getMultiChunks().size());
		assertEquals(1, databaseVersionA2.getFileContents().size());
		assertEquals(1, databaseVersionA2.getFileHistories().size());
		
		DatabaseVersion databaseVersionA3 = databaseVersionsMasterList.get(2);
		assertEquals("(A3)", databaseVersionA3.getVectorClock().toString());
		assertEquals(1, databaseVersionA3.getChunks().size());
		assertEquals(1, databaseVersionA3.getMultiChunks().size());
		assertEquals(1, databaseVersionA3.getFileContents().size());
		assertEquals(1, databaseVersionA3.getFileHistories().size());
		
		DatabaseVersion databaseVersionA4 = databaseVersionsMasterList.get(3);
		assertEquals("(A4)", databaseVersionA4.getVectorClock().toString());
		assertEquals(0, databaseVersionA4.getChunks().size());
		assertEquals(0, databaseVersionA4.getMultiChunks().size());
		assertEquals(0, databaseVersionA4.getFileContents().size());
		assertEquals(1, databaseVersionA4.getFileHistories().size());
		
		DatabaseVersion databaseVersionA5 = databaseVersionsMasterList.get(4);
		assertEquals("(A5)", databaseVersionA5.getVectorClock().toString());
		assertEquals(1, databaseVersionA5.getChunks().size());
		assertEquals(1, databaseVersionA5.getMultiChunks().size());
		assertEquals(1, databaseVersionA5.getFileContents().size());
		assertEquals(1, databaseVersionA5.getFileHistories().size());
		
		// - DIRTY
		assertNotNull(databaseVersionsDirty);
		
		List<DatabaseVersion> databaseVersionsDirtyList = TestCollectionUtil.toList(databaseVersionsDirty);
		assertEquals(1, databaseVersionsDirtyList.size());
		
		DatabaseVersion databaseVersionB1 = databaseVersionsDirtyList.get(0);
		assertEquals("(B1)", databaseVersionB1.getVectorClock().toString());
		assertEquals(1, databaseVersionB1.getChunks().size());
		assertEquals(1, databaseVersionB1.getMultiChunks().size());
		assertEquals(1, databaseVersionB1.getFileContents().size());
		assertEquals(2, databaseVersionB1.getFileHistories().size());
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetDatabaseVersionsTo() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileHistoryDao, multiChunkDao);
		
		Iterator<DatabaseVersion> databaseVersionsToA2 = databaseVersionDao.getDatabaseVersionsTo("A", 2);
		Iterator<DatabaseVersion> databaseVersionsToA3 = databaseVersionDao.getDatabaseVersionsTo("A", 3);
		
		
		fail("implement this");
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetLastDatabaseVersionHeader() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileHistoryDao, multiChunkDao);
		
		databaseVersionDao.getLastDatabaseVersionHeader();
		fail("implement this");
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetLocalDatabaseBranch() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileHistoryDao, multiChunkDao);
		
		databaseVersionDao.getLocalDatabaseBranch();
		fail("implement this");
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetMaxDirtyVectorClock() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileHistoryDao, multiChunkDao);
		
		//databaseVersionDao.getMaxDirtyVectorClock(machineName);
		fail("implement this");
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testMarkDatabaseVersionDirty() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileHistoryDao, multiChunkDao);
				
		//databaseVersionDao.markDatabaseVersionDirty(databaseVersionHeader, status);
		fail("implement this");
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testPersistDatabaseVersion() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileHistoryDao, multiChunkDao);
				
		//databaseVersionDao.persistDatabaseVersion(databaseVersion);
		fail("implement this");
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testRemoveDirtyDatabaseVersions() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileHistoryDao, multiChunkDao);
		
		databaseVersionDao.removeDirtyDatabaseVersions();
		fail("implement this");
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}	
}
