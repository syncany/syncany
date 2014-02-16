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
import org.syncany.database.FileContent;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.dao.FileContentSqlDao;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDatabaseUtil;
import org.syncany.tests.util.TestSqlDatabaseUtil;

public class FileContentDaoTest {
	@Test
	public void testGetFileContentsByVectorClock() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");

		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);

		Map<FileChecksum, FileContent> fileContentsA1 = fileContentDao.getFileContents(TestDatabaseUtil.createVectorClock("A1"));
		Map<FileChecksum, FileContent> fileContentsA2 = fileContentDao.getFileContents(TestDatabaseUtil.createVectorClock("A2"));
		Map<FileChecksum, FileContent> fileContentsA3 = fileContentDao.getFileContents(TestDatabaseUtil.createVectorClock("A3"));
		Map<FileChecksum, FileContent> fileContentsA4 = fileContentDao.getFileContents(TestDatabaseUtil.createVectorClock("A4"));
		Map<FileChecksum, FileContent> fileContentsA5 = fileContentDao.getFileContents(TestDatabaseUtil.createVectorClock("A5"));
		Map<FileChecksum, FileContent> fileContentsA6 = fileContentDao.getFileContents(TestDatabaseUtil.createVectorClock("A6"));
		Map<FileChecksum, FileContent> fileContentsA6B1 = fileContentDao.getFileContents(TestDatabaseUtil.createVectorClock("A6,B1"));
		Map<FileChecksum, FileContent> fileContentsA7B1 = fileContentDao.getFileContents(TestDatabaseUtil.createVectorClock("A7,B1"));
		Map<FileChecksum, FileContent> fileContentsInvalid1 = fileContentDao.getFileContents(TestDatabaseUtil.createVectorClock("Invalid1"));

		// Test
		assertNotNull(fileContentsA1);
		assertEquals(0, fileContentsA1.size());
		
		assertNotNull(fileContentsA2);
		assertEquals(0, fileContentsA2.size());
		
		assertNotNull(fileContentsA3);
		assertEquals(0, fileContentsA3.size());
		
		assertNotNull(fileContentsA4);
		assertEquals(0, fileContentsA4.size());
		
		assertNotNull(fileContentsA5);
		assertEquals(1, fileContentsA5.size());
		assertNotNull(fileContentsA5.get(FileChecksum.parseFileChecksum("0fefb345b62b6c0b0e5212158a9aa7c1eeec2ca6")));
		assertEquals(12, fileContentsA5.get(FileChecksum.parseFileChecksum("0fefb345b62b6c0b0e5212158a9aa7c1eeec2ca6")).getSize());

		assertNotNull(fileContentsA6);
		assertEquals(1, fileContentsA6.size());
		assertNotNull(fileContentsA6.get(FileChecksum.parseFileChecksum("24a39e00d6156804e27f7c0987d00903da8e6682")));
		assertEquals(508, fileContentsA6.get(FileChecksum.parseFileChecksum("24a39e00d6156804e27f7c0987d00903da8e6682")).getSize());

		assertNotNull(fileContentsA6B1);
		assertEquals(1, fileContentsA6B1.size());
		assertNotNull(fileContentsA6B1.get(FileChecksum.parseFileChecksum("7666fd3b860c9d7588d9ca1807eebdf8cfaa8be3")));
		assertEquals(2029, fileContentsA6B1.get(FileChecksum.parseFileChecksum("7666fd3b860c9d7588d9ca1807eebdf8cfaa8be3")).getSize());

		assertNotNull(fileContentsA7B1);
		assertEquals(1, fileContentsA7B1.size());
		assertNotNull(fileContentsA7B1.get(FileChecksum.parseFileChecksum("eba69a8e359ce3258520138a50ed9860127ab6e0")));
		assertEquals(512, fileContentsA7B1.get(FileChecksum.parseFileChecksum("eba69a8e359ce3258520138a50ed9860127ab6e0")).getSize());

		assertNotNull(fileContentsInvalid1);
		assertEquals(0, fileContentsInvalid1.size());

		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}

	@Test
	public void testGetFileContentByChecksum() throws Exception {
		// Setup
		Config testConfig = TestConfigUtil.createTestLocalConfig();
		Connection databaseConnection = testConfig.createDatabaseConnection();

		// Run
		TestSqlDatabaseUtil.runSqlFromResource(databaseConnection, "/sql/test.insert.set3.sql");

		FileContentSqlDao fileContentDao = new FileContentSqlDao(databaseConnection);

		FileContent fileContent1WithChunks = fileContentDao.getFileContent(FileChecksum.parseFileChecksum("eba69a8e359ce3258520138a50ed9860127ab6e0"), true);
		FileContent fileContent1WithoutChunks = fileContentDao.getFileContent(FileChecksum.parseFileChecksum("eba69a8e359ce3258520138a50ed9860127ab6e0"), false);

		FileContent fileContent2WithChunks = fileContentDao.getFileContent(FileChecksum.parseFileChecksum("254416e71ae50431fc6ced6751075b3366db7cc8"), true);
		FileContent fileContent2WithoutChunks = fileContentDao.getFileContent(FileChecksum.parseFileChecksum("254416e71ae50431fc6ced6751075b3366db7cc8"), false);

		FileContent fileContentNonExistingWithChunks = fileContentDao.getFileContent(FileChecksum.parseFileChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"), true);
		FileContent fileContentNonExistingWithoutChunks = fileContentDao.getFileContent(FileChecksum.parseFileChecksum("beefbeefbeefbeefbeefbeefbeefbeefbeefbeef"), false);
		
		FileContent fileContentNullChecksum = fileContentDao.getFileContent(null, false);
		
		// Test
		assertNotNull(fileContent1WithChunks);
		assertEquals(1, fileContent1WithChunks.getChunks().size());
		assertEquals("eba69a8e359ce3258520138a50ed9860127ab6e0", fileContent1WithChunks.getChecksum().toString());
		assertEquals("eba69a8e359ce3258520138a50ed9860127ab6e0", fileContent1WithChunks.getChunks().get(0).toString());

		assertNotNull(fileContent1WithoutChunks);
		assertEquals("eba69a8e359ce3258520138a50ed9860127ab6e0", fileContent1WithoutChunks.getChecksum().toString());
		assertEquals(0, fileContent1WithoutChunks.getChunks().size());

		assertNotNull(fileContent2WithChunks);
		assertEquals(3, fileContent2WithChunks.getChunks().size());
		assertEquals("254416e71ae50431fc6ced6751075b3366db7cc8", fileContent2WithChunks.getChecksum().toString());
		assertEquals("b310c0eedcd03238888c6abb3e3398633139ecc5", fileContent2WithChunks.getChunks().get(0).toString());
		assertEquals("8ed8d50a6e9da3197bd665bc3a1f229ebcde9b42", fileContent2WithChunks.getChunks().get(1).toString());
		assertEquals("ab85720d3f31bd08ca1cd25dcd8a490e5f00783b", fileContent2WithChunks.getChunks().get(2).toString());

		assertNotNull(fileContent2WithoutChunks);
		assertEquals("254416e71ae50431fc6ced6751075b3366db7cc8", fileContent2WithoutChunks.getChecksum().toString());
		assertEquals(0, fileContent2WithoutChunks.getChunks().size());

		assertNull(fileContentNonExistingWithChunks);
		assertNull(fileContentNonExistingWithoutChunks);
		
		assertNull(fileContentNullChecksum);

		// Tear down
		databaseConnection.close();
		TestConfigUtil.deleteTestLocalConfigAndData(testConfig);
	}

}
