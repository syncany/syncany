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

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.junit.Test;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.LimitedDosFileAttributes;

public class FolderPermissionChangeScenarioTest {
	@Test
	public void testFolderPermissionChange() throws Exception {		
		// Setup 
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Run 
		clientA.createNewFolder("Somefolder");
		clientA.upWithForceChecksum();
		
		clientB.createNewFolder("Somefolder");
		
		if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			Set<PosixFilePermission> expectedPosixPermissions = Files.getPosixFilePermissions(clientB.getLocalFile("Somefolder").toPath());
			
			Set<PosixFilePermission> changedPosixPermissions = PosixFilePermissions.fromString("rwxrwxrwx");
			Files.setPosixFilePermissions(clientB.getLocalFile("Somefolder").toPath(), changedPosixPermissions);
	
			clientB.down();
			
			assertEquals(1, clientB.getLocalFiles().size());
			assertEquals(PosixFilePermissions.toString(expectedPosixPermissions), PosixFilePermissions.toString(Files.getPosixFilePermissions(clientB.getLocalFile("Somefolder").toPath())));
		}
		else if (EnvironmentUtil.isWindows()) {
			DosFileAttributes expectedDosAttrs = Files.readAttributes(clientB.getLocalFile("Somefolder").toPath(), DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			
			Files.setAttribute(clientB.getLocalFile("Somefolder").toPath(), "dos:hidden", true);
			Files.setAttribute(clientB.getLocalFile("Somefolder").toPath(), "dos:archive", true);
			Files.setAttribute(clientB.getLocalFile("Somefolder").toPath(), "dos:system", true);
			Files.setAttribute(clientB.getLocalFile("Somefolder").toPath(), "dos:readonly", true);
		
			clientB.down();
			
			assertEquals(1, clientB.getLocalFiles().size());
			
			DosFileAttributes actualDosAttrs = Files.readAttributes(clientB.getLocalFile("Somefolder").toPath(), DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			assertEquals(LimitedDosFileAttributes.toString(expectedDosAttrs), LimitedDosFileAttributes.toString(actualDosAttrs));			
		}
		
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}		
}
