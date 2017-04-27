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

import static org.junit.Assert.*;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class RestrictedXmlCharInFilenameScenarioTest {
	@Test
	public void testEmptyFileCreateAndSync() throws Exception {
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		// A
		clientA.createNewFile("File|\u0002|", 100);
		clientA.up();		
		
		// B
		clientB.down();
		assertFileEquals(clientA.getLocalFile("File|\u0002|"), clientB.getLocalFile("File|\u0002|"));
		assertEquals(clientB.getLocalFile("File|\u0002|").length(), 100);

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
