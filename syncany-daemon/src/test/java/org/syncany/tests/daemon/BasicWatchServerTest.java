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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.syncany.config.to.DaemonConfigTO;
import org.syncany.database.FileVersion;
import org.syncany.operations.daemon.LocalEventBus;
import org.syncany.operations.daemon.WatchServer;
import org.syncany.operations.daemon.messages.CliRequest;
import org.syncany.operations.daemon.messages.CliResponse;
import org.syncany.operations.daemon.messages.GetFileHistoryRequest;
import org.syncany.operations.daemon.messages.GetFileHistoryResponse;
import org.syncany.operations.daemon.messages.GetFileRequest;
import org.syncany.operations.daemon.messages.GetFileResponseInternal;
import org.syncany.operations.daemon.messages.GetFileTreeRequest;
import org.syncany.operations.daemon.messages.GetFileTreeResponse;
import org.syncany.operations.daemon.messages.Response;
import org.syncany.operations.daemon.messages.RestoreRequest;
import org.syncany.operations.daemon.messages.RestoreResponse;
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
	
	private GetFileResponseInternal internalResponse;	
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
		List<FileVersion> files = new ArrayList<FileVersion>();
		
		for (int i = 0; i < 20; i++) {
			GetFileTreeRequest request = new GetFileTreeRequest();
			request.setId(i);
			request.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
				
			eventBus.post(request);
			
			Response response = waitForResponse(i);
			
			assertTrue(response instanceof GetFileTreeResponse);
			GetFileTreeResponse treeResponse = (GetFileTreeResponse) response;
			
			files = treeResponse.getFiles();
			
			if (files.size() == 2) {
				break;
			}
			
			if (i == 19) {
				assertEquals(2, files.size());
			}
			else {
				Thread.sleep(1000);
			}
		}
		
		if (files.get(0).getName().equals("folder")) {
			files = Arrays.asList(new FileVersion[]{files.get(1), files.get(0)});
		}

		assertEquals(clientA.getLocalFile("file-1").getName(), files.get(0).getName());
		assertEquals(clientA.getLocalFile("file-1").length(), (long) files.get(0).getSize());

		assertEquals(clientA.getLocalFile("folder").getName(), files.get(1).getName());
		assertTrue(clientA.getLocalFile("folder").isDirectory());
		assertEquals(files.get(1).getType(), FileVersion.FileType.FOLDER);

		// Create GetFileHistoryRequest for the first returned file
		GetFileHistoryRequest request = new GetFileHistoryRequest();
		request.setId(21);
		request.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
		request.setFileHistoryId(files.get(0).getFileHistoryId().toString());
						
		eventBus.post(request);
		
		Response response = waitForResponse(21);
		assertTrue(response instanceof GetFileHistoryResponse);
		GetFileHistoryResponse fileHistoryResponse = (GetFileHistoryResponse) response;
		assertEquals(1, fileHistoryResponse.getFiles().size());
		assertEquals(files.get(0), fileHistoryResponse.getFiles().get(0));
		
		// Create GetFileRequest for the first returned file
		GetFileRequest getFileRequest = new GetFileRequest();
		getFileRequest.setId(22);
		getFileRequest.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
		getFileRequest.setFileHistoryId(files.get(0).getFileHistoryId().toString());
		getFileRequest.setVersion(1);		
				
		eventBus.post(getFileRequest);
		
		int i = 0;
		while (internalResponse == null && i < 40) {
			Thread.sleep(100);
			i++;
		}
		
		assertEquals((long)files.get(0).getSize(), internalResponse.getTempFile().length());
		
		// Cli Requests
		clientA.copyFile("file-1", "file-1.bak");
		
		// CLI request while running.
		CliRequest cliRequest = new CliRequest();
		cliRequest.setId(30);
		cliRequest.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
		cliRequest.setCommand("status");
		cliRequest.setCommandArgs(new ArrayList<String>());
		
		// Create big file to trigger sync
		clientA.createNewFile("bigfileforlongsync", 5000);
		// Wait to allow sync to start
		Thread.sleep(100);
		
		eventBus.post(cliRequest);
		
		response = waitForResponse(30);
		
		assertTrue(response instanceof CliResponse);
		CliResponse cliResponse = (CliResponse) response;
		
		assertEquals("Cannot run CLI commands while sync is running or requested.\n", cliResponse.getOutput());
		
		// Allow daemon to sync
		Thread.sleep(10000);
		for (i = 31; i < 50; i++) {
			cliRequest.setId(i);
			eventBus.post(cliRequest);
			
			response = waitForResponse(i);
			cliResponse = (CliResponse) response;
			
			if (!"Cannot run CLI commands while sync is running or requested.\n".equals(cliResponse.getOutput())) {
				break;
			}
			
			Thread.sleep(1000);
		}		
		
		assertEquals("No local changes.\n", cliResponse.getOutput());
		
		// Restore file test
		
		RestoreRequest restoreRequest = new RestoreRequest();
		restoreRequest.setId(70);
		restoreRequest.setRoot(clientA.getConfig().getLocalDir().getAbsolutePath());
		restoreRequest.setFileHistoryId(files.get(0).getFileHistoryId().toString());
		restoreRequest.setVersion(1);
		
		eventBus.post(restoreRequest);
		
		response = waitForResponse(70);
		
		assertTrue(response instanceof RestoreResponse);
		RestoreResponse restoreResponse = (RestoreResponse) response;
		
		byte[] copyChecksum = FileUtil.createChecksum(clientA.getLocalFile("file-1.bak"), "SHA1");
		byte[] restoreChecksum = FileUtil.createChecksum(restoreResponse.getRestoredFile(), "SHA1");
		
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
	public void onGetFileResponseInternal(GetFileResponseInternal internalResponse) {
		this.internalResponse = internalResponse;
	}
	
	private Response waitForResponse(int id) throws Exception {
		int i = 0;
		while (responses.containsKey(id) == false && i < 1000) {
			Thread.sleep(100);
			i++;
		}
		
		return responses.get(id);
	}

}