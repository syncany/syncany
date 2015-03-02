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
package org.syncany.tests.integration.daemon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.syncany.config.LocalEventBus;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.database.FileVersion;
import org.syncany.operations.daemon.WatchServer;
import org.syncany.operations.daemon.messages.AlreadySyncingResponse;
import org.syncany.operations.daemon.messages.GetFileFolderRequest;
import org.syncany.operations.daemon.messages.GetFileFolderResponseInternal;
import org.syncany.operations.daemon.messages.GetFileHistoryFolderRequest;
import org.syncany.operations.daemon.messages.GetFileHistoryFolderResponse;
import org.syncany.operations.daemon.messages.LsFolderRequest;
import org.syncany.operations.daemon.messages.LsFolderResponse;
import org.syncany.operations.daemon.messages.RestoreFolderRequest;
import org.syncany.operations.daemon.messages.RestoreFolderResponse;
import org.syncany.operations.daemon.messages.StatusFolderRequest;
import org.syncany.operations.daemon.messages.StatusFolderResponse;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.ls.LsOperationOptions;
import org.syncany.operations.restore.RestoreOperationOptions;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDaemonUtil;
import org.syncany.util.FileUtil;

import com.google.common.eventbus.Subscribe;

/**
 * The BasicWatchServerTest tests the WatchServer as a seperate entity. It
 * should test if all basic functionality works as expected.
 *
 * @author Pim Otte
 */
public class BasicWatchServerTest {
	private Map<Integer, Response> responses = new HashMap<Integer, Response>();

	private GetFileFolderResponseInternal internalResponse;
	private LocalEventBus eventBus;

	/**
	 * The WatchServerTest tests all things WatchServer.
	 * This is one single test to prevent issues with parallelism. (Occupied ports, EventBus mixups etc.)
	 */
	@Test
	public void testWatchServer() throws Exception {
		final TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		final TestClient clientA = new TestClient("ClientA", testConnection);
		final TestClient clientB = new TestClient("ClientB", testConnection);
		int port = 58443;

		// Load config template
		DaemonConfigTO daemonConfig = TestDaemonUtil.loadDaemonConfig("daemonTwoFoldersNoWebServer.xml");

		// Set port to prevent conflicts with default daemons
		daemonConfig.getWebServer().setBindPort(port);

		// Dynamically insert paths
		daemonConfig.getFolders().get(0).setPath(clientA.getConfig().getLocalDir().getAbsolutePath());
		daemonConfig.getFolders().get(1).setPath(clientB.getConfig().getLocalDir().getAbsolutePath());

		// Create access token (not needed in this test, but prevents errors in daemon)
		daemonConfig.setPortTO(TestDaemonUtil.createPortTO(port));

		// Create watchServer
		WatchServer watchServer = new WatchServer();

		clientA.createNewFile("file-1");
		watchServer.start(daemonConfig);

		for (int i = 0; i < 20; i++) {
			if (clientB.getLocalFile("file-1").exists()) {
				break;
			}

			Thread.sleep(1000);
		}

		assertTrue("File has not synced to clientB", clientB.getLocalFile("file-1").exists());
		assertEquals(clientA.getLocalFile("file-1").length(), clientB.getLocalFile("file-1").length());

		registerWithBus();

		// Create watchServer
		clientA.createNewFolder("folder");
		clientA.createNewFile("folder/file-2");

		// Allow server to settle
		Thread.sleep(100);

		// Repeat request until 3 files are found.
		FileVersion[] files = null;

		for (int i = 0; i < 20; i++) {
			LsFolderRequest request = new LsFolderRequest();
			LsOperationOptions lsOperationOption = new LsOperationOptions();

			request.setId(i);
			request.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
			request.setOptions(lsOperationOption);
			eventBus.post(request);

			Response response = waitForResponse(i);

			assertTrue(response instanceof LsFolderResponse);
			LsFolderResponse treeResponse = (LsFolderResponse) response;

			files = treeResponse.getResult().getFileList().toArray(new FileVersion[] {});

			if (files.length == 2) {
				break;
			}

			if (i == 19) {
				assertEquals(2, files.length);
			}
			else {
				Thread.sleep(1000);
			}
		}

		if (files[0].getName().equals("folder")) {
			files = new FileVersion[] { files[1], files[0] };
		}

		assertEquals(clientA.getLocalFile("file-1").getName(), files[0].getName());
		assertEquals(clientA.getLocalFile("file-1").length(), (long) files[0].getSize());

		assertEquals(clientA.getLocalFile("folder").getName(), files[1].getName());
		assertTrue(clientA.getLocalFile("folder").isDirectory());
		assertEquals(files[1].getType(), FileVersion.FileType.FOLDER);

		// Create GetFileHistoryRequest for the first returned file
		GetFileHistoryFolderRequest request = new GetFileHistoryFolderRequest();
		request.setId(21);
		request.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
		request.setFileHistoryId(files[0].getFileHistoryId().toString());

		eventBus.post(request);

		Response response = waitForResponse(21);
		assertTrue(response instanceof GetFileHistoryFolderResponse);
		GetFileHistoryFolderResponse fileHistoryResponse = (GetFileHistoryFolderResponse) response;
		assertEquals(1, fileHistoryResponse.getFiles().size());
		assertEquals(files[0], fileHistoryResponse.getFiles().get(0));

		// Create GetFileRequest for the first returned file
		GetFileFolderRequest getFileRequest = new GetFileFolderRequest();
		getFileRequest.setId(22);
		getFileRequest.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
		getFileRequest.setFileHistoryId(files[0].getFileHistoryId().toString());
		getFileRequest.setVersion(1);

		eventBus.post(getFileRequest);

		int i = 0;
		while (internalResponse == null && i < 40) {
			Thread.sleep(100);
			i++;
		}

		assertEquals((long) files[0].getSize(), internalResponse.getTempFile().length());

		// Cli Requests
		clientA.copyFile("file-1", "file-1.bak");

		// CLI request while running.
		StatusFolderRequest statusRequest = new StatusFolderRequest();
		StatusOperationOptions statusOperationOption = new StatusOperationOptions();
		statusOperationOption.setForceChecksum(true);

		statusRequest.setId(30);
		statusRequest.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
		statusRequest.setOptions(statusOperationOption);

		// Create big file to trigger sync
		clientA.createNewFile("bigfileforlongsync", 5000);

		// ^^ Now sync should start and we send 'status' requests
		boolean syncRunningMessageReceived = false;

		for (i = 30; i < 50; i++) {
			statusRequest.setId(i);
			eventBus.post(statusRequest);

			response = waitForResponse(i);

			if (response instanceof AlreadySyncingResponse) {
				syncRunningMessageReceived = true;
				break;
			}

			Thread.sleep(200);
		}

		assertTrue(syncRunningMessageReceived);

		// Allow daemon to sync

		Thread.sleep(10000);
		for (i = 50; i < 60; i++) {
			statusRequest.setId(i);
			eventBus.post(statusRequest);

			response = waitForResponse(i);

			if (response instanceof StatusFolderResponse) {
				break;
			}

			Thread.sleep(1000);
		}

		assertNotNull(response);
		//assertEquals("No local changes.\n", cliResponse.getOutput());

		// Restore file test

		RestoreFolderRequest restoreRequest = new RestoreFolderRequest();
		RestoreOperationOptions restoreOperationOption = new RestoreOperationOptions();
		restoreOperationOption.setFileHistoryId(files[0].getFileHistoryId());
		restoreOperationOption.setFileVersion(1);

		restoreRequest.setId(70);
		restoreRequest.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
		restoreRequest.setOptions(restoreOperationOption);

		eventBus.post(restoreRequest);

		response = waitForResponse(70);

		assertTrue(response instanceof RestoreFolderResponse);
		RestoreFolderResponse restoreResponse = (RestoreFolderResponse) response;

		byte[] copyChecksum = FileUtil.createChecksum(clientA.getLocalFile("file-1.bak"), "SHA1");
		byte[] restoreChecksum = FileUtil.createChecksum(restoreResponse.getResult().getTargetFile(), "SHA1");

		assertArrayEquals(copyChecksum, restoreChecksum);
		watchServer.stop();
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	private void registerWithBus() {
		if (eventBus == null) {
			eventBus = LocalEventBus.getInstance();
			eventBus.register(this);
		}
	}

	@Subscribe
	public void onResponseReceived(Response response) {
		responses.put(response.getRequestId(), response);
	}

	@Subscribe
	public void onGetFileResponseInternal(GetFileFolderResponseInternal internalResponse) {
		this.internalResponse = internalResponse;
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