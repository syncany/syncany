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
package org.syncany.tests.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.FileVersionComparator.FileChange;
import org.syncany.database.FileVersionComparator.FileProperties;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.util.CollectionUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

import com.google.common.collect.Lists;

public class TestAssertUtil {
	private static final Logger logger = Logger.getLogger(TestAssertUtil.class.getSimpleName());

	public static void assertCollectionEquals(String message, Collection<? extends Object> expected, Collection<? extends Object> actual) {
		assertEquals(message + ": Different amount of objects.", expected.size(), actual.size());

		int i = 0;
		for (Iterator<? extends Object> expectedIt = expected.iterator(), actualIt = actual.iterator(); expectedIt.hasNext() && actualIt.hasNext();) {
			Object expectedObj = expectedIt.next();
			Object actualObj = actualIt.next();

			assertEquals(message + ": actual[" + i + "] differs from expected[" + i + "]: ", expectedObj, actualObj);
			i++;
		}
	}

	public static void assertFileListEqualsExcludeLockedAndNoRead(File expectedFilesRoot, File actualFilesRoot) throws ArrayComparisonFailure,
			Exception {
		Map<String, File> expectedFiles = TestFileUtil.getLocalFilesExcludeLockedAndNoRead(expectedFilesRoot);
		Map<String, File> actualFiles = TestFileUtil.getLocalFilesExcludeLockedAndNoRead(actualFilesRoot);

		assertFileListEquals("File list does not match", expectedFiles, actualFiles);
	}

	public static void assertFileListEquals(Map<String, File> expectedFiles, Map<String, File> actualFiles) throws ArrayComparisonFailure, Exception {
		assertFileListEquals("File list does not match", expectedFiles, actualFiles);
	}

	public static void assertFileListEquals(String message, Map<String, File> expectedFiles, Map<String, File> actualFiles)
			throws ArrayComparisonFailure, Exception {
		assertCollectionEquals("Actual file list (" + actualFiles.size() + " entries) differs from expected file list (" + expectedFiles.size()
				+ " entries)", expectedFiles.keySet(), actualFiles.keySet());

		for (Map.Entry<String, File> expectedFileEntry : expectedFiles.entrySet()) {
			File expectedFile = expectedFileEntry.getValue();
			File actualFile = actualFiles.remove(expectedFileEntry.getKey());

			assertFileEquals(expectedFile, actualFile);
		}
	}

	public static void assertConflictingFileExists(String originalFile, Map<String, File> actualFiles) {
		boolean conflictingFileExists = conflictingFileExists(originalFile, actualFiles);
		assertTrue("Conflicting file for '" + originalFile + "' does NOT exist, but it should exist.", conflictingFileExists);
	}

	public static void assertConflictingFileNotExists(String originalFile, Map<String, File> actualFiles) {
		boolean conflictingFileExists = conflictingFileExists(originalFile, actualFiles);
		assertFalse("Conflicting file for '" + originalFile + "' does exist, but it should NOT exist.", conflictingFileExists);
	}

	private static boolean conflictingFileExists(String originalFile, Map<String, File> actualFiles) {
		String fileNameWithoutExtention = TestFileUtil.getBasename(originalFile);
		Pattern conflictFilePattern = Pattern.compile(fileNameWithoutExtention + ".*conflicted.*");

		boolean conflictingFileFound = false;

		for (Map.Entry<String, File> actualFileEntry : actualFiles.entrySet()) {
			File actualFile = actualFileEntry.getValue();
			Matcher matcher = conflictFilePattern.matcher(actualFile.getName());

			if (matcher.matches()) {
				conflictingFileFound = true;
				break;
			}
		}

		return conflictingFileFound;
	}

	public static void assertFileEquals(File expectedFile, File actualFile) throws ArrayComparisonFailure, Exception {
		assertFileEquals(expectedFile, actualFile, new FileChange[] { FileChange.CHANGED_PATH });
	}

	public static void assertFileEquals(File expectedFile, File actualFile, FileChange... allowedChanges) throws ArrayComparisonFailure, Exception {
		if (expectedFile == null && actualFile == null) {
			return;
		}

		assertNotNull("Files are not equal: Actual file is " + actualFile + ", expected file is null.", expectedFile);
		assertNotNull("Files are not equal: Expected file is " + expectedFile + ", actual file is null.", actualFile);

		Path root = Paths.get(actualFile.getAbsolutePath()).getRoot();
		FileVersionComparator fileVersionComparator = new FileVersionComparator(root.toFile(), "SHA1");

		if (!FileUtil.exists(expectedFile)) {
			fail("Files are not equal: Expected file " + expectedFile + " does not exist.");
		}

		if (!FileUtil.exists(actualFile)) {
			fail("Files are not equal: Actual file " + actualFile + " does not exist.");
		}

		if (FileUtil.isSymlink(actualFile) && FileUtil.isSymlink(expectedFile)) {
			return;
		}

		if (actualFile.isDirectory() != expectedFile.isDirectory()) {
			fail("Files are not equal: Comparing a directory with a file (actual is dir = " + actualFile.isDirectory() + ", expected is dir = "
					+ expectedFile.isDirectory() + ")");
		}

		if (actualFile.isDirectory() && expectedFile.isDirectory()) {
			return;
		}

		if (actualFile.length() != expectedFile.length()) {
			fail("Files are not equal: Actual file size (" + actualFile + " = " + actualFile.length() + ") does not match expected file size ("
					+ expectedFile + " = " + expectedFile.length() + ")");
		}

		byte[] expectedFileChecksum = TestFileUtil.createChecksum(expectedFile);
		byte[] actualFileChecksum = TestFileUtil.createChecksum(actualFile);

		assertArrayEquals("Files are not equal: Actual file checksum (" + StringUtil.toHex(actualFileChecksum) + ") and expected file checksum ("
				+ StringUtil.toHex(expectedFileChecksum) + ") do not match.", expectedFileChecksum, actualFileChecksum);

		FileProperties actualFileProperties = fileVersionComparator.captureFileProperties(actualFile, null, true);
		FileProperties expectedFileProperties = fileVersionComparator.captureFileProperties(expectedFile, null, true);

		FileVersionComparison fileVersionComparison = fileVersionComparator.compare(expectedFileProperties, actualFileProperties, true);

		List<FileChange> allowedChangesList = new ArrayList<FileChange>(Arrays.asList(allowedChanges));
		allowedChangesList.add(FileChange.CHANGED_PATH);

		if (!CollectionUtil.containsOnly(fileVersionComparison.getFileChanges(), allowedChangesList)) {
			fail("Files are not equal: Actual file differs from expected file: " + fileVersionComparison.getFileChanges());
		}
	}

	public static void assertSqlResultEquals(File databaseFile, String sqlQuery, String expectedResultStr) throws SQLException {
		Connection databaseConnection = DatabaseConnectionFactory.createConnection(databaseFile);
		ResultSet resultSet = databaseConnection.prepareStatement(sqlQuery).executeQuery();

		List<String> actualResultStrList = new ArrayList<String>();

		while (resultSet.next()) {
			for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
				actualResultStrList.add(resultSet.getString(i));
			}
		}

		String actualResultStr = StringUtil.join(actualResultStrList, ",");
		assertEquals("SQL query result differs: " + sqlQuery, expectedResultStr, actualResultStr);
	}

	public static void assertSqlDatabaseEquals(File expectedDatabaseFile, File actualDatabaseFile) throws IOException, SQLException {
		// Compare tables + ignore columns
		String[][] compareTablesAndIgnoreColumns = new String[][] {
				new String[] { "chunk", "DATABASEVERSION_ID" },
				new String[] { "databaseversion", "ID" },
				new String[] { "databaseversion_vectorclock", "DATABASEVERSION_ID" },
				new String[] { "filecontent", "DATABASEVERSION_ID" },
				new String[] { "filecontent_chunk" },
				new String[] { "filehistory", "DATABASEVERSION_ID" },
				new String[] { "fileversion", "DATABASEVERSION_ID" },
				// skipped known_databases
				new String[] { "multichunk", "DATABASEVERSION_ID" },
				new String[] { "multichunk_chunk" }
		};

		assertSqlDatabaseTablesEqual(expectedDatabaseFile, actualDatabaseFile, compareTablesAndIgnoreColumns);
	}

	public static void assertSqlDatabaseTablesEqual(File expectedDatabaseFile, File actualDatabaseFile, String[]... compareTablesAndIgnoreColumns)
			throws IOException, SQLException {
		Connection expectedDatabaseConnection = DatabaseConnectionFactory.createConnection(expectedDatabaseFile);
		Connection actualDatabaseConnection = DatabaseConnectionFactory.createConnection(actualDatabaseFile);

		for (String[] tableNameAndIgnoreColumns : compareTablesAndIgnoreColumns) {
			String tableName = tableNameAndIgnoreColumns[0];

			List<String> ignoreColumnNames = Lists.newArrayList(tableNameAndIgnoreColumns);
			ignoreColumnNames.remove(0);

			// Get table's primary keys
			List<String> primaryKeys = new ArrayList<String>();
			ResultSet resultSet = actualDatabaseConnection.getMetaData().getPrimaryKeys(null, null, tableName.toUpperCase());

			while (resultSet.next()) {
				primaryKeys.add(resultSet.getString("COLUMN_NAME"));
			}

			// Get table's columns
			List<String> tableColumns = new ArrayList<String>();
			resultSet = actualDatabaseConnection.getMetaData().getColumns(null, null, tableName.toUpperCase(), null);

			while (resultSet.next()) {
				String columnName = resultSet.getString("COLUMN_NAME");

				if (!ignoreColumnNames.contains(columnName)) {
					tableColumns.add(columnName);
				}
			}

			// Get all entries of both tables, sorted by the primary keys
			String columnNameList = StringUtil.join(tableColumns, ", ");
			String primaryKeysOrderByClause = StringUtil.join(primaryKeys, " asc, ") + " asc";
			String selectQuery = String.format("select %s from %s order by %s", columnNameList, tableName, primaryKeysOrderByClause);

			logger.log(Level.FINE, " Comparing database table: " + selectQuery);

			ResultSet expectedResultSet = expectedDatabaseConnection.prepareStatement(selectQuery).executeQuery();
			ResultSet actualResultSet = actualDatabaseConnection.prepareStatement(selectQuery).executeQuery();

			while (true) {
				boolean expectedNext = expectedResultSet.next();
				boolean actualNext = actualResultSet.next();

				if (expectedNext && !actualNext) {
					fail("Table " + tableName + ": Actual is missing the following row from expected: " + getFormattedColumn(expectedResultSet));
				}
				else if (!expectedNext && actualNext) {
					fail("Table " + tableName + ": Actual has a row that was not expected: " + getFormattedColumn(actualResultSet));
				}
				else if (!expectedNext && !actualNext) {
					break;
				}
				else {
					String expectedFormattedColumn = getFormattedColumn(expectedResultSet);
					String actualFormattedColumn = getFormattedColumn(actualResultSet);

					assertEquals("Table " + tableName + ": Columns of actual and expected differ.", expectedFormattedColumn, actualFormattedColumn);
				}
			}
		}
	}

	private static String getFormattedColumn(ResultSet resultSet) throws SQLException {
		ResultSetMetaData metaData = resultSet.getMetaData();
		List<String> formattedColumnLine = new ArrayList<String>();

		for (int i = 0; i < metaData.getColumnCount(); i++) {
			formattedColumnLine.add(metaData.getColumnName(i + 1) + "=" + resultSet.getString(i + 1));
		}

		return StringUtil.join(formattedColumnLine, ", ");
	}

	public static void assertXmlDatabaseFileEquals(File expectedDatabaseFile, File actualDatabaseFile, Transformer transformer) throws IOException {
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

			assertNotNull("Database version " + writtenDatabaseVersion + " does not exist in read database.", readDatabaseVersion);

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

	private static void compareDatabaseVersionFileHistories(Collection<PartialFileHistory> writtenFileHistories,
			Collection<PartialFileHistory> readFileHistories) {
		assertTrue("FileHistory objects in written/read database version different.", writtenFileHistories.containsAll(readFileHistories));
	}

	public static void assertErrorStackTraceContains(String expectedContains, Exception e) {
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));

		String stackTrace = errors.toString();

		if (stackTrace.contains(expectedContains)) {
			return;
		}

		e.printStackTrace();
		fail("Stack trace expected to contain " + expectedContains);
	}

	public static void assertRegexInLines(String expectedLinePattern, String[] lines) {
		Pattern expectedPattern = Pattern.compile(expectedLinePattern);

		for (String line : lines) {
			if (expectedPattern.matcher(line).find()) {
				return;
			}
		}

		fail("Output does not contain " + expectedLinePattern);
	}
}
