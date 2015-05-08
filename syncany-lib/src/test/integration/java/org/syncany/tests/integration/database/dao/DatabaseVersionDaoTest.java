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
package org.syncany.tests.integration.database.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.dao.ChunkSqlDao;
import org.syncany.database.dao.DatabaseVersionSqlDao;
import org.syncany.database.dao.FileContentSqlDao;
import org.syncany.database.dao.FileHistorySqlDao;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.database.dao.MultiChunkSqlDao;
import org.syncany.operations.down.DatabaseBranch;
import org.syncany.tests.util.TestCollectionUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDatabaseUtil;
import org.syncany.tests.util.TestSqlUtil;

public class DatabaseVersionDaoTest {
	@Test
	public void testGetDatabaseVersionsMasterAndDirty() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
		
		Iterator<DatabaseVersion> databaseVersionsDirty = databaseVersionDao.getDirtyDatabaseVersions();
		
		// Test				
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
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
		
		Iterator<DatabaseVersion> databaseVersionsToA2 = databaseVersionDao.getDatabaseVersionsTo("A", 2);
		Iterator<DatabaseVersion> databaseVersionsToA5 = databaseVersionDao.getDatabaseVersionsTo("A", 5);
		Iterator<DatabaseVersion> databaseVersionsToB1 = databaseVersionDao.getDatabaseVersionsTo("B", 1); // B1 is DIRTY !
		
		List<DatabaseVersion> databaseVersionsToA2List = TestCollectionUtil.toList(databaseVersionsToA2);
		List<DatabaseVersion> databaseVersionsToA5List = TestCollectionUtil.toList(databaseVersionsToA5);		
		List<DatabaseVersion> databaseVersionsToB1List = TestCollectionUtil.toList(databaseVersionsToB1);
		
		// Test
		assertNotNull(databaseVersionsToA2);
		assertEquals(2, databaseVersionsToA2List.size());
		assertEquals("(A1)", databaseVersionsToA2List.get(0).getHeader().getVectorClock().toString());
		assertEquals("(A2)", databaseVersionsToA2List.get(1).getHeader().getVectorClock().toString());
		
		assertNotNull(databaseVersionsToA5);
		assertEquals(5, databaseVersionsToA5List.size());
		assertEquals("(A1)", databaseVersionsToA5List.get(0).getHeader().getVectorClock().toString());
		assertEquals("(A2)", databaseVersionsToA5List.get(1).getHeader().getVectorClock().toString());
		assertEquals("(A3)", databaseVersionsToA5List.get(2).getHeader().getVectorClock().toString());
		assertEquals("(A4)", databaseVersionsToA5List.get(3).getHeader().getVectorClock().toString());
		assertEquals("(A5)", databaseVersionsToA5List.get(4).getHeader().getVectorClock().toString());
		
		assertNotNull(databaseVersionsToB1);
		assertEquals(0, databaseVersionsToB1List.size());
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetLastDatabaseVersionHeader1() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
		
		DatabaseVersionHeader lastDatabaseVersionHeader = databaseVersionDao.getLastDatabaseVersionHeader();
		
		// Test
		assertNotNull(lastDatabaseVersionHeader);
		assertEquals("(A8,B3)", lastDatabaseVersionHeader.getVectorClock().toString());
		assertEquals("A/(A8,B3)/T=1389977288000", lastDatabaseVersionHeader.toString());		
		
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
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
		
		// a. Capture database version header (now)
		DatabaseVersionHeader lastDatabaseVersionHeaderBefore = databaseVersionDao.getLastDatabaseVersionHeader();
		
		// b. Add new database header (with one file history)
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
		DatabaseVersionHeader newDatabaseVersionHeader = new DatabaseVersionHeader();
		
		newDatabaseVersionHeader.setClient("C");
		newDatabaseVersionHeader.setDate(new Date(1489977288000L));
		newDatabaseVersionHeader.setVectorClock(TestDatabaseUtil.createVectorClock("A5,C1"));
		
		newDatabaseVersion.setHeader(newDatabaseVersionHeader);
		
		PartialFileHistory newFileHistory = new PartialFileHistory(FileHistoryId.secureRandomFileId());
		FileVersion newFileVersion = new FileVersion();
		
		newFileVersion.setVersion(1L);
		newFileVersion.setPath("newfile");
		newFileVersion.setChecksum(FileChecksum.parseFileChecksum("aaaaaaaaaaaaaaaaaaaaab2b263ffa4cc48e282f"));
		newFileVersion.setLinkTarget(null);
		newFileVersion.setPosixPermissions("rwxrwxrwx");
		newFileVersion.setDosAttributes(null);
		newFileVersion.setStatus(FileStatus.NEW);
		newFileVersion.setLastModified(new Date());
		newFileVersion.setUpdated(new Date());
		newFileVersion.setSize(1L);
		newFileVersion.setType(FileType.FILE);

		newFileHistory.addFileVersion(newFileVersion);
		newDatabaseVersion.addFileHistory(newFileHistory);

		ChunkEntry newChunkEntry = new ChunkEntry(ChunkChecksum.parseChunkChecksum("aaaaaaaaaaaaaaaaaaaaab2b263ffa4cc48e282f"), 1);
		newDatabaseVersion.addChunk(newChunkEntry);
		
		MultiChunkEntry newMultiChunkEntry = new MultiChunkEntry(MultiChunkId.parseMultiChunkId("1234567890987654321234567876543456555555"), 10);
		newMultiChunkEntry.addChunk(newChunkEntry.getChecksum());
		newDatabaseVersion.addMultiChunk(newMultiChunkEntry);
		
		FileContent newFileContent = new FileContent();
		newFileContent.setChecksum(FileChecksum.parseFileChecksum("aaaaaaaaaaaaaaaaaaaaab2b263ffa4cc48e282f"));
		newFileContent.setSize(1L);		
		newFileContent.addChunk(newChunkEntry.getChecksum());
		newDatabaseVersion.addFileContent(newFileContent);		
		
		// c. Persist database version
		databaseVersionDao.writeDatabaseVersion(newDatabaseVersion);
		
		// d. Capture new last database version header
		DatabaseVersionHeader lastDatabaseVersionHeaderAfter = databaseVersionDao.getLastDatabaseVersionHeader();	
		
		// Test
		assertNotNull(lastDatabaseVersionHeaderBefore);
		assertEquals("A/(A5)/T=1388935689000", lastDatabaseVersionHeaderBefore.toString());
		
		assertNotNull(lastDatabaseVersionHeaderAfter);
		assertEquals("C/(A5,C1)/T=1489977288000", lastDatabaseVersionHeaderAfter.toString());
		assertEquals(newDatabaseVersionHeader.getVectorClock(), lastDatabaseVersionHeaderAfter.getVectorClock());
		
		assertEquals(newChunkEntry, chunkDao.getChunk(ChunkChecksum.parseChunkChecksum("aaaaaaaaaaaaaaaaaaaaab2b263ffa4cc48e282f")));
		assertEquals(newFileContent, fileContentDao.getFileContent(FileChecksum.parseFileChecksum("aaaaaaaaaaaaaaaaaaaaab2b263ffa4cc48e282f"), true));
		
		Map<MultiChunkId, MultiChunkEntry> multiChunkIds = multiChunkDao.getMultiChunks(newDatabaseVersionHeader.getVectorClock());
		assertNotNull(multiChunkIds);
		assertEquals(1, multiChunkIds.size());
		
		MultiChunkEntry actualNewMultiChunkEntry = multiChunkIds.get(MultiChunkId.parseMultiChunkId("1234567890987654321234567876543456555555"));
		assertNotNull(actualNewMultiChunkEntry);
		assertEquals(newMultiChunkEntry.getId(), actualNewMultiChunkEntry.getId());
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetLocalDatabaseBranch1() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
		
		DatabaseBranch localDatabaseBranch = databaseVersionDao.getLocalDatabaseBranch();
		
		// Test
		assertNotNull(localDatabaseBranch);
		assertEquals(11, localDatabaseBranch.size());
		assertEquals(11, localDatabaseBranch.getAll().size());
		 
		assertEquals(TestDatabaseUtil.createBranch(
			new String[] {
				"A/(A1)/T=1389977166000",
				"A/(A2)/T=1389977199000",
				"A/(A3)/T=1389977203000",
				"A/(A4)/T=1389977207000",
				"A/(A5)/T=1389977214000",
				"A/(A6)/T=1389977222000",
				"B/(A6,B1)/T=1389977233000",
				"A/(A7,B1)/T=1389977234000",
				"B/(A7,B2)/T=1389977258000",
				"B/(A7,B3)/T=1389977264000",
				"A/(A8,B3)/T=1389977288000",
			}
		), localDatabaseBranch);		
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetLocalDatabaseBranch2() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
		
		DatabaseBranch localDatabaseBranch = databaseVersionDao.getLocalDatabaseBranch();
		
		// Test
		assertNotNull(localDatabaseBranch);
		assertEquals(5, localDatabaseBranch.size());
		assertEquals(5, localDatabaseBranch.getAll().size());
		 
		assertEquals(TestDatabaseUtil.createBranch(
			new String[] {
				"A/(A1)/T=1388589969000",
				"A/(A2)/T=1388676369000",
				"A/(A3)/T=1388762769000", // Note: Does NOT contain B1 (because: DIRTY!)
				"A/(A4)/T=1388849289000",
				"A/(A5)/T=1388935689000"
			}
		), localDatabaseBranch);		
				
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
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
		
		Long maxDirtyVectorClockA = databaseVersionDao.getMaxDirtyVectorClock("A");
		Long maxDirtyVectorClockB = databaseVersionDao.getMaxDirtyVectorClock("B");
		
		// Test
		assertNull(maxDirtyVectorClockA);
		assertNotNull(maxDirtyVectorClockB);
		assertEquals(1, (long) maxDirtyVectorClockB);
				
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
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set2.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);
				
		databaseVersionDao.markDatabaseVersionDirty(TestDatabaseUtil.createVectorClock("A48"));
		databaseVersionDao.markDatabaseVersionDirty(TestDatabaseUtil.createVectorClock("A49"));
		databaseVersionDao.markDatabaseVersionDirty(TestDatabaseUtil.createVectorClock("A50"));
		
		List<DatabaseVersion> dirtyDatabaseVersions = TestCollectionUtil.toList(databaseVersionDao.getDirtyDatabaseVersions());
		
		// Test
		assertNotNull(dirtyDatabaseVersions);
		assertEquals(3, dirtyDatabaseVersions.size());
		assertEquals("(A48)", dirtyDatabaseVersions.get(0).getVectorClock().toString());
		assertEquals("(A49)", dirtyDatabaseVersions.get(1).getVectorClock().toString());
		assertEquals("(A50)", dirtyDatabaseVersions.get(2).getVectorClock().toString());		
				
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
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);
		FileHistorySqlDao fileHistoryDao = new FileHistorySqlDao(databaseConnection, fileVersionDao);
		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);
		DatabaseVersionSqlDao databaseVersionDao = new DatabaseVersionSqlDao(databaseConnection, chunkDao, fileContentDao, fileVersionDao, fileHistoryDao, multiChunkDao);

		// a. Test before
		List<DatabaseVersion> dirtyDatabaseVersionsBefore = TestCollectionUtil.toList(databaseVersionDao.getDirtyDatabaseVersions());		
		assertNotNull(dirtyDatabaseVersionsBefore);
		assertNotNull(chunkDao.getChunk(ChunkChecksum.parseChunkChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")));
		assertNotNull(multiChunkDao.getDirtyMultiChunkIds());
		assertEquals(1, multiChunkDao.getDirtyMultiChunkIds().size());
		
		// b. Add new database version with DIRTY multichunk; remove DIRTY version		
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
		newDatabaseVersion.setVectorClock(TestDatabaseUtil.createVectorClock("A5,B2"));
		
		long newDatabaseVersionId = databaseVersionDao.writeDatabaseVersion(newDatabaseVersion);		
		databaseVersionDao.removeDirtyDatabaseVersions(newDatabaseVersionId); 
				
		// c. Test after		
		
		// Database version
		List<DatabaseVersion> dirtyDatabaseVersionsAfter = TestCollectionUtil.toList(databaseVersionDao.getDirtyDatabaseVersions());		
		assertNotNull(dirtyDatabaseVersionsAfter);
		assertEquals(0, dirtyDatabaseVersionsAfter.size());
		
		// Multichunk from dirty version "moved" to new version
		Map<MultiChunkId, MultiChunkEntry> multiChunksA5B2 = multiChunkDao.getMultiChunks(TestDatabaseUtil.createVectorClock("A5,B2"));		
		assertNotNull(multiChunksA5B2);
		assertEquals(1, multiChunksA5B2.size());
		assertNotNull(multiChunksA5B2.get(MultiChunkId.parseMultiChunkId("1234567890987654321123456789098765433222")));
		
		// File version/history/content ARE removed
		assertNull(fileContentDao.getFileContent(FileChecksum.parseFileChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"), true));
		
		// TODO [low] Test file version and file history removal		
		
		// Chunks and multichunks are NOT removed!
		assertNotNull(chunkDao.getChunk(ChunkChecksum.parseChunkChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")));
		
		assertNotNull(multiChunkDao.getMultiChunks(TestDatabaseUtil.createVectorClock("B1")));
		assertEquals(0, multiChunkDao.getMultiChunks(TestDatabaseUtil.createVectorClock("B1")).size());
		
		assertNotNull(multiChunkDao.getDirtyMultiChunkIds());
		assertEquals(0, multiChunkDao.getDirtyMultiChunkIds().size());		
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}	
}
