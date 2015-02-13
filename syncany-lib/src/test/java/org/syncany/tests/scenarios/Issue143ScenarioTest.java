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
package org.syncany.tests.scenarios;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.tests.util.TestSqlUtil;

public class Issue143ScenarioTest {
	@Test
	public void testChangeAttributes() throws Exception {
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		java.sql.Connection databaseConnectionA = clientA.getConfig().createDatabaseConnection();

		TestClient clientB = new TestClient("B", testConnection);

		CleanupOperationOptions cleanupOptions = new CleanupOperationOptions();
		cleanupOptions.setMinSecondsBetweenCleanups(0);
		cleanupOptions.setMinKeepSeconds(0);

		// Scenario, see
		// https://github.com/syncany/syncany/issues/143#issuecomment-50964685

		// Run 
		clientA.createNewFile("file1.jpg");
		clientA.upWithForceChecksum();
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));

		TestFileUtil.copyFile(clientA.getLocalFile("file1.jpg"), clientA.getLocalFile("file1 (copy).jpg"));
		clientA.upWithForceChecksum();
		assertEquals("2", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));

		clientA.deleteFile("file1 (copy).jpg");
		clientA.upWithForceChecksum();
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));

		clientA.cleanup(cleanupOptions); // Database versions of deleted file are removed
		assertEquals("1", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));

		TestFileUtil.copyFile(clientA.getLocalFile("file1.jpg"), clientA.getLocalFile("file1 (copy).jpg"));
		clientA.upWithForceChecksum();
		assertEquals("2", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));

		clientA.deleteFile("file1.jpg");
		clientA.upWithForceChecksum();
		assertEquals("3", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));

		clientA.cleanup(cleanupOptions); // Database version of deleted file is removed
		assertEquals("2", TestSqlUtil.runSqlSelect("select count(*) from databaseversion", databaseConnectionA));

		clientB.down(); // <<<< This creates the exception in #143
						// integrity constraint violation: foreign key no parent; SYS_FK_10173 table: FILEVERSION

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
