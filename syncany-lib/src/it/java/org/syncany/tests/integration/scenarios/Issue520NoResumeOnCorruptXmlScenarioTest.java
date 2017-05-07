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

import java.io.File;

import org.junit.Test;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Issue520NoResumeOnCorruptXmlScenarioTest {
	@Test
	public void testCorruptTransactionListFile() throws Exception {
		// Setup 
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		
		clientA.createNewFile("file1.txt", 1024);
		clientA.upWithForceChecksum();
		
		TestFileUtil.createFileWithContent(new File(clientA.getConfig().getAppDir(), "/state/transaction-list.txt"), "INVALID");
		
		clientA.createNewFile("file2.txt", 1024);
		clientA.upWithForceChecksum();  // This did FAIL due to an XML parsing exception
		
		assertEquals("There should be exactly two database files", 2, new File(testConnection.getPath() + "/databases").listFiles().length);
		assertEquals("There should be exactly two multichunks", 2, new File(testConnection.getPath() + "/multichunks").listFiles().length);
		
		// Tear down
		clientA.deleteTestData();
	}	

	@Test
	public void testCorruptTransactionFile() throws Exception {
		// Setup 
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		
		clientA.createNewFile("file1.txt", 1024);
		clientA.upWithForceChecksum();
		
		TestFileUtil.createFileWithContent(new File(clientA.getConfig().getAppDir(), "/state/transaction-list.txt"), "0000000001");
		TestFileUtil.createFileWithContent(new File(clientA.getConfig().getAppDir(), "/state/transaction-database.0000000001.xml"), "invalid");
		
		clientA.createNewFile("file2.txt", 1024);
		clientA.upWithForceChecksum();  // This did FAIL due to an XML parsing exception
		
		assertEquals("There should be exactly two database files", 2, new File(testConnection.getPath() + "/databases").listFiles().length);
		assertEquals("There should be exactly two multichunks", 2, new File(testConnection.getPath() + "/multichunks").listFiles().length);

		// Tear down
		clientA.deleteTestData();
	}	
}
