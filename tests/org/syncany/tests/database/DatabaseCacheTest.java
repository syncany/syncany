/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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

import org.junit.Ignore;
import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.tests.util.TestDatabaseUtil;

public class DatabaseCacheTest {	
	static {
		Logging.init();
	}
	
	@Test
	public void testChunkCache() throws IOException {		
		Database database = new Database();

		// Round 1: Add chunk to new database version, then add database version
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();		
        
		ChunkEntry chunkA1 = new ChunkEntry(new byte[] { 1,2,3,4,5,7,8,9,0 }, 12);
        databaseVersion1.addChunk(chunkA1);
        
        database.addDatabaseVersion(databaseVersion1);
        assertEquals(chunkA1, database.getChunk(new byte[] { 1,2,3,4,5,7,8,9,0 }));

		// Round 2: Add chunk to new database version, then add database version
		DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);		

		ChunkEntry chunkA2 = new ChunkEntry(new byte[] { 9,8,7,6,5,4,3,2,1 }, 112);        
        databaseVersion2.addChunk(chunkA2);

        database.addDatabaseVersion(databaseVersion2);
        assertEquals(chunkA1, database.getChunk(new byte[] { 1,2,3,4,5,7,8,9,0 }));
        assertEquals(chunkA2, database.getChunk(new byte[] { 9,8,7,6,5,4,3,2,1 }));        
        
        // Round 3: Add chunk to new database version, then add database version
		DatabaseVersion databaseVersion3 = TestDatabaseUtil.createDatabaseVersion(databaseVersion2);		
		
		ChunkEntry chunkA3 = new ChunkEntry(new byte[] { 1,1,1,1,1,1,1,1,1 }, 192);        
        databaseVersion3.addChunk(chunkA3);
        
        database.addDatabaseVersion(databaseVersion3);		
		assertEquals(chunkA1, database.getChunk(new byte[] { 1,2,3,4,5,7,8,9,0 }));
		assertEquals(chunkA2, database.getChunk(new byte[] { 9,8,7,6,5,4,3,2,1 }));
		assertEquals(chunkA3, database.getChunk(new byte[] { 1,1,1,1,1,1,1,1,1 }));		
	}
	
	@Test
	public void testMultiChunkCache() throws IOException {		
		Database database = new Database();

		// Round 1: Add chunk to multichunk
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();		
        
		MultiChunkEntry multiChunkP1 = new MultiChunkEntry(new byte[] { 8,8,8,8,8,8,8,8 });
		ChunkEntry chunkA1 = new ChunkEntry(new byte[] { 1,2,3,4,5,7,8,9,0 }, 12);
		
		multiChunkP1.addChunk(new ChunkEntryId(chunkA1.getChecksum()));        
		databaseVersion1.addChunk(chunkA1);		
		databaseVersion1.addMultiChunk(multiChunkP1);
		
        database.addDatabaseVersion(databaseVersion1);
        
        assertEquals(chunkA1, database.getChunk(new byte[] { 1,2,3,4,5,7,8,9,0 }));
        assertEquals(multiChunkP1, database.getMultiChunk(new byte[] { 8,8,8,8,8,8,8,8 }));

		// Round 2: Add chunk to multichunk
		DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);		
        
		MultiChunkEntry multiChunkP2 = new MultiChunkEntry(new byte[] { 7,7,7,7,7,7,7,7,7 });		
		MultiChunkEntry multiChunkP3 = new MultiChunkEntry(new byte[] { 5,5,5,5,5,5,5,5,5 });

		ChunkEntry chunkA2 = new ChunkEntry(new byte[] { 9,2,3,4,5,7,8,9,0 }, 912);
		ChunkEntry chunkA3 = new ChunkEntry(new byte[] { 8,2,3,4,5,7,8,9,0 }, 812);
		ChunkEntry chunkA4 = new ChunkEntry(new byte[] { 7,2,3,4,5,7,8,9,0 }, 712);

		multiChunkP2.addChunk(new ChunkEntryId(chunkA2.getChecksum()));
		multiChunkP2.addChunk(new ChunkEntryId(chunkA3.getChecksum()));
		multiChunkP3.addChunk(new ChunkEntryId(chunkA4.getChecksum()));

		databaseVersion2.addChunk(chunkA2);
		databaseVersion2.addChunk(chunkA3);
		databaseVersion2.addChunk(chunkA4);

		databaseVersion2.addMultiChunk(multiChunkP2);	
		databaseVersion2.addMultiChunk(multiChunkP3);
		
		database.addDatabaseVersion(databaseVersion2);
		
		//fail("xx");
        
		assertEquals(chunkA1, database.getChunk(new byte[] { 1,2,3,4,5,7,8,9,0 }));
        assertEquals(chunkA2, database.getChunk(new byte[] { 9,2,3,4,5,7,8,9,0 }));
        assertEquals(chunkA3, database.getChunk(new byte[] { 8,2,3,4,5,7,8,9,0 }));
        assertEquals(chunkA4, database.getChunk(new byte[] { 7,2,3,4,5,7,8,9,0 }));
        assertEquals(multiChunkP1, database.getMultiChunk(new byte[] { 8,8,8,8,8,8,8,8 }));
        assertEquals(multiChunkP2, database.getMultiChunk(new byte[] { 7,7,7,7,7,7,7,7,7 }));
        assertEquals(multiChunkP3, database.getMultiChunk(new byte[] { 5,5,5,5,5,5,5,5,5 }));
	}	
	
	@Test
	public void testFilenameCache() throws IOException {		
		Database database = new Database();

		// Round 1: Add file history & version 
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();		
        
		FileVersion fileVersion1 = TestDatabaseUtil.createFileVersion("file1.jpg");		
		PartialFileHistory fileHistory1 = new PartialFileHistory(11111111111111111L);		
		
		databaseVersion1.addFileHistory(fileHistory1);
		databaseVersion1.addFileVersionToHistory(fileHistory1.getFileId(), fileVersion1);
		
		database.addDatabaseVersion(databaseVersion1);     
		
        assertEquals(fileHistory1, database.getFileHistory("file1.jpg"));
        
        // Round 2: Add new version
        DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);		
        
		FileVersion fileVersion2 = TestDatabaseUtil.createFileVersion("file2.jpg", fileVersion1);		
		PartialFileHistory fileHistory2 = new PartialFileHistory(11111111111111111L); // same ID		
		
		databaseVersion2.addFileHistory(fileHistory2);
		databaseVersion2.addFileVersionToHistory(fileHistory2.getFileId(), fileVersion2);
		
		database.addDatabaseVersion(databaseVersion2);   
		
        assertNotNull(database.getFileHistory("file2.jpg"));
        assertEquals(2, database.getFileHistory("file2.jpg").getFileVersions().size());
        assertNull(database.getFileHistory("file1.jpg"));
        
        // Round 3: Add deleted version
        DatabaseVersion databaseVersion3 = TestDatabaseUtil.createDatabaseVersion(databaseVersion2);		
        
		FileVersion fileVersion3 = TestDatabaseUtil.createFileVersion("file2.jpg", fileVersion2);
		fileVersion3.setStatus(FileStatus.DELETED);
		
		PartialFileHistory fileHistory3 = new PartialFileHistory(11111111111111111L); // same ID		
		
		databaseVersion3.addFileHistory(fileHistory3);
		databaseVersion3.addFileVersionToHistory(fileHistory3.getFileId(), fileVersion3);
		
		database.addDatabaseVersion(databaseVersion3);   
		
        assertNull(database.getFileHistory("file2.jpg"));            
	}	
	
	@Test
	public void testFilenameCacheDeleteAndNewOfSameFileInOneDatabaseVersion() throws IOException {		
		Database database = new Database();

		// Round 1: Add file history & version 
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();		
        
		FileVersion fileVersion1 = TestDatabaseUtil.createFileVersion("file1.jpg");		
		PartialFileHistory fileHistory1 = new PartialFileHistory(11111111111111111L);		
		
		databaseVersion1.addFileHistory(fileHistory1);
		databaseVersion1.addFileVersionToHistory(fileHistory1.getFileId(), fileVersion1);
		
		database.addDatabaseVersion(databaseVersion1);     
		
        assertEquals(fileHistory1, database.getFileHistory("file1.jpg"));
        
        // Round 2: Add new version
        DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);		
        
        // - delete file1.jpg
		FileVersion fileVersion2 = TestDatabaseUtil.createFileVersion("file1.jpg", fileVersion1);
		fileVersion2.setStatus(FileStatus.DELETED);
		
		PartialFileHistory fileHistory2 = new PartialFileHistory(11111111111111111L); // same ID		
		
		databaseVersion2.addFileHistory(fileHistory2);
		databaseVersion2.addFileVersionToHistory(fileHistory2.getFileId(), fileVersion2);
		
		// - add file1.jpg (as FOLDER!)
		FileVersion fileVersion3 = TestDatabaseUtil.createFileVersion("file1.jpg"); // new file!
		fileVersion3.setType(FileType.FOLDER);
		
		PartialFileHistory fileHistory3 = new PartialFileHistory(222222222L); // new ID	!	
		
		databaseVersion2.addFileHistory(fileHistory3);
		databaseVersion2.addFileVersionToHistory(fileHistory3.getFileId(), fileVersion3);
		
		// - add datbase version
		database.addDatabaseVersion(databaseVersion2);   
		
        assertNotNull(database.getFileHistory("file1.jpg"));
        assertEquals(1, database.getFileHistory("file1.jpg").getFileVersions().size());
        assertEquals(fileHistory3, database.getFileHistory("file1.jpg"));        
	}		

	@Test
	public void testContentChecksumCache() throws IOException {		
		Database database = new Database();

		// Round 1: Add file history & version 
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();		
        
		// - history 1, version 1
		FileVersion fileVersion1 = TestDatabaseUtil.createFileVersion("samechecksum1.jpg");
		fileVersion1.setChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 });
		
		PartialFileHistory fileHistory1 = new PartialFileHistory(11111111111111111L);		
		
		databaseVersion1.addFileHistory(fileHistory1);
		databaseVersion1.addFileVersionToHistory(fileHistory1.getFileId(), fileVersion1);
		
		database.addDatabaseVersion(databaseVersion1);     
		
        assertNotNull(database.getFileHistories(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }));
        assertEquals(1, database.getFileHistories(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }).size());
        
        // Round 2: Add two other versions with same checksum to new database version
        DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);		
        
        // - history 1, version 2
        FileVersion fileVersion11 = TestDatabaseUtil.createFileVersion("samechecksum2-renamed.jpg", fileVersion1);
        fileVersion11.setChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }); // same checksum!
        fileVersion11.setStatus(FileStatus.RENAMED);
		
		PartialFileHistory fileHistory11 = new PartialFileHistory(11111111111111111L); // same ID as above		
		
		databaseVersion2.addFileHistory(fileHistory11);
		databaseVersion2.addFileVersionToHistory(fileHistory11.getFileId(), fileVersion11);
        
        // - history 2, version 1
		FileVersion fileVersion2 = TestDatabaseUtil.createFileVersion("samechecksum2.jpg");
		fileVersion2.setChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }); // same checksum!
		
		PartialFileHistory fileHistory2 = new PartialFileHistory(22222222222222222L); // different ID		
		
		databaseVersion2.addFileHistory(fileHistory2);
		databaseVersion2.addFileVersionToHistory(fileHistory2.getFileId(), fileVersion2);

		// - history 3, version 1
		FileVersion fileVersion3 = TestDatabaseUtil.createFileVersion("samechecksum3.jpg");
		fileVersion3.setChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }); // same checksum!
		
		PartialFileHistory fileHistory3 = new PartialFileHistory(33333333333333333L); // different ID		
		
		databaseVersion2.addFileHistory(fileHistory3);
		databaseVersion2.addFileVersionToHistory(fileHistory3.getFileId(), fileVersion3);
		
		database.addDatabaseVersion(databaseVersion2);   
		
		assertNotNull(database.getFileHistories(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }));
        assertEquals(3, database.getFileHistories(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }).size());        
	}		

	@Test
	public void testGetFileHistory() throws IOException {		
		Database database = new Database();

		// Round 1: Add file history & version 
		DatabaseVersion databaseVersion1 = TestDatabaseUtil.createDatabaseVersion();		
        
		// - history 1, version 1
		FileVersion fileVersion1 = TestDatabaseUtil.createFileVersion("samechecksum1.jpg");
		fileVersion1.setChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 });
		
		PartialFileHistory fileHistory1 = new PartialFileHistory(11111111111111111L);		
		
		databaseVersion1.addFileHistory(fileHistory1);
		databaseVersion1.addFileVersionToHistory(fileHistory1.getFileId(), fileVersion1);
		
		database.addDatabaseVersion(databaseVersion1);     
		
		assertNotNull(database.getFileHistory(11111111111111111L));
		assertEquals(fileHistory1, database.getFileHistory(11111111111111111L));
		
		// Round 2: Add two other versions with same checksum to new database version
        DatabaseVersion databaseVersion2 = TestDatabaseUtil.createDatabaseVersion(databaseVersion1);		
        
        // - history 1, version 2
        FileVersion fileVersion11 = TestDatabaseUtil.createFileVersion("samechecksum2-renamed.jpg", fileVersion1);
        fileVersion11.setChecksum(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 }); // same checksum!
        fileVersion11.setStatus(FileStatus.RENAMED);
		
		PartialFileHistory fileHistory11 = new PartialFileHistory(11111111111111111L); // same ID as above		
		
		databaseVersion2.addFileHistory(fileHistory11);
		databaseVersion2.addFileVersionToHistory(fileHistory11.getFileId(), fileVersion11);
		
		database.addDatabaseVersion(databaseVersion2);     
		
		assertNotNull(database.getFileHistory(11111111111111111L));
		assertEquals(2, database.getFileHistory(11111111111111111L).getFileVersions().size());		
	}
	
	@Test
	@Ignore
	public void testRemoveDatabaseVersion() {
		// TODO [medium] Implement this
	}
		
	
}
