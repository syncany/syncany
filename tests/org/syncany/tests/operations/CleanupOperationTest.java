/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.tests.operations;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.operations.CleanupOperation;
import org.syncany.operations.CleanupOperation.CleanupOperationOptions;
import org.syncany.operations.CleanupOperation.CleanupStrategy;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestDatabaseUtil;

public class CleanupOperationTest {

	Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-1"));

	@Test
	public void testCleanupScenario() throws Exception {
		// Setup
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);

		// Prepare Test 1: Add two versions of a file (first to be 'outdated')
		clientA.createNewFile("file-first-version-outdated.jpg"); // outdated
		clientA.upWithForceChecksum();
		
		clientA.changeFile("file-first-version-outdated.jpg"); // NOT outdated, because current
		clientA.upWithForceChecksum();
		
		// Prepare Test 2: Add one file, delete it again (= to be 'outdated') 
		clientA.createNewFile("file-deleted-and-outdated.jpg"); // outdated
		clientA.upWithForceChecksum();
		
		clientA.deleteFile("file-deleted-and-outdated.jpg"); // outdated, because deleted
		clientA.upWithForceChecksum();
		
		// WAIT, everything from here on is NOT outdated
		Thread.sleep(1200);
		
		// Prepare Test 3: Add one file, delete it again (NOT outdated) 
		clientA.createNewFile("file-deleted-but-NOT-outdated.jpg");
		clientA.upWithForceChecksum();
		
		clientA.deleteFile("file-deleted-but-NOT-outdated.jpg");
		clientA.upWithForceChecksum();		
		
		// Prepare Test 4: Add new file (NOT outdated)
		clientA.createNewFile("file-newnewnew.jpg");
		clientA.upWithForceChecksum(); 
		
		// Run		
		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setCleanUpOlderThanSeconds(1);
		options.setStrategy(CleanupStrategy.EXPIRATION_DATE);
		
		new CleanupOperation(clientA.getConfig(), null,options).execute();

		fail("add asserts");
		
		// Tear down
		clientA.cleanup();
		clientB.cleanup();
	}

	@Test
	public void testIdentifyDatabaseVersions() throws Exception {
		Config config = TestConfigUtil.createTestLocalConfig();

		Database database = new Database();

		List<DatabaseVersion> olderDatabaseVersions = createConsistentDatabaseVersions(5, 5, -40, null);
		List<DatabaseVersion> newerDatabaseVersions = createConsistentDatabaseVersions(5, 5, 40,
				olderDatabaseVersions.get(olderDatabaseVersions.size() - 1));

		database.addDatabaseVersions(olderDatabaseVersions);
		database.addDatabaseVersions(newerDatabaseVersions);

		CleanupOperationOptions options = new CleanupOperationOptions();
		options.setCleanUpOlderThanSeconds(30);
		options.setStrategy(CleanupStrategy.EXPIRATION_DATE);

		CleanupOperation operation = new CleanupOperation(config, database, options);
		operation.execute();
		// List<DatabaseVersion> identifiedDatabaseVersions = operation.identifyDatabaseVersions(options);

		// assertEquals(identifiedDatabaseVersions, olderDatabaseVersions);
	}

	private List<DatabaseVersion> createConsistentDatabaseVersions(int amount, int minuteOffset, int dayOffset, DatabaseVersion basedOnDatabaseVersion) {
		List<DatabaseVersion> databaseVersions = new ArrayList<DatabaseVersion>();

		DatabaseVersion databaseVersion = basedOnDatabaseVersion;
		calendar.add(Calendar.DATE, dayOffset);

		for (int i = 0; i < amount; i++) {
			calendar.add(Calendar.MINUTE, minuteOffset);
			Date databaseVersionDate = calendar.getTime();

			databaseVersion = TestDatabaseUtil.createDatabaseVersion(databaseVersion, databaseVersionDate);

			for (int j = 10; j < 20; j++) {
				PartialFileHistory fileHistory = new PartialFileHistory(FileHistoryId.parseFileId("" + j));

				for (int k = 0; k < 3; k++) {
					FileVersion fileVersion = new FileVersion();

					fileVersion.setVersion((long) k + 1);
					fileVersion.setType(FileType.FOLDER);
					fileVersion.setPath("dbv-" + i + "-file" + j + "-version-" + k + ".jpg");
					fileVersion.setUpdated(new Date(databaseVersionDate.getTime() + k * 1000));

					fileHistory.addFileVersion(fileVersion);
				}

				databaseVersion.addFileHistory(fileHistory);
			}

			databaseVersions.add(databaseVersion);
		}
		return databaseVersions;
	}
}
