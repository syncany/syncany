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
package org.syncany.tests.scenarios;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class IgnoredFileScenarioTest {
	
	@Test
	public void testIgnoredFileBasic() throws Exception {
		// Scenario: A ignores a file, creates it then ups, B should not have the file
		
		// Setup 
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		//Create ignore file and reload it
		File syncanyIgnore = clientA.getLocalFile(Config.FILE_IGNORE);
		TestFileUtil.createFileWithContent(syncanyIgnore, "ignoredfile.txt");
		clientA.getConfig().getIgnoredFiles().loadPatterns();
		
		// A new/up
		clientA.createNewFile("ignoredfile.txt");	
		clientA.createNewFile("nonignoredfile.txt");
		clientA.up();
		
		clientB.down();

		// The ignored file should not exist at B
		assertTrue(clientA.getLocalFile("ignoredfile.txt").exists());
		assertTrue(clientA.getLocalFile("nonignoredfile.txt").exists());
		assertFalse(clientB.getLocalFile("ignoredfile.txt").exists());
		assertTrue(clientB.getLocalFile("nonignoredfile.txt").exists());
		
		//Delete ignore file and reload patterns
		TestFileUtil.deleteFile(syncanyIgnore);
		clientA.getConfig().getIgnoredFiles().loadPatterns();
		clientA.up();
		
		clientB.down();
		
		// All files should be synced
		assertTrue(clientA.getLocalFile("ignoredfile.txt").exists());
		assertTrue(clientA.getLocalFile("nonignoredfile.txt").exists());
		assertTrue(clientB.getLocalFile("ignoredfile.txt").exists());
		assertTrue(clientB.getLocalFile("nonignoredfile.txt").exists());
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testIgnoredFileRegex() throws Exception {
		// Scenario: A ignores files with a regular expression, creates it then ups, B should not have the file

		// Setup 
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		//Create ignore file and reload it
		File syncanyIgnore = clientA.getLocalFile(Config.FILE_IGNORE);
		TestFileUtil.createFileWithContent(syncanyIgnore, "regex:.*.bak");
		clientA.getConfig().getIgnoredFiles().loadPatterns();
		
		// A new/up
		clientA.createNewFile("ignoredfile.bak");	
		clientA.up();
		
		clientB.down();

		// The ignored file should not exist at B
		assertTrue(clientA.getLocalFile("ignoredfile.bak").exists());
		assertFalse(clientB.getLocalFile("ignoredfile.bak").exists());
		
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testIgnoredDirectory() throws Exception {
		// Scenario: A ignores files with a regular expression, creates it then ups, B should not have the file

		// Setup 
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		//Create ignore file and reload it
		File syncanyIgnore = clientA.getLocalFile(Config.FILE_IGNORE);
		TestFileUtil.createFileWithContent(syncanyIgnore, "builds");
		clientA.getConfig().getIgnoredFiles().loadPatterns();
		
		// A new/up
		clientA.createNewFolder("builds");
		clientA.createNewFile("builds/test.txt");
		clientA.up();
		
		clientB.down();

		// The ignored file should not exist at B
		assertTrue(clientA.getLocalFile("builds/test.txt").exists());
		assertFalse(clientB.getLocalFile("builds/test.txt").exists());
		
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		TestFileUtil.deleteDirectory(tempDir);
	}
}
