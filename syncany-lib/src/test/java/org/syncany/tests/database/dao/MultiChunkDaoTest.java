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
package org.syncany.tests.database.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.dao.MultiChunkSqlDao;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDatabaseUtil;
import org.syncany.tests.util.TestSqlUtil;
import org.syncany.util.CollectionUtil;

public class MultiChunkDaoTest {
	@Test
	public void testGetMultiChunkId() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set3.sql");

		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		
		MultiChunkId multiChunkId1 = multiChunkDao.getMultiChunkId(ChunkChecksum.parseChunkChecksum("eba69a8e359ce3258520138a50ed9860127ab6e0"));
		MultiChunkId multiChunkId2 = multiChunkDao.getMultiChunkId(ChunkChecksum.parseChunkChecksum("0fecbac8ac8a5f8b7aa12b2741a4ef5db88c5dea"));
		MultiChunkId multiChunkId3 = multiChunkDao.getMultiChunkId(ChunkChecksum.parseChunkChecksum("38a18897e94a901b833e750e8604d9616a02ca84"));
		MultiChunkId multiChunkIdNonExistent = multiChunkDao.getMultiChunkId(ChunkChecksum.parseChunkChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"));

		// Test
		assertNotNull(multiChunkId1);
		assertEquals("0d79eed3fd8ac866b5872ea3f3f079c46dd15ac9", multiChunkId1.toString());
		
		assertNotNull(multiChunkId2);
		assertEquals("51aaca5c1280b1cf95cff8a3266a6bb44b482ad4", multiChunkId2.toString());
		
		assertNotNull(multiChunkId3);
		assertEquals("51aaca5c1280b1cf95cff8a3266a6bb44b482ad4", multiChunkId3.toString());
		assertEquals(multiChunkId2, multiChunkId3);
		
		assertNull(multiChunkIdNonExistent);
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetMultiChunkIds() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set3.sql");

		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		List<ChunkChecksum> checksums = new ArrayList<ChunkChecksum>();
		checksums.add(ChunkChecksum.parseChunkChecksum("eba69a8e359ce3258520138a50ed9860127ab6e0"));
		checksums.add(ChunkChecksum.parseChunkChecksum("0fecbac8ac8a5f8b7aa12b2741a4ef5db88c5dea"));
		checksums.add(ChunkChecksum.parseChunkChecksum("38a18897e94a901b833e750e8604d9616a02ca84"));
		checksums.add(ChunkChecksum.parseChunkChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"));
		
		//Ensure that uniqueness is not required
		checksums.add(ChunkChecksum.parseChunkChecksum("38a18897e94a901b833e750e8604d9616a02ca84"));
		
		Map<ChunkChecksum,MultiChunkId> multiChunkIds = multiChunkDao.getMultiChunkIdsByChecksums(checksums);		

		// Test
		assertNotNull(multiChunkIds.get(checksums.get(0)));
		assertEquals("0d79eed3fd8ac866b5872ea3f3f079c46dd15ac9", multiChunkIds.get(checksums.get(0)).toString());
		
		assertNotNull(multiChunkIds.get(checksums.get(1)));
		assertEquals("51aaca5c1280b1cf95cff8a3266a6bb44b482ad4", multiChunkIds.get(checksums.get(1)).toString());
		
		assertNotNull(multiChunkIds.get(checksums.get(2)));
		assertEquals("51aaca5c1280b1cf95cff8a3266a6bb44b482ad4", multiChunkIds.get(checksums.get(2)).toString());
		assertEquals(multiChunkIds.get(checksums.get(1)), multiChunkIds.get(checksums.get(2)));
		
		assertNull(multiChunkIds.get(checksums.get(3)));
		
		Map<ChunkChecksum,MultiChunkId> multiChunkIdsEmpty = multiChunkDao.getMultiChunkIdsByChecksums(new ArrayList<ChunkChecksum>());
		assertTrue(multiChunkIdsEmpty.isEmpty());

		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetMultiChunksByDatabaseVersion1() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set3.sql");

		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		
		Map<MultiChunkId, MultiChunkEntry> multiChunksA6 = multiChunkDao.getMultiChunks(TestDatabaseUtil.createVectorClock("A6"));
		Map<MultiChunkId, MultiChunkEntry> multiChunksA7B2 = multiChunkDao.getMultiChunks(TestDatabaseUtil.createVectorClock("A7,B2"));		

		// Test
		
		// - Database version "A6"
		assertNotNull(multiChunksA6);
		assertEquals(1, multiChunksA6.size());
		
		MultiChunkEntry multiChunkInA6 = multiChunksA6.get(MultiChunkId.parseMultiChunkId("9302d8b104023627f655fa7745927fdeb3df674b"));
		
		assertNotNull(multiChunkInA6);
		assertEquals("9302d8b104023627f655fa7745927fdeb3df674b", multiChunkInA6.getId().toString());
		assertTrue(CollectionUtil.containsExactly(multiChunkInA6.getChunks(), 
			ChunkChecksum.parseChunkChecksum("24a39e00d6156804e27f7c0987d00903da8e6682")			
		));
				
		// - Database version "A8,B3"		
		assertNotNull(multiChunksA7B2);
		assertEquals(1, multiChunksA7B2.size());
		
		MultiChunkEntry multiChunkInA7B2 = multiChunksA7B2.get(MultiChunkId.parseMultiChunkId("51aaca5c1280b1cf95cff8a3266a6bb44b482ad4"));
		
		assertEquals("51aaca5c1280b1cf95cff8a3266a6bb44b482ad4", multiChunkInA7B2.getId().toString());
		assertTrue(CollectionUtil.containsExactly(multiChunkInA7B2.getChunks(), 
			ChunkChecksum.parseChunkChecksum("0fecbac8ac8a5f8b7aa12b2741a4ef5db88c5dea"),
			ChunkChecksum.parseChunkChecksum("38a18897e94a901b833e750e8604d9616a02ca84"),
			ChunkChecksum.parseChunkChecksum("47dded182d31799267f12eb9864cdc11127b3352"),
			ChunkChecksum.parseChunkChecksum("5abe80d7dd96369a3e53993cd69279400ec740bd"),
			ChunkChecksum.parseChunkChecksum("5f0b34374821423f69bf2231210245ccf0302df0"),
			ChunkChecksum.parseChunkChecksum("615fba8c2281d5bee891eb092a252d235c237457"),
			ChunkChecksum.parseChunkChecksum("8ed8d50a6e9da3197bd665bc3a1f229ebcde9b42"),
			ChunkChecksum.parseChunkChecksum("9974b55a79994b4bfe007983539ca21b2679ba35"),
			ChunkChecksum.parseChunkChecksum("a301a81d5a4f427d04791b89bfd7798eda6bd013"),
			ChunkChecksum.parseChunkChecksum("a7405a0bada0035ed52a1a44a4d381b78dc59d19"),
			ChunkChecksum.parseChunkChecksum("ab85720d3f31bd08ca1cd25dcd8a490e5f00783b"),
			ChunkChecksum.parseChunkChecksum("b0223d9770a5c0d7e22ac3d2706c4c9858cf42a9"),
			ChunkChecksum.parseChunkChecksum("b310c0eedcd03238888c6abb3e3398633139ecc5"),
			ChunkChecksum.parseChunkChecksum("f15eace568ea3c324ecd3d01b67e692bbf8a2f1b")			
		));
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetMultiChunksByDatabaseVersion2() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");

		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		
		Map<MultiChunkId, MultiChunkEntry> multiChunksA4 = multiChunkDao.getMultiChunks(TestDatabaseUtil.createVectorClock("A4"));		
		Map<MultiChunkId, MultiChunkEntry> multiChunksA5 = multiChunkDao.getMultiChunks(TestDatabaseUtil.createVectorClock("A5"));

		// Test
		
		// - Database version "A4"		
		assertNotNull(multiChunksA4);
		assertEquals(0, multiChunksA4.size());
		
		// - Database version "A5"
		assertNotNull(multiChunksA5);
		assertEquals(1, multiChunksA5.size());
		
		MultiChunkEntry multiChunkInA5 = multiChunksA5.get(MultiChunkId.parseMultiChunkId("dddddddddddddddddddddddddddddddddddddddd"));
		
		assertNotNull(multiChunkInA5);
		assertEquals("dddddddddddddddddddddddddddddddddddddddd", multiChunkInA5.getId().toString());
		assertTrue(CollectionUtil.containsExactly(multiChunkInA5.getChunks(), 
			ChunkChecksum.parseChunkChecksum("ffffffffffffffffffffffffffffffffffffffff")			
		));
						
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetMultiChunkIdsByFileChecksum() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set3.sql");

		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		
		List<MultiChunkId> multiChunkIds1 = multiChunkDao.getMultiChunkIds(FileChecksum.parseFileChecksum("254416e71ae50431fc6ced6751075b3366db7cc8"));
		List<MultiChunkId> multiChunkIds2 = multiChunkDao.getMultiChunkIds(FileChecksum.parseFileChecksum("7666fd3b860c9d7588d9ca1807eebdf8cfaa8be3"));
		List<MultiChunkId> multiChunkIdsDoesNotExist = multiChunkDao.getMultiChunkIds(FileChecksum.parseFileChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"));
		
		// Test
		
		// - Multi chunk for file 254416e71ae50431fc6ced6751075b3366db7cc8
		assertNotNull(multiChunkIds1);
		assertEquals(1, multiChunkIds1.size());
		
		MultiChunkId multiChunkId1 = multiChunkIds1.get(0);
		assertEquals("51aaca5c1280b1cf95cff8a3266a6bb44b482ad4", multiChunkId1.toString());

		// - Multi chunk for file a7405a0bada0035ed52a1a44a4d381b78dc59d19
		assertNotNull(multiChunkIds2);
		assertEquals(1, multiChunkIds2.size());
		
		MultiChunkId multiChunkId2 = multiChunkIds2.get(0);
		assertEquals("53dbeafe18eb2cd6dc519f8b861cf974fda8f26a", multiChunkId2.toString());

		// - Multi chunk for non existent file
		assertNotNull(multiChunkIdsDoesNotExist);
		assertEquals(0, multiChunkIdsDoesNotExist.size());
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetDirtyMultiChunks() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");

		MultiChunkSqlDao multiChunkDao = new MultiChunkSqlDao(databaseConnection);
		List<MultiChunkId> dirtyMultiChunkIds = multiChunkDao.getDirtyMultiChunkIds();
				
		// Test
		assertNotNull(dirtyMultiChunkIds);
		assertEquals(1, dirtyMultiChunkIds.size());
		assertEquals(MultiChunkId.parseMultiChunkId("1234567890987654321123456789098765433222"), dirtyMultiChunkIds.get(0));
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
}
