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

import org.junit.Test;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class Issue303ScenarioTest {
	@Test
	public void testIssue303DeleteMovedFile() throws Exception {
		/*
		 * This test moves a file and then deletes it. In issue #303, clients
		 * that had not moved the file already did not delete the file. It was left over
		 * leading to re-synchronization with the next 'up'.
		 */
		
		// Setup 
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();		
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		clientA.createNewFile("Hello Christian, did something break?.txt");
		clientA.upWithForceChecksum();
		
		clientB.down();
		clientB.moveFile("Hello Christian, did something break?.txt", "Hello Christian, did something break (filename conflict).txt");
		clientB.upWithForceChecksum();
		
		clientB.deleteFile("Hello Christian, did something break (filename conflict).txt");
		clientB.upWithForceChecksum();
		
		clientA.down();
		assertFalse("File should have been deleted.", clientA.getLocalFile("Hello Christian, did something break?.txt").exists());
		assertFalse("File should not have been created.", clientA.getLocalFile("Hello Christian, did something break (filename conflict).txt").exists());
		
		// Tear down
		clientB.deleteTestData();
		clientA.deleteTestData();
	}		
}
