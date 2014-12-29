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
package org.syncany.tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.tests.util.TestDatabaseUtil;

public class MemoryDatabaseCacheTest {
	static {
		Logging.init();
	}

	@Test
	public void testChunkCache() throws IOException {
		MemoryDatabase database = new MemoryDatabase();

		// Round 1: Add chunk to new database version, then add database version
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();

		ChunkEntry chunkA1 = new ChunkEntry(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 }), 12);
		databaseVersion1.addChunk(chunkA1);

		database.addDatabaseVersion(databaseVersion1);
		assertEquals(chunkA1, database.getChunk(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 })));

		// Round 2: Add chunk to new database version, then add database version
		DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);

		ChunkEntry chunkA2 = new ChunkEntry(new ChunkChecksum(new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1 }), 112);
		databaseVersion2.addChunk(chunkA2);

		database.addDatabaseVersion(databaseVersion2);
		assertEquals(chunkA1, database.getChunk(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 })));
		assertEquals(chunkA2, database.getChunk(new ChunkChecksum(new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1 })));

		// Round 3: Add chunk to new database version, then add database version
		DatabaseVersion databaseVersion3 = TestDatabaseUtil.createDatabaseVersion(databaseVersion2);

		ChunkEntry chunkA3 = new ChunkEntry(new ChunkChecksum(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }), 192);
		databaseVersion3.addChunk(chunkA3);

		database.addDatabaseVersion(databaseVersion3);
		assertEquals(chunkA1, database.getChunk(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 })));
		assertEquals(chunkA2, database.getChunk(new ChunkChecksum(new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1 })));
		assertEquals(chunkA3, database.getChunk(new ChunkChecksum(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 })));
	}

	@Test
	public void testMultiChunkCache() throws IOException {
		MemoryDatabase database = new MemoryDatabase();

		// Round 1: Add chunk to multichunk
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();

		MultiChunkEntry multiChunkP1 = new MultiChunkEntry(new MultiChunkId(new byte[] { 8, 8, 8, 8, 8, 8, 8, 8 }), 10);
		ChunkEntry chunkA1 = new ChunkEntry(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 }), 12);

		multiChunkP1.addChunk(chunkA1.getChecksum());
		databaseVersion1.addChunk(chunkA1);
		databaseVersion1.addMultiChunk(multiChunkP1);

		database.addDatabaseVersion(databaseVersion1);

		assertEquals(chunkA1, database.getChunk(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 })));
		assertEquals(multiChunkP1, database.getMultiChunk(new MultiChunkId(new byte[] { 8, 8, 8, 8, 8, 8, 8, 8 })));

		// Round 2: Add chunk to multichunk
		DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);

		MultiChunkEntry multiChunkP2 = new MultiChunkEntry(new MultiChunkId(new byte[] { 7, 7, 7, 7, 7, 7, 7, 7, 7 }), 11);
		MultiChunkEntry multiChunkP3 = new MultiChunkEntry(new MultiChunkId(new byte[] { 5, 5, 5, 5, 5, 5, 5, 5, 5 }), 12);

		ChunkEntry chunkA2 = new ChunkEntry(new ChunkChecksum(new byte[] { 9, 2, 3, 4, 5, 7, 8, 9, 0 }), 912);
		ChunkEntry chunkA3 = new ChunkEntry(new ChunkChecksum(new byte[] { 8, 2, 3, 4, 5, 7, 8, 9, 0 }), 812);
		ChunkEntry chunkA4 = new ChunkEntry(new ChunkChecksum(new byte[] { 7, 2, 3, 4, 5, 7, 8, 9, 0 }), 712);

		multiChunkP2.addChunk(chunkA2.getChecksum());
		multiChunkP2.addChunk(chunkA3.getChecksum());
		multiChunkP3.addChunk(chunkA4.getChecksum());

		databaseVersion2.addChunk(chunkA2);
		databaseVersion2.addChunk(chunkA3);
		databaseVersion2.addChunk(chunkA4);

		databaseVersion2.addMultiChunk(multiChunkP2);
		databaseVersion2.addMultiChunk(multiChunkP3);

		database.addDatabaseVersion(databaseVersion2);

		// fail("xx");

		assertEquals(chunkA1, database.getChunk(new ChunkChecksum(new byte[] { 1, 2, 3, 4, 5, 7, 8, 9, 0 })));
		assertEquals(chunkA2, database.getChunk(new ChunkChecksum(new byte[] { 9, 2, 3, 4, 5, 7, 8, 9, 0 })));
		assertEquals(chunkA3, database.getChunk(new ChunkChecksum(new byte[] { 8, 2, 3, 4, 5, 7, 8, 9, 0 })));
		assertEquals(chunkA4, database.getChunk(new ChunkChecksum(new byte[] { 7, 2, 3, 4, 5, 7, 8, 9, 0 })));
		assertEquals(multiChunkP1, database.getMultiChunk(new MultiChunkId(new byte[] { 8, 8, 8, 8, 8, 8, 8, 8 })));
		assertEquals(multiChunkP2, database.getMultiChunk(new MultiChunkId(new byte[] { 7, 7, 7, 7, 7, 7, 7, 7, 7 })));
		assertEquals(multiChunkP3, database.getMultiChunk(new MultiChunkId(new byte[] { 5, 5, 5, 5, 5, 5, 5, 5, 5 })));
	}

	@Test
	public void testFilenameCache() throws IOException {
		MemoryDatabase database = new MemoryDatabase();

		// Round 1: Add file history & version
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();

		FileVersion fileVersion1 = TestDatabaseUtil.createFileVersion("file1.jpg");
		FileHistoryId idFile1 = FileHistoryId.parseFileId("1111111111111111");
		PartialFileHistory fileHistory1 = new PartialFileHistory(idFile1);

		fileHistory1.addFileVersion(fileVersion1);
		databaseVersion1.addFileHistory(fileHistory1);

		database.addDatabaseVersion(databaseVersion1);

		assertEquals(fileHistory1, database.getFileHistory("file1.jpg"));

		// Round 2: Add new version
		DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);

		FileVersion fileVersion2 = TestDatabaseUtil.createFileVersion("file2.jpg", fileVersion1);
		FileHistoryId idFile2 = FileHistoryId.parseFileId("1111111111111111"); // same ID
		PartialFileHistory fileHistory2 = new PartialFileHistory(idFile2);

		fileHistory2.addFileVersion(fileVersion2);
		databaseVersion2.addFileHistory(fileHistory2);

		database.addDatabaseVersion(databaseVersion2);

		assertNotNull(database.getFileHistory("file2.jpg"));
		assertEquals(2, database.getFileHistory("file2.jpg").getFileVersions().size());
		assertNull(database.getFileHistory("file1.jpg"));

		// Round 3: Add deleted version
		DatabaseVersion databaseVersion3 = TestDatabaseUtil.createDatabaseVersion(databaseVersion2);

		FileVersion fileVersion3 = TestDatabaseUtil.createFileVersion("file2.jpg", fileVersion2);
		fileVersion3.setStatus(FileStatus.DELETED);

		FileHistoryId idFile3 = FileHistoryId.parseFileId("1111111111111111"); // same ID
		PartialFileHistory fileHistory3 = new PartialFileHistory(idFile3); // same ID

		fileHistory3.addFileVersion(fileVersion3);
		databaseVersion3.addFileHistory(fileHistory3);

		database.addDatabaseVersion(databaseVersion3);

		assertNull(database.getFileHistory("file2.jpg"));
	}

	@Test
	public void testFilenameCacheDeleteAndNewOfSameFileInOneDatabaseVersion() throws IOException {
		MemoryDatabase database = new MemoryDatabase();

		// Round 1: Add file history & version
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();

		FileVersion fileVersion1 = TestDatabaseUtil.createFileVersion("file1.jpg");
		FileHistoryId idFile1 = FileHistoryId.parseFileId("1111111111111111");
		PartialFileHistory fileHistory1 = new PartialFileHistory(idFile1);

		fileHistory1.addFileVersion(fileVersion1);
		databaseVersion1.addFileHistory(fileHistory1);

		database.addDatabaseVersion(databaseVersion1);

		assertEquals(fileHistory1, database.getFileHistory("file1.jpg"));

		// Round 2: Add new version
		DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);

		// - delete file1.jpg
		FileVersion fileVersion2 = TestDatabaseUtil.createFileVersion("file1.jpg", fileVersion1);
		fileVersion2.setStatus(FileStatus.DELETED);

		FileHistoryId idFile2 = FileHistoryId.parseFileId("1111111111111111"); // same ID
		PartialFileHistory fileHistory2 = new PartialFileHistory(idFile2);

		fileHistory2.addFileVersion(fileVersion2);
		databaseVersion2.addFileHistory(fileHistory2);

		// - add file1.jpg (as FOLDER!)
		FileVersion fileVersion3 = TestDatabaseUtil.createFileVersion("file1.jpg"); // new file!
		fileVersion3.setType(FileType.FOLDER);

		FileHistoryId idFile3 = FileHistoryId.parseFileId("1111111111111112"); // different ID
		PartialFileHistory fileHistory3 = new PartialFileHistory(idFile3); // same ID

		fileHistory3.addFileVersion(fileVersion3);
		databaseVersion2.addFileHistory(fileHistory3);

		// - add database version
		database.addDatabaseVersion(databaseVersion2);

		assertNotNull(database.getFileHistory("file1.jpg"));
		assertEquals(1, database.getFileHistory("file1.jpg").getFileVersions().size());
		assertEquals(fileHistory3, database.getFileHistory("file1.jpg"));
	}

	@Test
	public void testContentChecksumCache() throws IOException {
		MemoryDatabase database = new MemoryDatabase();

		// Round 1: Add file history & version
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();

		// - history 1, version 1
		FileVersion fileVersion1 = TestDatabaseUtil.createFileVersion("samechecksum1.jpg");
		fileVersion1.setChecksum(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }));

		FileHistoryId idFile1 = FileHistoryId.parseFileId("1111111111111111");
		PartialFileHistory fileHistory1 = new PartialFileHistory(idFile1);

		fileHistory1.addFileVersion(fileVersion1);
		databaseVersion1.addFileHistory(fileHistory1);

		database.addDatabaseVersion(databaseVersion1);

		assertNotNull(database.getFileHistories(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 })));
		assertEquals(1, database.getFileHistories(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 })).size());

		// Round 2: Add two other versions with same checksum to new database version
		DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);

		// - history 1, version 2
		FileVersion fileVersion11 = TestDatabaseUtil.createFileVersion("samechecksum2-renamed.jpg", fileVersion1);
		fileVersion11.setChecksum(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 })); // same checksum!
		fileVersion11.setStatus(FileStatus.RENAMED);

		FileHistoryId idFile11 = FileHistoryId.parseFileId("1111111111111111");
		PartialFileHistory fileHistory11 = new PartialFileHistory(idFile11); // same ID as above

		fileHistory11.addFileVersion(fileVersion11);
		databaseVersion2.addFileHistory(fileHistory11);

		// - history 2, version 1
		FileVersion fileVersion2 = TestDatabaseUtil.createFileVersion("samechecksum2.jpg");
		fileVersion2.setChecksum(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 })); // same checksum!

		FileHistoryId idFile2 = FileHistoryId.parseFileId("1111111122222222"); // different ID
		PartialFileHistory fileHistory2 = new PartialFileHistory(idFile2);

		fileHistory2.addFileVersion(fileVersion2);
		databaseVersion2.addFileHistory(fileHistory2);

		// - history 3, version 1
		FileVersion fileVersion3 = TestDatabaseUtil.createFileVersion("samechecksum3.jpg");
		fileVersion3.setChecksum(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 })); // same checksum!

		FileHistoryId idFile3 = FileHistoryId.parseFileId("aaaaaaaa22222222"); // different ID
		PartialFileHistory fileHistory3 = new PartialFileHistory(idFile3); // different ID

		fileHistory3.addFileVersion(fileVersion3);
		databaseVersion2.addFileHistory(fileHistory3);

		database.addDatabaseVersion(databaseVersion2);

		assertNotNull(database.getFileHistories(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 })));
		assertEquals(3, database.getFileHistories(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 })).size());
	}

	@Test
	public void testGetFileHistory() throws IOException {
		MemoryDatabase database = new MemoryDatabase();

		// Round 1: Add file history & version
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();

		// - history 1, version 1
		FileVersion fileVersion1 = TestDatabaseUtil.createFileVersion("samechecksum1.jpg");
		fileVersion1.setChecksum(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }));

		FileHistoryId idFile1 = FileHistoryId.parseFileId("1111111111111111");
		PartialFileHistory fileHistory1 = new PartialFileHistory(idFile1);

		fileHistory1.addFileVersion(fileVersion1);
		databaseVersion1.addFileHistory(fileHistory1);

		database.addDatabaseVersion(databaseVersion1);

		FileHistoryId idFile1Get = FileHistoryId.parseFileId("1111111111111111");
		assertNotNull(database.getFileHistory(idFile1Get));
		assertEquals(fileHistory1, database.getFileHistory(idFile1Get));

		// Round 2: Add two other versions with same checksum to new database version
		DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);

		// - history 1, version 2
		FileVersion fileVersion11 = TestDatabaseUtil.createFileVersion("samechecksum2-renamed.jpg", fileVersion1);
		fileVersion11.setChecksum(new FileChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 })); // same checksum!
		fileVersion11.setStatus(FileStatus.RENAMED);

		FileHistoryId idFile11 = FileHistoryId.parseFileId("1111111111111111"); // same ID
		PartialFileHistory fileHistory11 = new PartialFileHistory(idFile11);

		fileHistory11.addFileVersion(fileVersion11);
		databaseVersion2.addFileHistory(fileHistory11);

		database.addDatabaseVersion(databaseVersion2);

		FileHistoryId idFile111 = FileHistoryId.parseFileId("1111111111111111"); // same ID
		assertNotNull(database.getFileHistory(idFile111));
		assertEquals(2, database.getFileHistory(idFile111).getFileVersions().size());
	}

	@Test
	public void testRemoveDatabaseVersion() {
		MemoryDatabase database = new MemoryDatabase();

		// Round 1: Add file history & version
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();

		// - history 1, version 1
		FileVersion fileVersion1 = TestDatabaseUtil.createFileVersion("file.jpg");

		FileHistoryId idFile1 = FileHistoryId.parseFileId("1111111111111111");
		PartialFileHistory fileHistory1 = new PartialFileHistory(idFile1);

		fileHistory1.addFileVersion(fileVersion1);
		databaseVersion1.addFileHistory(fileHistory1);

		database.addDatabaseVersion(databaseVersion1);
		
		// - history 1, version 2		
		DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);
		FileVersion fileVersion2 = TestDatabaseUtil.createFileVersion("file.jpg", fileVersion1);

		FileHistoryId idFile1b = FileHistoryId.parseFileId("1111111111111111");
		PartialFileHistory fileHistory1b = new PartialFileHistory(idFile1b);

		fileHistory1b.addFileVersion(fileVersion2);
		databaseVersion2.addFileHistory(fileHistory1b);

		database.addDatabaseVersion(databaseVersion2);
				
		// Tests: Database should have 2 versions of file
		assertEquals(2, database.getFileHistory(idFile1).getFileVersions().size());
		assertEquals(2, database.getFileHistory(idFile1b).getFileVersions().size());
		
		
		// Round 2: Remove second database version
		database.removeDatabaseVersion(databaseVersion2);
		
		// Tests: Second version removed, 1 version left
		assertEquals(1, database.getFileHistory(idFile1).getFileVersions().size());
		assertEquals(1, database.getFileHistory(idFile1b).getFileVersions().size());
		assertEquals(fileVersion1, database.getFileHistory(idFile1).getLastVersion());
		
		
		// Round 3: Add database version again
		database.addDatabaseVersion(databaseVersion2);
		
		// Tests: Second version added, 2 versions of file
		assertEquals(2, database.getFileHistory(idFile1).getFileVersions().size());
		assertEquals(2, database.getFileHistory(idFile1b).getFileVersions().size());
		
		
		// Round 4: Remove FIRST database version		
		database.removeDatabaseVersion(databaseVersion1);
		
		// Tests: First version removed, 1 version left
		assertEquals(1, database.getFileHistory(idFile1).getFileVersions().size());
		assertEquals(1, database.getFileHistory(idFile1b).getFileVersions().size());
		assertEquals(fileVersion2, database.getFileHistory(idFile1).getLastVersion());
		

		// Round 5: Remove second database version		
		database.removeDatabaseVersion(databaseVersion2);

		// Tests: Second version removed, none left
		assertNull(database.getFileHistory(idFile1));		
	}

}
