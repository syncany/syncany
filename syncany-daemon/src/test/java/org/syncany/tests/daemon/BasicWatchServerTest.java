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
package org.syncany.tests.daemon;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.config.to.PortTO;
import org.syncany.config.to.UserTO;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.daemon.WatchServer;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

/**
 * The BasicWatchServerTest tests the WatchServer as a seperate entity. It
 * should test if all basic functionality works as expected.
 * 
 * @author Pim Otte
 *
 */
public class BasicWatchServerTest {
	private static final String DAEMON_RESOURCE_PATTERN = "/org/syncany/config/to/%s";

	@Test
	public void AddSingleFileTest() throws Exception {
		final TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		final TestClient clientA = new TestClient("ClientA", testConnection);
		final TestClient clientB = new TestClient("ClientB", testConnection);
		
		// Load config from resource
		String fullPathResource = String.format(DAEMON_RESOURCE_PATTERN, "daemonTwoFoldersNoWebServer.xml");
		InputStream inputStream = DaemonConfigTO.class.getResourceAsStream(fullPathResource);
		File tempConfigFile = File.createTempFile("syncanyTemp-", "");
		tempConfigFile.deleteOnExit();
		
		try (FileOutputStream outputStream = new FileOutputStream(tempConfigFile)) {
	        IOUtils.copy(inputStream, outputStream);
	    }
		
		DaemonConfigTO daemonConfig = DaemonConfigTO.load(tempConfigFile);
		// Dynamically insert paths
		daemonConfig.getFolders().get(0).setPath(clientA.getConfig().getLocalDir().getAbsolutePath());
		daemonConfig.getFolders().get(1).setPath(clientB.getConfig().getLocalDir().getAbsolutePath());
		
		// Create access token (not needed in this test, but prevents errors in daemon)
		String accessToken = CipherUtil.createRandomAlphabeticString(20);
		
		UserTO cliUser = new UserTO();
		cliUser.setUsername("CLI");
		cliUser.setPassword(accessToken);
		
		PortTO portTO = new PortTO();
		portTO.setPort(daemonConfig.getWebServer().getPort());
		portTO.setUser(cliUser);
		
		
		// Create watchServer
		WatchServer watchServer = new WatchServer();
		
		clientA.createNewFile("file-1");
		watchServer.start(daemonConfig);
		
		
		
		for (int i = 0; i < 20; i++) {
			if(clientB.getLocalFile("file-1") != null) {
				break;
			}
			Thread.sleep(1000);
		}
		
		assertTrue("File has not synced to clientB", clientB.getLocalFile("file-1") != null);
		
		watchServer.stop();
		clientA.deleteTestData();
		clientB.deleteTestData();
		
	}

}