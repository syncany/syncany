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

import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class Issue374Pre1965DateScenarioTest {
	@Test
	public void testIssue316CleanupThenDeleteFile() throws Exception {		
		/*
		 * This test simulates files with a timestamp before 1965 (unix time < 0).  
		 */
		
		// Setup 
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();		
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		clientA.createNewFile("pre1965.txt");

		Date dateBefore1965 = new SimpleDateFormat("dd-MM-yyyy").parse("31-12-1964");		
		Files.setLastModifiedTime(clientA.getLocalFile("pre1965.txt").toPath(), FileTime.fromMillis(dateBefore1965.getTime()));
		
		clientA.upWithForceChecksum();
		
		clientB.down(); // This was throwing an exception in FileSystemAction.setLastModified()
		
		// Tear down
		clientB.deleteTestData();
		clientA.deleteTestData();
	}		
	
}
