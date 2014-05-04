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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.down.DownOperationResult.DownResultCode;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class DoSameActionAtTwoClientsTest {
	@Test
	public void testIssue76() throws Exception {
		/* If two clients create the same file at the same time, different multichunks
		 * will contain the same chunks. This lead to issue 76.
		 */
		
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		
		UpOperationOptions upOperationOptionsWithForce = new UpOperationOptions();
		upOperationOptionsWithForce.setForceUploadEnabled(true);
		
		// Client A creates some files
		clientA.getLocalFile("sphinxbase-0.8").mkdirs();
		clientA.getLocalFile("sphinxbase-0.8/win32/sphinx_jsgf2fsg").mkdirs();
		clientA.getLocalFile("sphinxbase-0.8/src/sphinx_adtools").mkdirs();
		
		clientA.createNewFile("sphinxbase-0.8/config.sub");
		clientA.createNewFile("sphinxbase-0.8/win32/sphinx_jsgf2fsg/sphinx_jsgf2fsg.vcxproj");
		clientA.createNewFile("sphinxbase-0.8/src/sphinx_adtools/sphinx_pitch.c");
		
		// Client B creates the exact SAME FILES (here: copies the file tree from A)
		FileUtils.copyDirectory(clientA.getLocalFile("sphinxbase-0.8"), clientB.getLocalFile("sphinxbase-0.8"), true);
	
		// Now, both upload that
		UpOperationResult upResultA = clientA.upWithForceChecksum(); // (A1)
		assertEquals(UpResultCode.OK_CHANGES_UPLOADED, upResultA.getResultCode());
		assertEquals(8, upResultA.getChangeSet().getNewFiles().size());
		
		UpOperationResult upResultB = clientB.up(upOperationOptionsWithForce); // (B1)
		assertEquals(UpResultCode.OK_CHANGES_UPLOADED, upResultB.getResultCode());
		assertEquals(8, upResultB.getChangeSet().getNewFiles().size());
		
		DownOperationResult downResultA = clientA.down(); 
		assertEquals(DownResultCode.OK_NO_REMOTE_CHANGES, downResultA.getResultCode());
		assertEquals(0, downResultA.getDirtyDatabasesCreated().size());
		assertEquals(false, downResultA.getChangeSet().hasChanges());
		
		// For peaking (does NOT affect the test)
		FileUtils.copyFile(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000001"), 
				new File(testConnection.getRepositoryPath(), "databases/TEMP_db-B-0000000001"));

		DownOperationResult downResultB = clientB.down(); // creates DIRTY; deletes (B1)
		assertEquals(DownResultCode.OK_WITH_REMOTE_CHANGES, downResultB.getResultCode());
		assertEquals(1, downResultB.getDirtyDatabasesCreated().size());
		assertEquals(false, downResultB.getChangeSet().hasChanges()); // TODO [low] Shouldn't this be 'true'?
		
		// For peaking (does NOT affect the test)
		FileUtils.copyDirectory(clientB.getLocalFile(".syncany/db"), clientB.getLocalFile(".syncany/db_WITH_DIRTY_B1"));
		
		Files.setPosixFilePermissions(clientB.getLocalFile("sphinxbase-0.8").toPath(), PosixFilePermissions.fromString("rwxrwxrwx"));
		UpOperationResult upResultB2 = clientB.up(); // (B2)
		assertEquals(UpResultCode.OK_CHANGES_UPLOADED, upResultB2.getResultCode());
		assertEquals(1, upResultB2.getChangeSet().getChangedFiles().size());
		
		// For peaking (does NOT affect the test)
		FileUtils.copyDirectory(clientB.getLocalFile(".syncany/db"), clientB.getLocalFile(".syncany/db_DELETED_B1_WITH_B2"));

		clientA.down();
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}	
	
}
