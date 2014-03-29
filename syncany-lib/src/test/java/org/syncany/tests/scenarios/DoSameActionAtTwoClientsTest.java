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

import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.Test;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class DoSameActionAtTwoClientsTest {
	@Test
	public void testCleanupMergeDatabaseFilesScenario1() throws Exception {
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		UpOperationOptions upOperationOptionsWithForce = new UpOperationOptions();
		upOperationOptionsWithForce.setForceUploadEnabled(true);

		clientA.createNewFile("sphinxbase-0.8.tar.gz");
		clientA.upWithForceChecksum();
		
		clientB.down();
		
		// "Extract archive"
		clientA.getLocalFile("sphinxbase-0.8").mkdirs();
		clientA.getLocalFile("sphinxbase-0.8/win32/sphinx_jsgf2fsg").mkdirs();
		clientA.getLocalFile("sphinxbase-0.8/src/sphinx_adtools").mkdirs();
		
		clientA.createNewFile("sphinxbase-0.8/config.sub");
		clientA.createNewFile("sphinxbase-0.8/win32/sphinx_jsgf2fsg/sphinx_jsgf2fsg.vcxproj");
		clientA.createNewFile("sphinxbase-0.8/src/sphinx_adtools/sphinx_pitch.c");
		
		// "Do same action" (here: copy file tree)
		clientB.getLocalFile("sphinxbase-0.8").mkdirs();
		clientB.getLocalFile("sphinxbase-0.8/win32/sphinx_jsgf2fsg").mkdirs();
		clientB.getLocalFile("sphinxbase-0.8/src/sphinx_adtools").mkdirs();
		
		TestFileUtil.copyFile(clientA.getLocalFile("sphinxbase-0.8/config.sub"), clientB.getLocalFile("sphinxbase-0.8/config.sub"));
		TestFileUtil.copyFile(clientA.getLocalFile("sphinxbase-0.8/win32/sphinx_jsgf2fsg/sphinx_jsgf2fsg.vcxproj"),
				clientB.getLocalFile("sphinxbase-0.8/win32/sphinx_jsgf2fsg/sphinx_jsgf2fsg.vcxproj"));
		TestFileUtil.copyFile(clientA.getLocalFile("sphinxbase-0.8/src/sphinx_adtools/sphinx_pitch.c"),
				clientB.getLocalFile("sphinxbase-0.8/src/sphinx_adtools/sphinx_pitch.c"));

		clientA.upWithForceChecksum();
		clientB.up(upOperationOptionsWithForce);
		
		clientA.down();
		
		clientB.down(); // creates DIRTY
		Files.setPosixFilePermissions(clientB.getLocalFile("sphinxbase-0.8").toPath(), PosixFilePermissions.fromString("rwxrwxrwx"));
		clientB.up();
		
		clientA.down();
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}	
	
}
