package org.syncany.tests.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.internal.ArrayComparisonFailure;
import org.syncany.chunk.Transformer;
import org.syncany.database.ChunkEntry;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.util.StringUtil;

public class TestAssertUtil {
	public static void assertCollectionEquals(String message, Collection<? extends Object> expected, Collection<? extends Object> actual) {
		assertEquals(message+": Different amount of objects.", expected.size(), actual.size());
		
		Iterator<? extends Object> expectedIt = expected.iterator();
		Iterator<? extends Object> actualIt = actual.iterator();
		
		int i = 0;
		while (expectedIt.hasNext()) {			
			Object expectedObj = expectedIt.next();			
			Object actualObj = actualIt.next();
			
			assertEquals(message+": actual["+i+"] differs from expected["+i+"]: ", expectedObj, actualObj);
			i++;
		}
	}

	public static void assertFileListEquals(Map<String, File> expectedFiles, Map<String, File> actualFiles) throws ArrayComparisonFailure, Exception {
		assertFileListEquals("File list does not match", expectedFiles, actualFiles);
	}
	
	public static void assertFileListEquals(String message, Map<String, File> expectedFiles, Map<String, File> actualFiles) throws ArrayComparisonFailure, Exception {
		assertCollectionEquals("Actual file list ("+actualFiles.size()+" entries) differs from expected file list ("+expectedFiles.size()+" entries)", expectedFiles.keySet(), actualFiles.keySet());

		for (Map.Entry<String, File> expectedFileEntry : expectedFiles.entrySet()) {
			File expectedFile = expectedFileEntry.getValue();
			File actualFile = actualFiles.remove(expectedFileEntry.getKey());
			
			assertFileEquals(message+": File not equal", expectedFile, actualFile);
		}		
	}
	
	public static void assertFileEquals(File expectedFile, File actualFile) throws ArrayComparisonFailure, Exception {
		assertFileEquals("Files are not equal", expectedFile, actualFile);
	}
	
	public static void assertFileEquals(String message, File expectedFile, File actualFile) throws ArrayComparisonFailure, Exception {
		if (expectedFile == null && actualFile == null) {
			return;
		}
		
		assertNotNull(message+": Actual file is "+actualFile+", expected file is null.", expectedFile);
		assertNotNull(message+": Expected file is "+expectedFile+", actual file is null.", actualFile);

		if (!expectedFile.exists()) {
			fail(message+": Expected file "+expectedFile+" does not exist.");
		}
		
		if (!actualFile.exists()) {
			fail(message+": Actual file "+actualFile+" does not exist.");
		}

		if (actualFile.isDirectory() != expectedFile.isDirectory()) {
			fail(message+" Comparing a directory with a file (actual is dir = "+actualFile.isDirectory()+", expected is dir = "+expectedFile.isDirectory()+")");
		}
		
		if (actualFile.isDirectory() && expectedFile.isDirectory()) {
			return;
		}
		
		if (actualFile.length() != expectedFile.length()) {
			fail(message+": Actual file size ("+actualFile+" = "+actualFile.length()+") does not match expected file size ("+expectedFile+" = "+expectedFile.length()+")");
		}
		
		byte[] expectedFileChecksum = TestFileUtil.createChecksum(expectedFile);
		byte[] actualFileChecksum = TestFileUtil.createChecksum(actualFile);
		
		assertArrayEquals(message+": Actual file checksum ("+StringUtil.toHex(actualFileChecksum)+") and expected file checksum ("+StringUtil.toHex(expectedFileChecksum)+") do not match.", expectedFileChecksum, actualFileChecksum);		
	}
	
	public static void assertDatabaseFileEquals(File expectedDatabaseFile, File actualDatabaseFile, Transformer transformer) throws IOException {
		Database expectedDatabase = TestDatabaseUtil.readDatabaseFileFromDisk(expectedDatabaseFile, transformer);
		Database actualDatabase = TestDatabaseUtil.readDatabaseFileFromDisk(actualDatabaseFile, transformer);
		
		assertDatabaseEquals(expectedDatabase, actualDatabase);
	}

	public static void assertDatabaseEquals(Database expectedDatabase, Database actualDatabase) {
		List<DatabaseVersion> writtenDatabaseVersions = expectedDatabase.getDatabaseVersions();
		List<DatabaseVersion> readDatabaseVersions = actualDatabase.getDatabaseVersions();
		
		assertEquals("Different number of database versions.", writtenDatabaseVersions.size(), readDatabaseVersions.size());
			
		for (DatabaseVersion writtenDatabaseVersion : writtenDatabaseVersions) {
			DatabaseVersion readDatabaseVersion = null;
			
			for (DatabaseVersion aReadDatabaseVersion : readDatabaseVersions) {
				if (aReadDatabaseVersion.equals(writtenDatabaseVersion)) {
					readDatabaseVersion = aReadDatabaseVersion;
					break;
				}
			}
			
			assertNotNull("Database version "+writtenDatabaseVersion+" does not exist in read database.", readDatabaseVersion);
			
			assertDatabaseVersionEquals(writtenDatabaseVersion, readDatabaseVersion);
		}
	}	
	
	public static void assertDatabaseVersionEquals(DatabaseVersion expectedDatabaseVersion, DatabaseVersion actualDatabaseVersion) {
		assertVectorClockEquals(expectedDatabaseVersion.getVectorClock(), actualDatabaseVersion.getVectorClock());
		compareDatabaseVersionChunks(expectedDatabaseVersion.getChunks(), actualDatabaseVersion.getChunks());
		compareDatabaseVersionMultiChunks(expectedDatabaseVersion.getMultiChunks(), actualDatabaseVersion.getMultiChunks());
		compareDatabaseVersionFileContents(expectedDatabaseVersion.getFileContents(), actualDatabaseVersion.getFileContents());
		compareDatabaseVersionFileHistories(expectedDatabaseVersion.getFileHistories(), actualDatabaseVersion.getFileHistories());	
	}		

	public static void assertVectorClockEquals(VectorClock expectedVectorClock, VectorClock actualVectorClock) {
		assertEquals("Vector clocks differ.", expectedVectorClock, actualVectorClock);		
	}

	private static void compareDatabaseVersionChunks(Collection<ChunkEntry> writtenChunks, Collection<ChunkEntry> readChunks) {	
		assertEquals("Different amount of Chunk objects.", writtenChunks.size(), readChunks.size());
		assertTrue("Chunk objects in written/read database version different.", writtenChunks.containsAll(readChunks));
		//assertCollectionEquals("Chunk objects in written/read database version different.", writtenChunks, readChunks);
	}
	
	private static void compareDatabaseVersionMultiChunks(Collection<MultiChunkEntry> writtenMultiChunks, Collection<MultiChunkEntry> readMultiChunks) {
		assertEquals("Different amount of MultiChunk objects.", writtenMultiChunks.size(), readMultiChunks.size());
		assertTrue("MultiChunk objects in written/read database version different.", writtenMultiChunks.containsAll(readMultiChunks));
		//assertCollectionEquals("MultiChunk objects in written/read database version different.", writtenMultiChunks, readMultiChunks);
	}	
	
	private static void compareDatabaseVersionFileContents(Collection<FileContent> writtenFileContents, Collection<FileContent> readFileContents) {
		assertEquals("Different amount of FileContent objects.", writtenFileContents.size(), readFileContents.size());
		assertTrue("FileContent objects in written/read database version different.", writtenFileContents.containsAll(readFileContents));
		//assertCollectionEquals("FileContent objects in written/read database version different.", writtenFileContents, readFileContents);
	}	

	private static void compareDatabaseVersionFileHistories(Collection<PartialFileHistory> writtenFileHistories, Collection<PartialFileHistory> readFileHistories) {
		assertTrue("FileHistory objects in written/read database version different.", writtenFileHistories.containsAll(readFileHistories));
	}		
}
