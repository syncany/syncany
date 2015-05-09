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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.status.StatusOperationResult;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class CallUpWhileStillWritingFileScenarioTest {
	private static final Logger logger = Logger.getLogger(CallUpWhileStillWritingFileScenarioTest.class.getSimpleName());

	@Test
	public void testUpWhileWritingFile() throws Exception {
		// Setup
		final TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();

		final TestClient clientA = new TestClient("A", testConnection);
		final TestClient clientB = new TestClient("B", testConnection);

		final File testFile = clientA.getLocalFile("large-test-file");
		final long testFileLength = 100 * 1024 * 1024;

		Thread writeFileThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					logger.log(Level.INFO, "Started thread to write file to " + testFile + "  ...");

					FileOutputStream fos = new FileOutputStream(testFile);
					Random randomEngine = new Random();

					byte[] buf = new byte[4096];
					int writtenLen = 0;

					while (writtenLen < testFileLength) {
						randomEngine.nextBytes(buf);
						fos.write(buf, 0, buf.length);

						writtenLen += buf.length;
					}

					fos.close();

					logger.log(Level.INFO, "Ended thread to write file to " + testFile + "  ...");
				}
				catch (IOException e) {
					logger.log(Level.FINE, "Thread failed to write to file", e);
				}
			}
		}, "writerThread");



		// Before start: setup up databases (takes a while)
		clientA.status();
		clientB.status();

		// Run!
		writeFileThread.start();

		Thread.sleep(50);

		logger.log(Level.INFO, "Started clientA.up()");
		UpOperationResult upResult = clientA.up();
		StatusOperationResult statusResult = upResult.getStatusResult();
		logger.log(Level.INFO, "Ended clientA.up()");

		writeFileThread.join();

		// Test 1: Check result sets for inconsistencies
		assertTrue("Status command expected to return changes.", statusResult.getChangeSet().hasChanges());
		assertFalse("File should NOT be uploaded while still writing (no half-file upload).", upResult.getChangeSet().hasChanges());

		// Test 2: Check database for inconsistencies
		SqlDatabase database = clientA.loadLocalDatabase();

		assertEquals("File should NOT be uploaded while still writing (no half-file upload).", 0, database.getFileList("large-test-file", null, false, false, false, null).size());
		assertNull("There should NOT be a new database version, because file should not have been added.", database.getLastDatabaseVersionHeader());

		// Test 3: Check file system for inconsistencies
		File repoPath = new File(((LocalTransferSettings) testConnection).getPath() + "/databases");
		String[] repoFileList = repoPath.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("database-");
			}
		});

		assertEquals("Repository should NOT contain any files.", 0, repoFileList.length);

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
	}
}
