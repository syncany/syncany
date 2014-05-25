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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlDatabaseUtil;

/**
 * Tests the {@link FileVersionSqlDao}
 * <p>
 * Note: {@link FileVersionSqlDao#writeFileVersions(Connection, FileHistoryId, long, java.util.Collection) is
 * tested in combination with the rest of the database write functioins. 
 */
public class FileVersionDaoTest {	
	@Test
	public void testFileVersionGetCurrentFileTree() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();
				
		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "test.insert.set2.sql");

		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);				
		Map<String, FileVersion> currentFileTree = fileVersionDao.getCurrentFileTree();
		
		// Test
		assertEquals(50, currentFileTree.size());
		
		assertNotNull(currentFileTree.get("file1"));
		assertNotNull(currentFileTree.get("file1").getChecksum());
		assertEquals("fe83f217d464f6fdfa5b2b1f87fe3a1a47371196", currentFileTree.get("file1").getChecksum().toString());
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testFileVersionGetFileTreeAtDate() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();
				
		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");

		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);				
		
		Map<String, FileVersion> fileTreeWithOneDatabaseVersion = fileVersionDao.getFileTreeAtDate(toDate("2014-01-01 18:00:00.000+0100"));
		Map<String, FileVersion> fileTreeWithTwoDatabaseVersions = fileVersionDao.getFileTreeAtDate(toDate("2014-01-02 18:00:00.000+0100"));
		Map<String, FileVersion> fileTreeWithThreeDatabaseVersions = fileVersionDao.getFileTreeAtDate(toDate("2014-01-03 18:00:00.000+0100"));
		Map<String, FileVersion> fileTreeWithFourDatabaseVersions = fileVersionDao.getFileTreeAtDate(toDate("2014-01-04 18:00:00.000+0100"));
		Map<String, FileVersion> fileTreeWithFiveDatabaseVersions = fileVersionDao.getFileTreeAtDate(toDate("2014-01-05 18:00:00.000+0100"));
		
		// Tests
		
		// 1. First database version
		assertEquals(1, fileTreeWithOneDatabaseVersion.size()); // Added "file1"
		assertNotNull(fileTreeWithOneDatabaseVersion.get("file1"));
		assertNotNull(fileTreeWithOneDatabaseVersion.get("file1").getChecksum());
		assertEquals("fe83f217d464f6fdfa5b2b1f87fe3a1a47371196", fileTreeWithOneDatabaseVersion.get("file1").getChecksum().toString());

		// 2. Second database version
		assertEquals(2, fileTreeWithTwoDatabaseVersions.size()); // Added "file2"
		assertNotNull(fileTreeWithTwoDatabaseVersions.get("file1"));
		assertNotNull(fileTreeWithTwoDatabaseVersions.get("file1").getChecksum());
		assertNotNull(fileTreeWithTwoDatabaseVersions.get("file2"));
		assertNotNull(fileTreeWithTwoDatabaseVersions.get("file2").getChecksum());
		assertEquals("fe83f217d464f6fdfa5b2b1f87fe3a1a47371196", fileTreeWithTwoDatabaseVersions.get("file1").getChecksum().toString());
		assertEquals("bf8b4530d8d246dd74ac53a13471bba17941dff7", fileTreeWithTwoDatabaseVersions.get("file2").getChecksum().toString());
		
		// 3. Third database version
		assertEquals(3, fileTreeWithThreeDatabaseVersions.size()); // Added "file3"
		assertNotNull(fileTreeWithThreeDatabaseVersions.get("file1"));
		assertNotNull(fileTreeWithThreeDatabaseVersions.get("file1").getChecksum());
		assertNotNull(fileTreeWithThreeDatabaseVersions.get("file2"));
		assertNotNull(fileTreeWithThreeDatabaseVersions.get("file2").getChecksum());
		assertNotNull(fileTreeWithThreeDatabaseVersions.get("file3"));
		assertNotNull(fileTreeWithThreeDatabaseVersions.get("file3").getChecksum());
		assertEquals("fe83f217d464f6fdfa5b2b1f87fe3a1a47371196", fileTreeWithThreeDatabaseVersions.get("file1").getChecksum().toString());
		assertEquals("bf8b4530d8d246dd74ac53a13471bba17941dff7", fileTreeWithThreeDatabaseVersions.get("file2").getChecksum().toString());
		assertEquals("8ce24fc0ea8e685eb23bf6346713ad9fef920425", fileTreeWithThreeDatabaseVersions.get("file3").getChecksum().toString());
				
		// 4. Fourth database version
		assertEquals(2, fileTreeWithFourDatabaseVersions.size()); // Deleted "file1"
		assertNull(fileTreeWithFourDatabaseVersions.get("file1"));
		assertNotNull(fileTreeWithFourDatabaseVersions.get("file2"));
		assertNotNull(fileTreeWithFourDatabaseVersions.get("file2").getChecksum());
		assertNotNull(fileTreeWithFourDatabaseVersions.get("file3"));
		assertNotNull(fileTreeWithFourDatabaseVersions.get("file3").getChecksum());
		assertEquals("bf8b4530d8d246dd74ac53a13471bba17941dff7", fileTreeWithFourDatabaseVersions.get("file2").getChecksum().toString());
		assertEquals("8ce24fc0ea8e685eb23bf6346713ad9fef920425", fileTreeWithFourDatabaseVersions.get("file3").getChecksum().toString());
		
		// 5. Fifth database version
		assertEquals(3, fileTreeWithFiveDatabaseVersions.size()); // Added new "file1"
		assertNotNull(fileTreeWithFiveDatabaseVersions.get("file1"));
		assertNotNull(fileTreeWithFiveDatabaseVersions.get("file1").getChecksum());
		assertNotNull(fileTreeWithFiveDatabaseVersions.get("file2"));
		assertNotNull(fileTreeWithFiveDatabaseVersions.get("file2").getChecksum());
		assertNotNull(fileTreeWithFiveDatabaseVersions.get("file3"));
		assertNotNull(fileTreeWithFiveDatabaseVersions.get("file3").getChecksum());
		assertEquals("ffffffffffffffffffffffffffffffffffffffff", fileTreeWithFiveDatabaseVersions.get("file1").getChecksum().toString());
		assertEquals("bf8b4530d8d246dd74ac53a13471bba17941dff7", fileTreeWithFiveDatabaseVersions.get("file2").getChecksum().toString());
		assertEquals("8ce24fc0ea8e685eb23bf6346713ad9fef920425", fileTreeWithFiveDatabaseVersions.get("file3").getChecksum().toString());
				
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	@Test
	public void testFileVersionGetByPath() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();
				
		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "test.insert.set1.sql");

		FileVersionSqlDao fileVersionDao = new FileVersionSqlDao(databaseConnection);				
		
		FileVersion file1ByPath = fileVersionDao.getFileVersionByPath("file1");
		FileVersion file2ByPath = fileVersionDao.getFileVersionByPath("file2");
		FileVersion file3ByPath = fileVersionDao.getFileVersionByPath("file3");
		FileVersion file4ByPath = fileVersionDao.getFileVersionByPath("file4");
		
		// Test
		
		// - By Path: File 1
		assertNotNull(file1ByPath);
		assertEquals(1, (long) file1ByPath.getVersion());
		assertFalse("rwxrw-r--".equals(file1ByPath.getPosixPermissions()));
		assertNotNull(file1ByPath.getChecksum());
		assertEquals("ffffffffffffffffffffffffffffffffffffffff", file1ByPath.getChecksum().toString());
		
		// - By Path: File 2
		assertNotNull(file2ByPath);
		assertEquals(1, (long) file2ByPath.getVersion());
		assertNotNull(file2ByPath.getChecksum());
		assertEquals("bf8b4530d8d246dd74ac53a13471bba17941dff7", file2ByPath.getChecksum().toString());		
		assertEquals(toDate("2014-01-02 16:26:09.000+0100"), file2ByPath.getLastModified());
		assertEquals(toDate("2014-01-02 16:26:09.000+0100"), file2ByPath.getUpdated());
		assertEquals("rw-r--r--", file2ByPath.getPosixPermissions());
		assertNull(file2ByPath.getDosAttributes());
		
		// - By Path: File 3
		assertNotNull(file3ByPath);
		assertEquals(1, (long) file3ByPath.getVersion());
		assertNotNull(file3ByPath.getChecksum());
		assertEquals("8ce24fc0ea8e685eb23bf6346713ad9fef920425", file3ByPath.getChecksum().toString());
		assertEquals(toDate("2014-01-03 16:26:09.000+0100"), file3ByPath.getLastModified());
		assertEquals(toDate("2014-01-03 16:26:09.000+0100"), file3ByPath.getUpdated());
		assertEquals("rw-r--r--", file3ByPath.getPosixPermissions());
		assertNull(file3ByPath.getDosAttributes());		
		
		// - By Path: File 4
		assertNull(file4ByPath);
		
		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}
	
	private Date toDate(String dateString) throws ParseException {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").parse(dateString);
	}
}
