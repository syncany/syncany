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

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

/**
 * This test was created to reproduce https://github.com/syncany/syncany/issues/92
 * This bug was caused by an issue in which the checksum of a chunk was used instead
 * of the the checksum of the content. These are the same for small files. Involving
 * some large files and triggering a cleanup caused this bug.
 * 
 * @author Pim Otte
 *
 */
public class ManySyncUpsLargeFileScenarioTest {
	@Test
	public void testManySyncUpsWithLargeFile() throws Exception {
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// Cause Cleanup with a moderately large file
		for (int i=1; i<=16; i++) {
			clientA.createNewFile("file"+i, 600181);
			TestFileUtil.changeRandomPartOfBinaryFile(clientA.getLocalFile("file"+i));
			if (i > 2) {
				TestFileUtil.changeRandomPartOfBinaryFile(clientA.getLocalFile("file"+ (i-1)));
				TestFileUtil.changeRandomPartOfBinaryFile(clientA.getLocalFile("file1"));
			}
			clientA.up();		
		}
		
		clientB.down();
		
		for (int i=1; i<=16; i++) {
			assertTrue(clientA.getLocalFile("file" + i).exists());
			assertTrue(clientB.getLocalFile("file" + i).exists());
		}
		
		clientA.deleteTestData();	
		clientB.deleteTestData();
	}
}
