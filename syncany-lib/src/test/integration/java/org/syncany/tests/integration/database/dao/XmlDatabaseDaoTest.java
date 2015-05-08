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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseVersionEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.database.dao.DatabaseXmlSerializer.DatabaseReadType;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestDatabaseUtil;

public class XmlDatabaseDaoTest {
	private File tempDir;

	static {
		Logging.init();
	}

	@Before
	public void setUp() throws Exception {
		tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
	}

	@After
	public void tearDown() {
		TestFileUtil.deleteDirectory(tempDir);
	}

	@Test
	public void testWriteAndReadChunks() throws IOException {
		// Prepare
		MemoryDatabase newDatabase = new MemoryDatabase();
		DatabaseVersion newDatabaseVersion = createDatabaseVersion();

		// Create chunks
		ChunkEntry chunkA1 = new ChunkEntry(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 }), 12);
		ChunkEntry chunkA2 = new ChunkEntry(new ChunkChecksum(new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1 }), 34);
		ChunkEntry chunkA3 = new ChunkEntry(new ChunkChecksum(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }), 56);
		ChunkEntry chunkA4 = new ChunkEntry(new ChunkChecksum(new byte[] { 2, 2, 2, 2, 2, 2, 2, 2, 2 }), 78);

		newDatabaseVersion.addChunk(chunkA1);
		newDatabaseVersion.addChunk(chunkA2);
		newDatabaseVersion.addChunk(chunkA3);
		newDatabaseVersion.addChunk(chunkA4);

		// Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);

		// Write database to disk, read it again, and compare them
		MemoryDatabase loadedDatabase = writeReadAndCompareDatabase(newDatabase);

		// Check chunks
		assertEquals("Chunk not found in database loaded.", chunkA1, loadedDatabase.getChunk(chunkA1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA2, loadedDatabase.getChunk(chunkA2.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA3, loadedDatabase.getChunk(chunkA3.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA4, loadedDatabase.getChunk(chunkA4.getChecksum()));
	}

	private DatabaseVersion createDatabaseVersion() {
		return createDatabaseVersion(null);
	}

	private DatabaseVersion createDatabaseVersion(DatabaseVersion basedOnDatabaseVersion) {
		VectorClock vectorClock = (basedOnDatabaseVersion != null) ? basedOnDatabaseVersion.getVectorClock().clone() : new VectorClock();
		vectorClock.incrementClock("someclient");

		DatabaseVersion databaseVersion = new DatabaseVersion();

		databaseVersion.setClient("someclient");
		databaseVersion.setTimestamp(new Date());
		databaseVersion.setVectorClock(vectorClock);

		return databaseVersion;
	}

	@Test
	public void testWriteAndReadChunksWithMultiChunks() throws IOException {
		// Prepare
		MemoryDatabase newDatabase = new MemoryDatabase();
		DatabaseVersion newDatabaseVersion = createDatabaseVersion();

		// Create chunks
		ChunkEntry chunkA1 = new ChunkEntry(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 }), 12);
		ChunkEntry chunkA2 = new ChunkEntry(new ChunkChecksum(new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1 }), 34);
		ChunkEntry chunkA3 = new ChunkEntry(new ChunkChecksum(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }), 56);
		ChunkEntry chunkA4 = new ChunkEntry(new ChunkChecksum(new byte[] { 2, 2, 2, 2, 2, 2, 2, 2, 2 }), 78);

		ChunkEntry chunkB1 = new ChunkEntry(new ChunkChecksum(new byte[] { 3, 3, 3, 3, 3, 3, 3, 3, 3 }), 910);
		ChunkEntry chunkB2 = new ChunkEntry(new ChunkChecksum(new byte[] { 4, 4, 4, 4, 4, 4, 4, 4, 4 }), 1112);

		newDatabaseVersion.addChunk(chunkA1);
		newDatabaseVersion.addChunk(chunkA2);
		newDatabaseVersion.addChunk(chunkA3);
		newDatabaseVersion.addChunk(chunkA4);
		newDatabaseVersion.addChunk(chunkB1);
		newDatabaseVersion.addChunk(chunkB2);

		// Distribute chunks to multichunks
		MultiChunkEntry multiChunkA = new MultiChunkEntry(new MultiChunkId(new byte[] { 6, 6, 6, 6, 6, 6, 6, 6, 6 }), 10);
		multiChunkA.addChunk(chunkA1.getChecksum());
		multiChunkA.addChunk(chunkA2.getChecksum());
		multiChunkA.addChunk(chunkA3.getChecksum());
		newDatabaseVersion.addMultiChunk(multiChunkA);

		MultiChunkEntry multiChunkB = new MultiChunkEntry(new MultiChunkId(new byte[] { 7, 7, 7, 7, 7, 7, 7, 7, 7 }), 11);
		multiChunkB.addChunk(chunkA4.getChecksum());
		multiChunkB.addChunk(chunkB1.getChecksum());
		multiChunkB.addChunk(chunkB2.getChecksum());
		newDatabaseVersion.addMultiChunk(multiChunkB);

		// Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);

		// Write database to disk, read it again, and compare them
		MemoryDatabase loadedDatabase = writeReadAndCompareDatabase(newDatabase);

		// Check chunks
		assertEquals("Chunk not found in database loaded.", chunkA1, loadedDatabase.getChunk(chunkA1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA2, loadedDatabase.getChunk(chunkA2.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA3, loadedDatabase.getChunk(chunkA3.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA4, loadedDatabase.getChunk(chunkA4.getChecksum()));

		assertEquals("Chunk not found in database loaded.", chunkB1, loadedDatabase.getChunk(chunkB1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkB2, loadedDatabase.getChunk(chunkB2.getChecksum()));

		// Check multichunks
		MultiChunkEntry loadedMultiChunkA = loadedDatabase.getMultiChunk(multiChunkA.getId());
		MultiChunkEntry loadedMultiChunkB = loadedDatabase.getMultiChunk(multiChunkB.getId());

		assertEquals("Multichunk not found in database loaded.", multiChunkA, loadedMultiChunkA);
		assertEquals("Multichunk not found in database loaded.", multiChunkB, loadedMultiChunkB);

		assertArrayEquals("Chunks in multichunk expected to be different.", multiChunkA.getChunks().toArray(), loadedMultiChunkA.getChunks()
				.toArray());
		assertArrayEquals("Chunks in multichunk expected to be different.", multiChunkB.getChunks().toArray(), loadedMultiChunkB.getChunks()
				.toArray());
	}

	@Test
	public void testWriteAndReadChunksWithFileContents() throws IOException {
		// Prepare
		MemoryDatabase newDatabase = new MemoryDatabase();
		DatabaseVersion newDatabaseVersion = createDatabaseVersion();

		// Create chunks
		ChunkEntry chunkA1 = new ChunkEntry(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 }), 12);
		ChunkEntry chunkA2 = new ChunkEntry(new ChunkChecksum(new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1 }), 34);
		ChunkEntry chunkA3 = new ChunkEntry(new ChunkChecksum(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }), 56);
		ChunkEntry chunkA4 = new ChunkEntry(new ChunkChecksum(new byte[] { 2, 2, 2, 2, 2, 2, 2, 2, 2 }), 78);

		ChunkEntry chunkB1 = new ChunkEntry(new ChunkChecksum(new byte[] { 3, 3, 3, 3, 3, 3, 3, 3, 3 }), 910);
		ChunkEntry chunkB2 = new ChunkEntry(new ChunkChecksum(new byte[] { 4, 4, 4, 4, 4, 4, 4, 4, 4 }), 1112);

		newDatabaseVersion.addChunk(chunkA1);
		newDatabaseVersion.addChunk(chunkA2);
		newDatabaseVersion.addChunk(chunkA3);
		newDatabaseVersion.addChunk(chunkA4);
		newDatabaseVersion.addChunk(chunkB1);
		newDatabaseVersion.addChunk(chunkB2);

		// Distribute chunks to file contents    	
		FileContent contentA = new FileContent();
		contentA.addChunk(chunkA1.getChecksum());
		contentA.addChunk(chunkA2.getChecksum());
		contentA.addChunk(chunkA3.getChecksum());
		contentA.addChunk(chunkA4.getChecksum());
		contentA.setChecksum(new FileChecksum(new byte[] { 5, 5, 5, 4, 4, 5, 5, 5, 5 }));
		newDatabaseVersion.addFileContent(contentA);

		FileContent contentB = new FileContent();
		contentB.addChunk(chunkB1.getChecksum());
		contentB.addChunk(chunkB2.getChecksum());
		contentB.setChecksum(new FileChecksum(new byte[] { 1, 1, 1, 3, 3, 5, 5, 5, 5 }));
		newDatabaseVersion.addFileContent(contentB);

		// Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);

		// Write database to disk, read it again, and compare them
		MemoryDatabase loadedDatabase = writeReadAndCompareDatabase(newDatabase);

		// Check chunks
		assertEquals("Chunk not found in database loaded.", chunkA1, loadedDatabase.getChunk(chunkA1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA2, loadedDatabase.getChunk(chunkA2.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA3, loadedDatabase.getChunk(chunkA3.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA4, loadedDatabase.getChunk(chunkA4.getChecksum()));

		assertEquals("Chunk not found in database loaded.", chunkB1, loadedDatabase.getChunk(chunkB1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkB2, loadedDatabase.getChunk(chunkB2.getChecksum()));

		// Check file contents
		FileContent loadedContentA = loadedDatabase.getContent(contentA.getChecksum());
		FileContent loadedContentB = loadedDatabase.getContent(contentB.getChecksum());

		assertEquals("File content not found in database loaded.", contentA, loadedContentA);
		assertEquals("File content not found in database loaded.", contentB, loadedContentB);

		assertArrayEquals("Chunks in file content expected to be different.", contentA.getChunks().toArray(), loadedContentA.getChunks().toArray());
		assertArrayEquals("Chunks in file content expected to be different.", contentB.getChunks().toArray(), loadedContentB.getChunks().toArray());
	}

	@Test
	public void testWriteAndReadFileHistoryAndFileVersion() throws IOException {
		// Prepare
		MemoryDatabase newDatabase = new MemoryDatabase();
		DatabaseVersion newDatabaseVersion = createDatabaseVersion();

		// Create directories (no content!)

		// File A
		PartialFileHistory fileHistoryA = new PartialFileHistory(FileHistoryId.secureRandomFileId());
		newDatabaseVersion.addFileHistory(fileHistoryA);

		FileVersion versionA1 = new FileVersion();
		versionA1.setVersion(1L);
		versionA1.setType(FileType.FOLDER);
		versionA1.setPath("Pictures/2013/New York Folder");
		versionA1.setStatus(FileStatus.NEW);
		versionA1.setSize(0L);
		versionA1.setLastModified(new Date());
		fileHistoryA.addFileVersion(versionA1);

		FileVersion versionA2 = new FileVersion();
		versionA2.setVersion(2L);
		versionA2.setType(FileType.FOLDER);
		versionA2.setPath("Pictures/2013/New York");
		versionA2.setStatus(FileStatus.RENAMED);
		versionA2.setSize(0L);
		versionA2.setLastModified(new Date());
		fileHistoryA.addFileVersion(versionA2);

		// File B
		PartialFileHistory fileHistoryB = new PartialFileHistory(FileHistoryId.secureRandomFileId());
		newDatabaseVersion.addFileHistory(fileHistoryB);

		FileVersion versionB1 = new FileVersion();
		versionB1.setVersion(1L);
		versionB1.setType(FileType.FOLDER);
		versionB1.setPath("Pictures/2013/Egypt Folder");
		versionB1.setStatus(FileStatus.NEW);
		versionB1.setSize(0L);
		versionB1.setLastModified(new Date());
		fileHistoryB.addFileVersion(versionB1);

		FileVersion versionB2 = new FileVersion();
		versionB2.setVersion(2L);
		versionB2.setType(FileType.FOLDER);
		versionB2.setPath("Pictures/2013/Egypt");
		versionB2.setStatus(FileStatus.RENAMED);
		versionB2.setSize(0L);
		versionB2.setLastModified(new Date());
		fileHistoryB.addFileVersion(versionB2);

		// Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);

		// Write database to disk, read it again, and compare them
		MemoryDatabase loadedDatabase = writeReadAndCompareDatabase(newDatabase);

		// File histories
		PartialFileHistory loadedFileHistoryA = loadedDatabase.getFileHistory(fileHistoryA.getFileHistoryId());
		PartialFileHistory loadedFileHistoryB = loadedDatabase.getFileHistory(fileHistoryB.getFileHistoryId());

		assertEquals("File history not found in database loaded.", fileHistoryA, loadedFileHistoryA);
		assertEquals("File history not found in database loaded.", fileHistoryB, loadedFileHistoryB);

		assertArrayEquals("File versions differ in loaded database.", fileHistoryA.getFileVersions().values().toArray(),
				loadedFileHistoryA.getFileVersions().values().toArray());

		assertArrayEquals("File versions differ in loaded database.", fileHistoryB.getFileVersions().values().toArray(),
				loadedFileHistoryB.getFileVersions().values().toArray());
	}

	@Test
	public void testWriteAndReadVectorClock() throws IOException {
		// Prepare
		MemoryDatabase newDatabase = new MemoryDatabase();
		DatabaseVersion newDatabaseVersion = createDatabaseVersion();

		// Create new vector clock
		VectorClock vc = new VectorClock();

		vc.setClock("UserA", 14234234L);
		vc.setClock("UserB", 9433431232432L);
		vc.setClock("UserC", 1926402374L);

		newDatabaseVersion.setVectorClock(vc);

		// Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);

		// Write database to disk, read it again, and compare them
		MemoryDatabase loadedDatabase = writeReadAndCompareDatabase(newDatabase);

		// Check VC
		DatabaseVersion loadedDatabaseVersionSelectedByVectorClock = loadedDatabase.getDatabaseVersion(vc);
		DatabaseVersion loadedDatabaseVersionSelectedFirst = loadedDatabase.getDatabaseVersions().get(0);

		assertEquals("Vector clocks do not match (selected by vector clock)", vc, loadedDatabaseVersionSelectedByVectorClock.getVectorClock());
		assertEquals("Vector clocks do not match (selected first)", vc, loadedDatabaseVersionSelectedFirst.getVectorClock());
		assertEquals("Database versions do not match.", loadedDatabaseVersionSelectedByVectorClock, loadedDatabaseVersionSelectedFirst);
	}

	@Test
	public void testWriteAndReadMultipleDatabaseVersions() throws IOException {
		MemoryDatabase writtenDatabase = new MemoryDatabase();
		List<DatabaseVersion> writtenDatabaseVersions = new ArrayList<DatabaseVersion>();

		for (int i = 0; i < 10; i++) {
			DatabaseVersion basedOnDatabaseVersion = (i > 0) ? writtenDatabaseVersions.get(i - 1) : null;
			DatabaseVersion newDatabaseVersion = createDatabaseVersion(basedOnDatabaseVersion);

			// Some random chunks
			newDatabaseVersion.addChunk(new ChunkEntry(new ChunkChecksum(TestFileUtil.createRandomArray(20)), 32 * 1024));
			newDatabaseVersion.addChunk(new ChunkEntry(new ChunkChecksum(TestFileUtil.createRandomArray(20)), 32 * 1024));
			newDatabaseVersion.addChunk(new ChunkEntry(new ChunkChecksum(TestFileUtil.createRandomArray(20)), 32 * 1024));

			// Add to database
			writtenDatabase.addDatabaseVersion(newDatabaseVersion);

			// Add to test array
			writtenDatabaseVersions.add(newDatabaseVersion);
		}

		// Write database to disk, read it again, and compare them
		writeReadAndCompareDatabase(writtenDatabase);
	}

	@Test
	public void testWritePartialDatabaseOneToFive() throws IOException {
		MemoryDatabase writtenDatabase = new MemoryDatabase();
		List<DatabaseVersion> writtenDatabaseVersions = new ArrayList<DatabaseVersion>();

		for (int i = 0; i < 10; i++) {
			DatabaseVersion basedOnDatabaseVersion = (i > 0) ? writtenDatabaseVersions.get(i - 1) : null;
			DatabaseVersion newDatabaseVersion = createDatabaseVersion(basedOnDatabaseVersion);

			// Some random chunks
			newDatabaseVersion.addChunk(new ChunkEntry(new ChunkChecksum(TestFileUtil.createRandomArray(20)), 32 * 1024));
			newDatabaseVersion.addChunk(new ChunkEntry(new ChunkChecksum(TestFileUtil.createRandomArray(20)), 32 * 1024));
			newDatabaseVersion.addChunk(new ChunkEntry(new ChunkChecksum(TestFileUtil.createRandomArray(20)), 32 * 1024));

			// Add to database
			writtenDatabase.addDatabaseVersion(newDatabaseVersion);

			// Add to test array
			writtenDatabaseVersions.add(newDatabaseVersion);
		}

		// Write database to disk, read it again, and compare them
		File writtenDatabaseFile = new File(tempDir + "/db-" + Math.random() + "-" + Math.abs(new Random().nextInt(Integer.MAX_VALUE)));

		DatabaseXmlSerializer writeDAO = new DatabaseXmlSerializer();
		writeDAO.save(writtenDatabase.getDatabaseVersions(), writtenDatabaseFile);

		// Read again
		MemoryDatabase readDatabase = new MemoryDatabase();

		DatabaseXmlSerializer readDAO = new DatabaseXmlSerializer();
		readDAO.load(readDatabase, writtenDatabaseFile, null, null, DatabaseReadType.FULL);

		for (int i = 0; i < 10; i++) {
			DatabaseVersion writtenDatabaseVersion = writtenDatabaseVersions.get(i);
			DatabaseVersion readDatabaseVersion = readDatabase.getDatabaseVersion(writtenDatabaseVersion.getVectorClock());

			assertNotNull(readDatabaseVersion);
			assertDatabaseVersionEquals(writtenDatabaseVersion, readDatabaseVersion);
		}

		assertEquals(10, readDatabase.getDatabaseVersions().size());
	}

	private MemoryDatabase writeReadAndCompareDatabase(MemoryDatabase writtenDatabase) throws IOException {
		File writtenDatabaseFile = new File(tempDir + "/db-" + Math.random() + "-" + Math.abs(new Random().nextInt(Integer.MAX_VALUE)));
		TestDatabaseUtil.writeDatabaseFileToDisk(writtenDatabase, writtenDatabaseFile, null);
		MemoryDatabase readDatabase = TestDatabaseUtil.readDatabaseFileFromDisk(writtenDatabaseFile, null);

		TestAssertUtil.assertDatabaseEquals(writtenDatabase, readDatabase);

		return readDatabase;
	}
}
