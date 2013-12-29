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
package org.syncany.tests.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.internal.ArrayComparisonFailure;
import org.syncany.chunk.Transformer;
import org.syncany.database.ChunkEntry;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.FileVersionComparator.FileChange;
import org.syncany.database.FileVersionComparator.FileProperties;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.util.CollectionUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class TestAssertUtil {
	private static final Logger logger = Logger.getLogger(TestAssertUtil.class.getSimpleName());
	
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

	public static void assertFileListEqualsExcludeLockedAndNoRead(File expectedFilesRoot, File actualFilesRoot) throws ArrayComparisonFailure, Exception {
		Map<String, File> expectedFiles = TestFileUtil.getLocalFilesExcludeLockedAndNoRead(expectedFilesRoot);
		Map<String, File> actualFiles = TestFileUtil.getLocalFilesExcludeLockedAndNoRead(actualFilesRoot);
		
		assertFileListEquals("File list does not match", expectedFiles, actualFiles);
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
	
	public static void assertConflictingFileExists(String originalFile, Map<String, File> actualFiles) {
		String fileNameWithoutExtention = FileUtil.getBasename(originalFile);
		Pattern conflictFilePattern = Pattern.compile(fileNameWithoutExtention + ".*conflicted.*");
		
		boolean conflictingFileFound = false;

		for (Map.Entry<String, File> actualFileEntry : actualFiles.entrySet()) {
			File actualFile = actualFileEntry.getValue();
			Matcher matcher = conflictFilePattern.matcher(actualFile.getName());

			if(matcher.matches()) {
				conflictingFileFound = true;
				break;
			}
		}		
		assertTrue("Pattern " + conflictFilePattern + " could not be found.",conflictingFileFound);
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
		
		Path root = Paths.get(actualFile.getAbsolutePath()).getRoot();
		FileVersionComparator fileVersionComparator = new FileVersionComparator(root.toFile(), "SHA1");
		
		if (!FileUtil.exists(expectedFile)) {
			fail(message+": Expected file "+expectedFile+" does not exist.");
		}
		
		if (!FileUtil.exists(actualFile)) {
			fail(message+": Actual file "+actualFile+" does not exist.");
		}

		if (FileUtil.isSymlink(actualFile) && FileUtil.isSymlink(expectedFile)) {
			return;
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
		
		FileProperties actualFileProperties = fileVersionComparator.captureFileProperties(actualFile, null, true);
		FileProperties expectedFileProperties = fileVersionComparator.captureFileProperties(expectedFile, null, true);
		
		FileVersionComparison fileVersionComparison = fileVersionComparator.compare(expectedFileProperties, actualFileProperties, true);
		
		if (!CollectionUtil.containsOnly(fileVersionComparison.getFileChanges(), FileChange.CHANGED_PATH)) {
			fail(message+": Actual file differs from expected file: "+fileVersionComparison.getFileChanges());
		}		
	}
	
	public static void assertDatabaseFileEquals(File expectedDatabaseFile, File actualDatabaseFile, Transformer transformer) throws IOException {		
		MemoryDatabase expectedDatabase = TestDatabaseUtil.readDatabaseFileFromDisk(expectedDatabaseFile, transformer);
		MemoryDatabase actualDatabase = TestDatabaseUtil.readDatabaseFileFromDisk(actualDatabaseFile, transformer);
		
		assertDatabaseEquals(expectedDatabase, actualDatabase);
	}

	public static void assertDatabaseEquals(MemoryDatabase expectedDatabase, MemoryDatabase actualDatabase) {
		logger.log(Level.INFO, "--");
		logger.log(Level.INFO, "Now comparing two databases.");
		logger.log(Level.INFO, "DON'T WORRY. This can take a long time or even overload the heap space.");
		
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

		logger.log(Level.INFO, "End of comparing databases");		
		logger.log(Level.INFO, "--");		
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
