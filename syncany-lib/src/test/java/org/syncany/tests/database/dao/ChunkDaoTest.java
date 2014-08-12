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
import java.util.Map;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.dao.ChunkSqlDao;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDatabaseUtil;
import org.syncany.tests.util.TestSqlUtil;

public class ChunkDaoTest {
	@Test
	public void testGetChunk() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set3.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		
		ChunkEntry chunk1 = chunkDao.getChunk(ChunkChecksum.parseChunkChecksum("615fba8c2281d5bee891eb092a252d235c237457"));
		ChunkEntry chunk2 = chunkDao.getChunk(ChunkChecksum.parseChunkChecksum("ab85720d3f31bd08ca1cd25dcd8a490e5f00783b"));
		ChunkEntry chunkNonExistent = chunkDao.getChunk(ChunkChecksum.parseChunkChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"));
		
		// Test
		assertNotNull(chunk1);
		assertEquals("615fba8c2281d5bee891eb092a252d235c237457", chunk1.getChecksum().toString());
		assertEquals(8387, chunk1.getSize());
		
		assertNotNull(chunk2);
		assertEquals("ab85720d3f31bd08ca1cd25dcd8a490e5f00783b", chunk2.getChecksum().toString());
		assertEquals(5176, chunk2.getSize());
		
		assertNull(chunkNonExistent);
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testGetChunksForDatabaseVersion() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");
		
		ChunkSqlDao chunkDao = new ChunkSqlDao(databaseConnection);
		
		Map<ChunkChecksum, ChunkEntry> chunksForA1 = chunkDao.getChunks(TestDatabaseUtil.createVectorClock("A1"));
		Map<ChunkChecksum, ChunkEntry> chunksForA2 = chunkDao.getChunks(TestDatabaseUtil.createVectorClock("A2"));
		Map<ChunkChecksum, ChunkEntry> chunksForA4 = chunkDao.getChunks(TestDatabaseUtil.createVectorClock("A4"));
		Map<ChunkChecksum, ChunkEntry> chunksForB1 = chunkDao.getChunks(TestDatabaseUtil.createVectorClock("B1"));
		Map<ChunkChecksum, ChunkEntry> chunksForNonExistent = chunkDao.getChunks(TestDatabaseUtil.createVectorClock("NonExistent1"));

		// Test
		assertNotNull(chunksForA1);
		assertEquals(1, chunksForA1.size());
		assertNotNull(chunksForA1.get(ChunkChecksum.parseChunkChecksum("fe83f217d464f6fdfa5b2b1f87fe3a1a47371196")));
		
		assertNotNull(chunksForA2);
		assertEquals(1, chunksForA2.size());
		assertNotNull(chunksForA2.get(ChunkChecksum.parseChunkChecksum("bf8b4530d8d246dd74ac53a13471bba17941dff7")));
		
		assertNotNull(chunksForA4);
		assertEquals(0, chunksForA4.size()); // Especially: does NOT contain fe83f217d464f6fdfa5b2b1f87fe3a1a47371196		
		
		assertNotNull(chunksForB1);
		assertEquals(1, chunksForB1.size());
		assertNotNull(chunksForB1.get(ChunkChecksum.parseChunkChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")));
		
		assertNotNull(chunksForNonExistent);
		assertEquals(0, chunksForNonExistent.size());
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}	
}
