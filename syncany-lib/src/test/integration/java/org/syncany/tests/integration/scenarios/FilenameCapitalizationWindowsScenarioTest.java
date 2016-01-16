/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.tests.integration.scenarios;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

import static org.junit.Assert.assertEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

public class FilenameCapitalizationWindowsScenarioTest {
	// TODO [medium] Windows: LARGE/small capitalization --> Dropbox makes a file "name (Case Conflict 1)"; define expected/desired behavior

	@Test
	public void testFilenameCapitalizationWindows() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run
		clientA.createNewFile("FILENAME-FOR-WINDOWS");
		clientA.createNewFile("filename-for-windows");
		clientA.createNewFile("Filename-For-Windows");
		clientA.upWithForceChecksum();
		assertEquals("There should be three files.", 3, clientA.getLocalFilesExcludeLockedAndNoRead().size());

		clientB.down();
		assertEquals("There should be three files.", 3, clientB.getLocalFilesExcludeLockedAndNoRead().size());
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Test
	public void testFilenameNonASCII() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run
		clientA.createNewFile("Exposé"); // Non-ASCII
		clientA.createNewFile("這是一個測試"); // Non-ASCII
		clientA.createNewFile("One&Two"); // & is not allowed in XML
		clientA.upWithForceChecksum();
		assertEquals("There should be three files.", 3, clientA.getLocalFilesExcludeLockedAndNoRead().size());

		clientB.down();
		assertEquals("There should be three files.", 3, clientB.getLocalFilesExcludeLockedAndNoRead().size());
		assertFileListEquals(clientA.getLocalFilesExcludeLockedAndNoRead(), clientB.getLocalFilesExcludeLockedAndNoRead());
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

}
