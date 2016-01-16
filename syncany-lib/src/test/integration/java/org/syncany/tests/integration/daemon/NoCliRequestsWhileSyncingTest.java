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
package org.syncany.tests.integration.daemon;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.syncany.config.LocalEventBus;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.operations.daemon.WatchServer;
import org.syncany.operations.daemon.messages.AlreadySyncingResponse;
import org.syncany.operations.daemon.messages.StatusFolderRequest;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDaemonUtil;

import com.google.common.eventbus.Subscribe;

public class NoCliRequestsWhileSyncingTest {
	private Map<Integer, Response> responses = new HashMap<Integer, Response>();

	@BeforeClass
	public static void setUp() throws Exception {
		TestDaemonUtil.cleanUserConfig();
	}

	@Test
	public void testNoCliRequestWhileSyncing() throws Exception {
		final TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		final TestClient clientA = new TestClient("ClientA", testConnection);
		int port = 58444;

		// Load config template
		DaemonConfigTO daemonConfig = TestDaemonUtil.loadDaemonConfig("daemonOneFolderNoWebServer.xml");

		// Set port to prevent conflicts with default daemons
		daemonConfig.getWebServer().setBindPort(port);

		// Dynamically insert paths
		daemonConfig.getFolders().get(0).setPath(clientA.getConfig().getLocalDir().getAbsolutePath());

		// Create access token (not needed in this test, but prevents errors in daemon)
		daemonConfig.setPortTO(TestDaemonUtil.createPortTO(port));

		// Register to event bus
		LocalEventBus localEventBus = LocalEventBus.getInstance();
		localEventBus.register(this);

		// Prepare CLI request
		StatusFolderRequest cliStatusRequest = new StatusFolderRequest();
		cliStatusRequest.setId(2586);
		cliStatusRequest.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());

		// Create watchServer
		WatchServer watchServer = new WatchServer();
		watchServer.start(daemonConfig);
		Thread.sleep(1000); // Settlement for watch server

		// Create large file, then wait 3sec for the settlement timer and
		// send the CLI request at the same time
		clientA.createNewFile("largefile", 10 * 1024 * 1024);

		Response response = null;
		for (int i = 1; i < 100; i++) {
			cliStatusRequest.setId(i);
			localEventBus.post(cliStatusRequest);

			// Then, let's hope the response is "no, no, no!"
			response = waitForResponse(i);

			if (response instanceof AlreadySyncingResponse) {
				break;
			}
			Thread.sleep(40);
		}
		assertTrue(response instanceof AlreadySyncingResponse);

		watchServer.stop();
		clientA.deleteTestData();
	}

	@Subscribe
	public void onResponseReceived(Response response) {
		responses.put(response.getRequestId(), response);
	}

	private Response waitForResponse(int id) throws Exception {
		int i = 0;
		while (!responses.containsKey(id) && i < 1000) {
			Thread.sleep(100);
			i++;
		}

		return responses.get(id);
	}
}