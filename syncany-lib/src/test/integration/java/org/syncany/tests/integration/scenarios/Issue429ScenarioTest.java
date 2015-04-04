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

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
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

	@Test
	public void testSameFileDifferentNameFuzzy() throws Exception {
		for (int seed = 1; seed < 1000; seed++) {
			LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

			TestClient clientA = new TestClient("A", testConnection);
			TestClient clientB = new TestClient("B", testConnection);

			Random randomA = new Random(2 * seed);
			Random randomB = new Random(2 * seed + 1);

			activeThread A = new activeThread(randomA, clientA);
			activeThread B = new activeThread(randomB, clientB);
			Thread AThread = new Thread(A, "A");
			Thread BThread = new Thread(B, "B");
			try {
				AThread.start();
				BThread.start();
				int actionsA = -1;
				int actionsB = -1;

				for (int i = 0; i < 50; i++) {
					TestClient clientC = new TestClient("C", testConnection);
					clientC.down();
					if (!AThread.isAlive() || !BThread.isAlive()) {
						throw new RuntimeException("One of the threads died");
					}

					if (actionsA == A.actions || actionsB == B.actions) {
						throw new RuntimeException("One of the threads stopped doing things");
					}

					actionsA = A.actions;
					actionsB = B.actions;

					FileUtils.deleteDirectory(clientC.getLocalFile(""));
					Thread.sleep(2000);
				}

				AThread.interrupt();
				BThread.interrupt();
			}
			catch (Exception e) {
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
		public int actions = 0;

		public activeThread(Random random, TestClient client) {
			this.random = random;
			this.client = client;
		}

		@Override
		public void run() {
			try {
				while (true) {
					int choice = random.nextInt(8);

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
					actions++;
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
}
