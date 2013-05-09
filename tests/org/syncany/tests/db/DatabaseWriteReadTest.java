package org.syncany.tests.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.syncany.db.ChunkEntry;
import org.syncany.db.Database;
import org.syncany.db.DatabaseDAO;
import org.syncany.db.DatabaseVersion;
import org.syncany.db.FileContent;
import org.syncany.db.FileHistoryPart;
import org.syncany.db.FileVersion;
import org.syncany.db.MultiChunkEntry;
import org.syncany.tests.util.TestUtil;

public class DatabaseWriteReadTest {
	private File tempDir;
	
	@Before
	public void setUp() throws Exception {
		tempDir = TestUtil.createTempDirectoryInSystemTemp();		
	}
	
	@After
	public void tearDown() {
		TestUtil.deleteDirectory(tempDir);
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
		writeReadAndCompareDatabase(newDatabase);
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
        MultiChunkEntry multiChunkA = new MultiChunkEntry();
        multiChunkA.addChunk(chunkA1); 
        multiChunkA.addChunk(chunkA2); 
        multiChunkA.addChunk(chunkA3); 
        multiChunkA.setChecksum(new byte[] {6,6,6,6,6,6,6,6,6});
        newDatabaseVersion.addMultiChunk(multiChunkA);
        
        MultiChunkEntry multiChunkB = new MultiChunkEntry();
        multiChunkB.addChunk(chunkA4); 
        multiChunkB.addChunk(chunkB1); 
        multiChunkB.addChunk(chunkB2); 
        multiChunkB.setChecksum(new byte[] {7,7,7,7,7,7,7,7,7});
        newDatabaseVersion.addMultiChunk(multiChunkB);        
        		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		writeReadAndCompareDatabase(newDatabase);
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
        contentA.addChunk(chunkA1);
        contentA.addChunk(chunkA2);
        contentA.addChunk(chunkA3);
        contentA.addChunk(chunkA4);
        contentA.setChecksum(new byte[]{5,5,5,4,4,5,5,5,5});              
        newDatabaseVersion.addFileContent(contentA);
                
        FileContent contentB = new FileContent();
        contentB.addChunk(chunkB1);
        contentB.addChunk(chunkB2);   
        contentB.setChecksum(new byte[]{1,1,1,3,3,5,5,5,5});                      
        newDatabaseVersion.addFileContent(contentB);
        		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		writeReadAndCompareDatabase(newDatabase);		
	}
	
	@Test
	public void testWriteAndReadFileHistoryAndFileVersion() throws IOException {
		// Prepare
		Database newDatabase = new Database();
		DatabaseVersion newDatabaseVersion = new DatabaseVersion();
	
		// Create directories (no content!)

		// File A
		FileHistoryPart fileHistoryA = new FileHistoryPart();
		newDatabaseVersion.addFileHistory(fileHistoryA);
		
        FileVersion versionA1 = new FileVersion();
        versionA1.setVersion(1L);
        versionA1.setPath("Pictures/2013");
        versionA1.setName("New York Folder");
        newDatabaseVersion.addFileVersionToHistory(fileHistoryA.getFileId(), versionA1);
        
        FileVersion versionA2 = new FileVersion();
        versionA2.setVersion(2L);
        versionA2.setPath("Pictures/2013");
        versionA2.setName("New York");        
        newDatabaseVersion.addFileVersionToHistory(fileHistoryA.getFileId(), versionA2);	
		       
        // File B
		FileHistoryPart fileHistoryB = new FileHistoryPart();
		newDatabaseVersion.addFileHistory(fileHistoryB);
		
        FileVersion versionB1 = new FileVersion();
        versionB1.setVersion(1L);
        versionB1.setPath("Pictures/2013");
        versionB1.setName("Egypt Folder");
        newDatabaseVersion.addFileVersionToHistory(fileHistoryB.getFileId(), versionB1);
        
        FileVersion versionB2 = new FileVersion();
        versionB2.setVersion(2L);
        versionB2.setPath("Pictures/2013");
        versionB2.setName("Egypt");        
        newDatabaseVersion.addFileVersionToHistory(fileHistoryB.getFileId(), versionB1);	        	
        		
        // Add database version
		newDatabase.addDatabaseVersion(newDatabaseVersion);
		
		// Write database to disk, read it again, and compare them
		writeReadAndCompareDatabase(newDatabase);
	}	
	
	@Test
	@Ignore
	public void testWriteAndReadMultipleDatabaseVersions() {
		Database newDatabase = new Database();
		DatabaseVersion firstDatabaseVersion = new DatabaseVersion();
		DatabaseVersion secondDatabaseVersion = new DatabaseVersion();

		// TODO testWriteAndReadMultipleDatabaseVersions
	}
	
	private void writeReadAndCompareDatabase(Database writtenDatabase) throws IOException {
		File writtenDatabaseFile = writeDatabaseFileToDisk(writtenDatabase);
		Database readDatabase = readDatabaseFileFromDisk(writtenDatabaseFile);
		
		compareDatabases(writtenDatabase, readDatabase);
	}		

	private File writeDatabaseFileToDisk(Database db) throws IOException {
		File writtenDatabaseFile = new File(tempDir+"/db-"+Math.random());
		
		DatabaseDAO dao = new DatabaseDAO();
		dao.save(db, writtenDatabaseFile);
		
		return writtenDatabaseFile;
	}
	
	private Database readDatabaseFileFromDisk(File databaseFile) throws IOException {
		Database db = new Database();
		
		DatabaseDAO dao = new DatabaseDAO();
		dao.load(db, databaseFile);
		
		return db;
	}
	
	private void compareDatabases(Database writtenDatabase, Database readDatabase) {
		Map<Long, DatabaseVersion> writtenDatabaseVersions = writtenDatabase.getDatabaseVersions();
		Map<Long, DatabaseVersion> readDatabaseVersions = readDatabase.getDatabaseVersions();
		
		assertEquals("Different number of database versions.", writtenDatabaseVersions.size(), readDatabaseVersions.size());
			
		for (Map.Entry<Long, DatabaseVersion> writtenDatabaseVersionEntry : writtenDatabaseVersions.entrySet()) {
			long writtenDatabaseVersionNumber = writtenDatabaseVersionEntry.getKey();
			DatabaseVersion writtenDatabaseVersion = writtenDatabaseVersionEntry.getValue();
			
			DatabaseVersion readDatabaseVersion = readDatabaseVersions.get(writtenDatabaseVersionNumber);
			assertNotNull("Database version "+writtenDatabaseVersionNumber+" does not exist in read database.", readDatabaseVersion);
			
			compareDatabaseVersions(writtenDatabaseVersion, readDatabaseVersion);
		}
	}	
	
	private void compareDatabaseVersions(DatabaseVersion writtenDatabaseVersion, DatabaseVersion readDatabaseVersion) {
		compareDatabaseVersionChunks(writtenDatabaseVersion.getChunks(), readDatabaseVersion.getChunks());
		compareDatabaseVersionMultiChunks(writtenDatabaseVersion.getMultiChunks(), readDatabaseVersion.getMultiChunks());
		compareDatabaseVersionFileContents(writtenDatabaseVersion.getFileContents(), readDatabaseVersion.getFileContents());
		// TODO File Content + the rest
		
	}	
	
	private void compareDatabaseVersionChunks(Collection<ChunkEntry> writtenChunks, Collection<ChunkEntry> readChunks) {	
		assertEquals("Different amount of Chunk objects.", writtenChunks.size(), readChunks.size());
		assertTrue("Chunk objects in written/read database version different.", writtenChunks.containsAll(readChunks));
	}
	
	private void compareDatabaseVersionMultiChunks(Collection<MultiChunkEntry> writtenMultiChunks, Collection<MultiChunkEntry> readMultiChunks) {
		assertEquals("Different amount of MultiChunk objects.", writtenMultiChunks.size(), readMultiChunks.size());
		assertTrue("MultiChunk objects in written/read database version different.", writtenMultiChunks.containsAll(readMultiChunks));		
	}	
	
	private void compareDatabaseVersionFileContents(Collection<FileContent> writtenFileContents, Collection<FileContent> readFileContents) {
		assertEquals("Different amount of FileContent objects.", writtenFileContents.size(), readFileContents.size());
		assertTrue("FileContent objects in written/read database version different.", writtenFileContents.containsAll(readFileContents));		
	}	
}
