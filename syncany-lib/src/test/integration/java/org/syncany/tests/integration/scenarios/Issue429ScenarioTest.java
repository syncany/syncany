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
package org.syncany.tests.integration.scenarios;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class Issue429ScenarioTest {
	private static final Logger logger = Logger.getLogger(Issue429ScenarioTest.class.getSimpleName());

	@Ignore
	public void testIssue492UploadDuplicateFile() throws Exception {
		// Setup 
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		clientA.createNewFile("file1.txt", 1024);
		clientA.upWithForceChecksum();

		FileUtils.copyFile(clientA.getLocalFile("file1.txt"), clientB.getLocalFile("file2.txt"));

		clientB.down();
		clientB.up();

		clientA.deleteFile("file1.txt");
		clientA.deleteFile("file2.txt");
		clientA.upWithForceChecksum();
		clientB.cleanup();

		clientC.down();
		// Tear down
		//clientB.deleteTestData();
		//clientA.deleteTestData();
	}

	@Ignore
	public void testSpecificQueue() throws Exception {
		String[] commands = new String[] { "A5", "B5", "B0", "B0", "A7", "A3", "B6", "B1", "A7", "A6", "A7", "B5", "A1", "A0", "B6", "A5", "B0",
				"B6", "A7", "A0", "B7", "A5", "B1", "B7", "A6", "B7", "A0", "A3", "B4", "B7", "A2", "A7", "A4", "B1", "B4", "A3", "B0", "A0", "A4",
				"A6", "B3", "B3", "B2", "A1", "B1", "B3", "A1", "A7", "B7", "B7", "A1", "B4", "A4", "A4", "A1", "B4", "A6", "B2", "B5", "B7", "A5",
				"B2", "B2", "B3", "B1", "B5", "B3", "B1", "B3", "B2", "B4" };
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		for (String command : commands) {
			int choice = Integer.parseInt(command.substring(1));
			if (command.contains("A")) {
				performAction(clientA, choice);
			}
			else {
				performAction(clientB, choice);
			}
		}
		clientA.deleteTestData();
		clientB.deleteTestData();
	}

	@Ignore
	public void testSameFileDifferentNameFuzzy() throws Exception {
		for (int seed = 0; seed < 1000; seed++) {
			LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

			TestClient clientA = new TestClient("A", testConnection);
			TestClient clientB = new TestClient("B", testConnection);

			Random randomA = new Random(2 * seed);
			Random randomB = new Random(2 * seed + 1);

			Queue<String> queue = new ConcurrentLinkedQueue<>();

			activeThread A = new activeThread(randomA, clientA, queue);
			activeThread B = new activeThread(randomB, clientB, queue);
			Thread AThread = new Thread(A, "A");
			Thread BThread = new Thread(B, "B");
			try {
				AThread.start();
				BThread.start();
				//				int actionsA = -1;
				//				int actionsB = -1;

				for (int i = 0; i < 50; i++) {
					TestClient clientC = new TestClient("C", testConnection);
					clientC.down();
					if (!AThread.isAlive() || !BThread.isAlive()) {
						throw new RuntimeException("One of the threads died");
					}

					FileUtils.deleteDirectory(clientC.getLocalFile(""));
					Thread.sleep(2000);
				}

				AThread.interrupt();
				BThread.interrupt();
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Queue:" + queue.toString());
				logger.log(Level.INFO, "Something went wrong at seed: " + seed);
				throw e;
			}
			clientA.deleteTestData();
			clientB.deleteTestData();
		}
	}

	public static class activeThread implements Runnable {
		private static final Logger logger = Logger.getLogger(activeThread.class.getSimpleName());
		Random random;
		TestClient client;
		Queue<String> queue;

		public activeThread(Random random, TestClient client, Queue<String> queue) {
			this.random = random;
			this.client = client;
			this.queue = queue;
		}

		@Override
		public void run() {
			try {
				while (true) {
					int choice = random.nextInt(8);
					queue.add(client.getConfig().getDisplayName() + choice);
					performAction(client, choice);
					int sleepTime = random.nextInt(500);
					logger.log(Level.INFO, "Now sleeping for " + sleepTime + " ms.");
					Thread.sleep(sleepTime);
				}
			}
			catch (Exception e) {
				logger.log(Level.INFO, "Error in thread", e.getMessage());
				throw new RuntimeException("Something went wrong.", e);
			}
		}

	}

	public static void performAction(TestClient client, int choice) throws Exception {
		switch (choice) {
		case 0:
			logger.log(Level.INFO, "0. Creating file with content");
			if (!client.getLocalFile(client.getConfig().getDisplayName()).exists()) {
				client.createFileWithContent(client.getConfig().getDisplayName(), "content");
			}
			break;
		case 1:
			logger.log(Level.INFO, "1. Deleting my file.");
			client.deleteFile(client.getConfig().getDisplayName());
			break;
		case 2:
			logger.log(Level.INFO, "2. Deleting all files");
			for (String file : client.getLocalFiles().keySet()) {
				client.deleteFile(file);
			}
			break;
		case 3:
			logger.log(Level.INFO, "3. Performing Up");
			client.upWithForceChecksum();
			break;
		case 4:
			logger.log(Level.INFO, "4. Performing Down");
			client.down();
			break;
		case 5:
			logger.log(Level.INFO, "5. Performing Cleanup");
			CleanupOperationOptions options = new CleanupOperationOptions();
			options.setForce(true);
			client.cleanup(options);
			break;
		case 6:
			logger.log(Level.INFO, "6. Syncing.");
			client.sync();
			break;
		case 7:
			logger.log(Level.INFO, "7. Creating file with content (2)");
			if (!client.getLocalFile(client.getConfig().getDisplayName() + "2").exists()) {
				client.createFileWithContent(client.getConfig().getDisplayName() + "2", "content");
			}
			break;
		default:
			break;
		}

	}
}
