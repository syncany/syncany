package org.syncany.tests.database;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.database.ChunkEntry;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestDatabaseUtil;
import org.syncany.tests.util.TestFileUtil;

public class DatabaseXmlDAOTest {
	private File tempDir;
	
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
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
		
		// Create chunks
        ChunkEntry chunkA1 = new ChunkEntry(new byte[] { 1,2,3,4,5,7,8,9,0}, 12);
        ChunkEntry chunkA2 = new ChunkEntry(new byte[] { 9,8,7,6,5,4,3,2,1}, 34);
        ChunkEntry chunkA3 = new ChunkEntry(new byte[] { 1,1,1,1,1,1,1,1,1}, 56);
        ChunkEntry chunkA4 = new ChunkEntry(new byte[] { 2,2,2,2,2,2,2,2,2}, 78);
        
        newDatabaseVersion.addChunk(chunkA1);
        newDatabaseVersion.addChunk(chunkA2);
        newDatabaseVersion.addChunk(chunkA3);
        newDatabaseVersion.addChunk(chunkA4);
		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		
		// Check chunks
		assertEquals("Chunk not found in database loaded.", chunkA1, loadedDatabase.getChunk(chunkA1.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA2, loadedDatabase.getChunk(chunkA2.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA3, loadedDatabase.getChunk(chunkA3.getChecksum()));
		assertEquals("Chunk not found in database loaded.", chunkA4, loadedDatabase.getChunk(chunkA4.getChecksum()));
	}
		
	@Test
	public void testWriteAndReadChunksWithMultiChunks() throws IOException {
		// Prepare
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
		
		// Create chunks
        ChunkEntry chunkA1 = new ChunkEntry(new byte[] { 1,2,3,4,5,7,8,9,0}, 12);
        ChunkEntry chunkA2 = new ChunkEntry(new byte[] { 9,8,7,6,5,4,3,2,1}, 34);
        ChunkEntry chunkA3 = new ChunkEntry(new byte[] { 1,1,1,1,1,1,1,1,1}, 56);
        ChunkEntry chunkA4 = new ChunkEntry(new byte[] { 2,2,2,2,2,2,2,2,2}, 78);

        ChunkEntry chunkB1 = new ChunkEntry(new byte[] { 3,3,3,3,3,3,3,3,3}, 910);
        ChunkEntry chunkB2 = new ChunkEntry(new byte[] { 4,4,4,4,4,4,4,4,4}, 1112);
        
        newDatabaseVersion.addChunk(chunkA1);
        newDatabaseVersion.addChunk(chunkA2);
        newDatabaseVersion.addChunk(chunkA3);
        newDatabaseVersion.addChunk(chunkA4);
        newDatabaseVersion.addChunk(chunkB1);
        newDatabaseVersion.addChunk(chunkB2);        
        
        // Distribute chunks to multichunks
        MultiChunkEntry multiChunkA = new MultiChunkEntry(new byte[] {6,6,6,6,6,6,6,6,6});
        multiChunkA.addChunk(new ChunkEntryId(chunkA1.getChecksum())); 
        multiChunkA.addChunk(new ChunkEntryId(chunkA2.getChecksum())); 
        multiChunkA.addChunk(new ChunkEntryId(chunkA3.getChecksum()));
        newDatabaseVersion.addMultiChunk(multiChunkA);
        
        MultiChunkEntry multiChunkB = new MultiChunkEntry(new byte[] {7,7,7,7,7,7,7,7,7});
        multiChunkB.addChunk(new ChunkEntryId(chunkA4.getChecksum()));
        multiChunkB.addChunk(new ChunkEntryId(chunkB1.getChecksum()));
        multiChunkB.addChunk(new ChunkEntryId(chunkB2.getChecksum()));
        newDatabaseVersion.addMultiChunk(multiChunkB);        
        		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		
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
	
		assertArrayEquals("Chunks in multichunk expected to be different.", multiChunkA.getChunks().toArray(), loadedMultiChunkA.getChunks().toArray());
		assertArrayEquals("Chunks in multichunk expected to be different.", multiChunkB.getChunks().toArray(), loadedMultiChunkB.getChunks().toArray());
	}	
	
	@Test
	public void testWriteAndReadChunksWithFileContents() throws IOException {
		// Prepare
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
		
		// Create chunks
        ChunkEntry chunkA1 = new ChunkEntry(new byte[] { 1,2,3,4,5,7,8,9,0}, 12);
        ChunkEntry chunkA2 = new ChunkEntry(new byte[] { 9,8,7,6,5,4,3,2,1}, 34);
        ChunkEntry chunkA3 = new ChunkEntry(new byte[] { 1,1,1,1,1,1,1,1,1}, 56);
        ChunkEntry chunkA4 = new ChunkEntry(new byte[] { 2,2,2,2,2,2,2,2,2}, 78);

        ChunkEntry chunkB1 = new ChunkEntry(new byte[] { 3,3,3,3,3,3,3,3,3}, 910);
        ChunkEntry chunkB2 = new ChunkEntry(new byte[] { 4,4,4,4,4,4,4,4,4}, 1112);
        
        newDatabaseVersion.addChunk(chunkA1);
        newDatabaseVersion.addChunk(chunkA2);
        newDatabaseVersion.addChunk(chunkA3);
        newDatabaseVersion.addChunk(chunkA4);
        newDatabaseVersion.addChunk(chunkB1);
        newDatabaseVersion.addChunk(chunkB2);        
        
        // Distribute chunks to file contents    	
        FileContent contentA = new FileContent();        
        contentA.addChunk(new ChunkEntryId(chunkA1.getChecksum()));
        contentA.addChunk(new ChunkEntryId(chunkA2.getChecksum()));
        contentA.addChunk(new ChunkEntryId(chunkA3.getChecksum()));
        contentA.addChunk(new ChunkEntryId(chunkA4.getChecksum()));
        contentA.setChecksum(new byte[]{5,5,5,4,4,5,5,5,5});              
        newDatabaseVersion.addFileContent(contentA);
                
        FileContent contentB = new FileContent();
        contentB.addChunk(new ChunkEntryId(chunkB1.getChecksum()));
        contentB.addChunk(new ChunkEntryId(chunkB2.getChecksum())); 
        contentB.setChecksum(new byte[]{1,1,1,3,3,5,5,5,5});                      
        newDatabaseVersion.addFileContent(contentB);
        		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		
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
		assertEquals("File content not found in database loaded.", contentB, loadedContentB	);
	
		assertArrayEquals("Chunks in file content expected to be different.", contentA.getChunks().toArray(), loadedContentA.getChunks().toArray());
		assertArrayEquals("Chunks in file content expected to be different.", contentB.getChunks().toArray(), loadedContentB.getChunks().toArray());
	}
	
	@Test
	public void testWriteAndReadFileHistoryAndFileVersion() throws IOException {
		// Prepare
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
	
		// Create directories (no content!)

		// File A
		PartialFileHistory fileHistoryA = new PartialFileHistory();
		newDatabaseVersion.addFileHistory(fileHistoryA);
		
        FileVersion versionA1 = new FileVersion();
        versionA1.setVersion(1L);
        versionA1.setType(FileType.FOLDER);
        versionA1.setPath("Pictures/2013");
        versionA1.setName("New York Folder");
        versionA1.setStatus(FileStatus.NEW);
        versionA1.setSize(0L);
        newDatabaseVersion.addFileVersionToHistory(fileHistoryA.getFileId(), versionA1);
        
        FileVersion versionA2 = new FileVersion();
        versionA2.setVersion(2L);
        versionA2.setType(FileType.FOLDER);
        versionA2.setPath("Pictures/2013");
        versionA2.setName("New York");  
        versionA2.setStatus(FileStatus.RENAMED);
        versionA2.setSize(0L);
        newDatabaseVersion.addFileVersionToHistory(fileHistoryA.getFileId(), versionA2);	
		       
        // File B
		PartialFileHistory fileHistoryB = new PartialFileHistory();
		newDatabaseVersion.addFileHistory(fileHistoryB);
		
        FileVersion versionB1 = new FileVersion();
        versionB1.setVersion(1L);
        versionB1.setType(FileType.FOLDER);
        versionB1.setPath("Pictures/2013");
        versionB1.setName("Egypt Folder");
        versionB1.setStatus(FileStatus.NEW);
        versionB1.setSize(0L);
        newDatabaseVersion.addFileVersionToHistory(fileHistoryB.getFileId(), versionB1);
        
        FileVersion versionB2 = new FileVersion();
        versionB2.setVersion(2L);
        versionB2.setType(FileType.FOLDER);
        versionB2.setPath("Pictures/2013");
        versionB2.setName("Egypt");        
        versionB2.setStatus(FileStatus.RENAMED);
        versionB2.setSize(0L);
        newDatabaseVersion.addFileVersionToHistory(fileHistoryB.getFileId(), versionB2);	        	
        		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		 
		// File histories
		PartialFileHistory loadedFileHistoryA = loadedDatabase.getFileHistory(fileHistoryA.getFileId());
		PartialFileHistory loadedFileHistoryB = loadedDatabase.getFileHistory(fileHistoryB.getFileId());
		
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
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();

		// Create new vector clock
		VectorClock vc = new VectorClock();
		
		vc.setClock("User 1", 14234234L);
		vc.setClock("User 2", 9433431232432L);
		vc.setClock("User 3", 1926402374L);
		
		newDatabaseVersion.setVectorClock(vc);
		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		Database loadedDatabase = writeReadAndCompareDatabase(newDatabase);
		
		// Check VC
		DatabaseVersion loadedDatabaseVersionSelectedByVectorClock = loadedDatabase.getDatabaseVersion(vc);
		DatabaseVersion loadedDatabaseVersionSelectedFirst = loadedDatabase.getDatabaseVersions().get(0);
				
		assertEquals("Vector clocks do not match (selected by vector clock)", vc, loadedDatabaseVersionSelectedByVectorClock.getVectorClock());
		assertEquals("Vector clocks do not match (selected first)", vc, loadedDatabaseVersionSelectedFirst.getVectorClock());
		assertEquals("Database versions do not match.", loadedDatabaseVersionSelectedByVectorClock, loadedDatabaseVersionSelectedFirst);		
	}
		
	@Test
	@Ignore
	@SuppressWarnings("unused")
	public void testWriteAndReadMultipleDatabaseVersions() {
		Database newDatabase = new Database();
		DatabaseVersion firstDatabaseVersion = new DatabaseVersion();
		DatabaseVersion secondDatabaseVersion = new DatabaseVersion();

		// TODO [low] testWriteAndReadMultipleDatabaseVersions
	}
	
	private Database writeReadAndCompareDatabase(Database writtenDatabase) throws IOException {
		File writtenDatabaseFile = new File(tempDir+"/db-"+Math.random()+"-" + new Random().nextInt(Integer.MAX_VALUE));
		TestDatabaseUtil.writeDatabaseFileToDisk(writtenDatabase, writtenDatabaseFile, null);
		Database readDatabase = TestDatabaseUtil.readDatabaseFileFromDisk(writtenDatabaseFile, null);
		
		TestAssertUtil.assertDatabaseEquals(writtenDatabase, readDatabase);
		
		return readDatabase;
	}		
	
}
