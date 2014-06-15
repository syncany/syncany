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

import static org.junit.Assert.*;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class MoveFolderWithQuotesOnWindowsScenario {	
	@Test
	public void testMoveFolderWithQuotes_issue124() throws Exception {
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// Create folder with file without quotes
		clientA.createNewFolder("folder_without_quotes");
		clientA.createNewFile("folder_without_quotes/file");
		clientA.up();
		
		// Sync to B
		clientB.down();
		
		// Now we should move to a folder with quotes, e.g.  folder "with" quotes
		// To make this work on Windows, we first move the folder and then modify
		// the database file on the remote storage to mock it
		
		clientA.moveFile("folder_without_quotes", "folder_with_quotes");
		clientA.up();
		
		for (File databaseFile : new File(testConnection.getRepositoryPath()+"/databases/").listFiles()) {
			String databaseFileA2Content = IOUtils.toString(new FileInputStream(databaseFile), "UTF-8");
			String databaseFileA2NewContent = databaseFileA2Content.replaceAll("folder_with_quotes", "folder &quot;with&quot; quotes");

			IOUtils.write(databaseFileA2NewContent, new FileOutputStream(databaseFile));
		}
		
		clientB.down(); // << In issue 124, this failed because we tried to move the inner "file" to the folder with quotes
		assertTrue(clientB.getLocalFile("folder with quotes (filename conflict)").exists());
		assertTrue(clientB.getLocalFile("folder with quotes (filename conflict)/file").exists());
			
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
