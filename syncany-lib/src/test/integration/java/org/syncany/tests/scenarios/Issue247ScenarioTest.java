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

import java.io.File;

import org.junit.Test;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class Issue247ScenarioTest {
	@Test
	public void testIssue247() throws Exception {		
		// Setup 
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();		
		
		TestClient clientDzHome = new TestClient("Dz", testConnection);
		TestClient clientUxWork = new TestClient("Ux", testConnection);
				
		UpOperationOptions upOptionsWithForce = new UpOperationOptions();
		upOptionsWithForce.setForceUploadEnabled(true);

		// home -- generate some database versions so that the cleanup/merge will have something to merge		
		clientDzHome.createNewFile("someotherfile0.xml");
		clientDzHome.upWithForceChecksum();

		for (int i=0; i<30; i++) {
			clientDzHome.changeFile("someotherfile0.xml");
			clientDzHome.upWithForceChecksum();			
		}
		
		// home
		clientDzHome.createNewFile("config.xml"); // <<<<< This is the file/checksum causing the issue
		clientDzHome.upWithForceChecksum();

		TestFileUtil.copyFile(clientDzHome.getLocalFile("config.xml"), new File(tempDir, "config.xml")); // Make a copy (for later)		

		// work
		clientUxWork.down();

		clientUxWork.changeFile("config.xml");
		clientUxWork.upWithForceChecksum();
		
		clientUxWork.createNewFile("someotherfile1.xml");
		clientUxWork.upWithForceChecksum();
		
		clientUxWork.createNewFile("someotherfile2.xml");
		clientUxWork.upWithForceChecksum();
		
		clientUxWork.createNewFile("someotherfile3.xml");
		clientUxWork.upWithForceChecksum();
		
		// home
		clientDzHome.down();
		
		clientDzHome.changeFile("config.xml");
		clientDzHome.upWithForceChecksum();
		
		clientDzHome.changeFile("config.xml");
		clientDzHome.upWithForceChecksum();

		// work
		clientUxWork.down();

		CleanupOperationOptions cleanupOptions = new CleanupOperationOptions();
		
		clientUxWork.cleanup(cleanupOptions);
				
		// home
		clientDzHome.down();
		
		clientDzHome.deleteFile("config.xml"); // Recreate problematic config.xml/checksum
		TestFileUtil.copyFile(new File(tempDir, "config.xml"), clientDzHome.getLocalFile("config.xml"));
		
		clientDzHome.upWithForceChecksum();
		
		// work
		clientUxWork.down();
		
		// Tear down
		TestFileUtil.deleteDirectory(tempDir);
		
		clientUxWork.deleteTestData();
		clientDzHome.deleteTestData();
	}		
}
