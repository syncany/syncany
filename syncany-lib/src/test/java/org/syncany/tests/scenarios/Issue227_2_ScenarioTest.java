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

import java.io.File;
import java.io.FilenameFilter;

import org.junit.Test;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.plugins.local.LocalTransferSettings;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

import static org.junit.Assert.assertEquals;

public class Issue227_2_ScenarioTest {
	@Test
	public void testIssue227_MOM() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientIH = new TestClient("iH", testConnection);
		TestClient clientMOM = new TestClient("MOM", testConnection);
		TestClient clientHSE = new TestClient("hSE", testConnection);

		UpOperationOptions upOptionsWithForce = new UpOperationOptions();
		upOptionsWithForce.setForceUploadEnabled(true);

		clientIH.createNewFile("file1.jpg");
		clientIH.up();

		clientIH.createNewFile("file2.jpg");
		clientIH.up();

		clientMOM.down();
		clientHSE.down();

		// -

		clientMOM.createNewFile("file3.jpg");
		clientMOM.up();

		clientMOM.createNewFile("file4.jpg");
		clientMOM.up();

		clientIH.down();
		clientHSE.down();

		// All in sync

		/*
		 * We want to create a time difference situation here between different clients.
		 *
		 * In reality:
		 * - Client "hSE" uploads a new database AFTER client "MOM"
		 *
		 * In this test:
		 * 1. Client "hSE" uploads a new database BEFORE client "MOM"
		 * 2. We hide "hSE"'s database by moving it to a temp. file
		 *    // ...
		 * 5. When we do 'down' at client "IH", the databases of client "MOM" are considered DIRTY
		 *
		 */

		// 1. Upload new database for hSE
		clientHSE.createNewFile("fileHSE-1.jpg");
		clientHSE.up(upOptionsWithForce);

		File[] hSEDatabaseFiles = new File(testConnection.getPath() + "/databases/").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.contains("hSE");
			}
		});

		assertEquals(1, hSEDatabaseFiles.length);

		// 2. Hide database from other clients
		File hSEDatabaseFile = hSEDatabaseFiles[0];
		File hSEDatabaseFileHidden = new File(hSEDatabaseFile.getParentFile(), "HIDE_THIS_FILE_" + hSEDatabaseFile.getName());

		hSEDatabaseFile.renameTo(hSEDatabaseFileHidden);

		// 3. This shouldn't do anything; no new databases!
		DownOperationResult downOperationResult = clientIH.down();
		assertEquals(0, downOperationResult.getDownloadedUnknownDatabases().size());

		// 4. Upload database from client "MOM" (later considered DIRTY)
		clientMOM.createNewFile("fileMOM-1.jpg");
		clientMOM.up(upOptionsWithForce);

		clientMOM.createNewFile("fileMOM-2.jpg");
		clientMOM.up(upOptionsWithForce);

		// 5. Download changes from "MOM" (apply databases and files that will later be DIRTY)
		downOperationResult = clientIH.down();
		assertEquals(0, downOperationResult.getDirtyDatabasesCreated().size());

		// 6. Rename hidden database (= the later winner!)Now download the changes that
		//    Databases of client "MOM" will be considered "DIRTY"
		hSEDatabaseFileHidden.renameTo(hSEDatabaseFile);

		downOperationResult = clientIH.down();
		assertEquals(2, downOperationResult.getDirtyDatabasesCreated().size());

		// 7. This should remove DIRTY database versions from the database
		//    and ADD the multichunks from the previous database versions to the new database version (<< this is what kills MOM)
		clientIH.up();

		clientMOM.down(); // <<<<<<<< This should produce Pim's stack trace from issue #227

		// Tear down
		clientIH.deleteTestData();
		clientMOM.deleteTestData();
		clientHSE.deleteTestData();
	}

	@Test
	public void testIssue227_IH() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientIH = new TestClient("iH", testConnection);
		TestClient clientMOM = new TestClient("MOM", testConnection);
		TestClient clientHSE = new TestClient("hSE", testConnection);
		TestClient clientMee = new TestClient("Mee", testConnection);

		UpOperationOptions upOptionsWithForce = new UpOperationOptions();
		upOptionsWithForce.setForceUploadEnabled(true);

		clientIH.createNewFile("file1.jpg");
		clientIH.up();

		clientIH.createNewFile("file2.jpg");
		clientIH.up();

		clientMOM.down();
		clientHSE.down();

		// -

		clientMOM.createNewFile("file3.jpg");
		clientMOM.up();

		clientMOM.createNewFile("file4.jpg");
		clientMOM.up();

		clientIH.down();
		clientHSE.down();

		// All in sync

		/*
		 * We want to create a time difference situation here between different clients.
		 *
		 * In reality:
		 * - Client "hSE" uploads a new database AFTER client "MOM"
		 *
		 * In this test:
		 * 1. Client "hSE" uploads a new database BEFORE client "MOM"
		 * 2. We hide "hSE"'s database by moving it to a temp. file
		 *    // ...
		 * 5. When we do 'down' at client "IH", the databases of client "MOM" are considered DIRTY
		 *
		 */

		// 1. Upload new database for hSE
		clientHSE.createNewFile("fileHSE-1.jpg");
		clientHSE.up(upOptionsWithForce);

		File[] hSEDatabaseFiles = new File(testConnection.getPath() + "/databases/").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.contains("hSE");
			}
		});

		assertEquals(1, hSEDatabaseFiles.length);

		// 2. Hide database from other clients
		File hSEDatabaseFile = hSEDatabaseFiles[0];
		File hSEDatabaseFileHidden = new File(hSEDatabaseFile.getParentFile(), "HIDE_THIS_FILE_" + hSEDatabaseFile.getName());

		hSEDatabaseFile.renameTo(hSEDatabaseFileHidden);

		// 3. This shouldn't do anything; no new databases!
		DownOperationResult downOperationResult = clientIH.down();
		assertEquals(0, downOperationResult.getDownloadedUnknownDatabases().size());

		// 4. Upload database from client "MOM" (later considered DIRTY)
		clientMOM.createNewFile("fileMOM-1.jpg");
		clientMOM.changeFile("file1.jpg");
		clientMOM.up(upOptionsWithForce);

		clientMOM.createNewFile("fileMOM-2.jpg");
		clientMOM.up(upOptionsWithForce);

		// 5. Download changes from "MOM" (apply databases and files that will later be DIRTY)
		downOperationResult = clientIH.down();
		assertEquals(0, downOperationResult.getDirtyDatabasesCreated().size());

		// 6. Rename hidden database (= the later winner!)Now download the changes that
		//    Databases of client "MOM" will be considered "DIRTY"
		hSEDatabaseFileHidden.renameTo(hSEDatabaseFile);

		downOperationResult = clientIH.down();
		assertEquals(2, downOperationResult.getDirtyDatabasesCreated().size());

		// 7. This should remove DIRTY database versions from the database
		//    and ADD the multichunks from the previous database versions to the new database version (<< this is what kills MOM)
		clientIH.up();

		clientIH.down();
		clientIH.copyFile("file2.jpg", "file2copy.jpg"); // <<< This copies a file for which the filecontent has been deleted
		clientIH.up();

		clientMee.down(); // << This should throw Philipp's stack trace in #227

		// Tear down
		clientIH.deleteTestData();
		clientMOM.deleteTestData();
		clientHSE.deleteTestData();
		clientMee.deleteTestData();
	}


	@Test
	public void testIssue227_multiple_resolve() throws Exception {
		// Setup
		LocalTransferSettings testConnection = (LocalTransferSettings) TestConfigUtil.createTestLocalConnection();

		TestClient clientIH = new TestClient("iH", testConnection);
		TestClient clientMOM = new TestClient("MOM", testConnection);
		TestClient clientHSE = new TestClient("hSE", testConnection);
		TestClient clientMee = new TestClient("Mee", testConnection);
		TestClient clientIHtwo = new TestClient("IHtwo", testConnection);

		UpOperationOptions upOptionsWithForce = new UpOperationOptions();
		upOptionsWithForce.setForceUploadEnabled(true);

		clientIH.createNewFile("file1.jpg");
		clientIH.up();

		clientIH.createNewFile("file2.jpg");
		clientIH.up();

		clientMOM.down();
		clientHSE.down();

		// -

		clientMOM.createNewFile("file3.jpg");
		clientMOM.up();

		clientMOM.createNewFile("file4.jpg");
		clientMOM.up();

		clientIH.down();
		clientIHtwo.down();
		clientHSE.down();

		// All in sync

		/*
		 * We want to create a time difference situation here between different clients.
		 *
		 * In reality:
		 * - Client "hSE" uploads a new database AFTER client "MOM"
		 *
		 * In this test:
		 * 1. Client "hSE" uploads a new database BEFORE client "MOM"
		 * 2. We hide "hSE"'s database by moving it to a temp. file
		 *    // ...
		 * 5. When we do 'down' at client "IH", the databases of client "MOM" are considered DIRTY
		 *
		 */

		// 1. Upload new database for hSE
		clientHSE.createNewFile("fileHSE-1.jpg");
		clientHSE.up(upOptionsWithForce);

		File[] hSEDatabaseFiles = new File(testConnection.getPath() + "/databases/").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.contains("hSE");
			}
		});

		assertEquals(1, hSEDatabaseFiles.length);

		// 2. Hide database from other clients
		File hSEDatabaseFile = hSEDatabaseFiles[0];
		File hSEDatabaseFileHidden = new File(hSEDatabaseFile.getParentFile(), "HIDE_THIS_FILE_" + hSEDatabaseFile.getName());

		hSEDatabaseFile.renameTo(hSEDatabaseFileHidden);

		// 3. This shouldn't do anything; no new databases!
		DownOperationResult downOperationResult = clientIH.down();
		assertEquals(0, downOperationResult.getDownloadedUnknownDatabases().size());

		clientIHtwo.down(); // same as IH!

		// 4. Upload database from client "MOM" (later considered DIRTY)
		clientMOM.createNewFile("fileMOM-1.jpg");
		clientMOM.changeFile("file1.jpg");
		clientMOM.up(upOptionsWithForce);

		clientMOM.createNewFile("fileMOM-2.jpg");
		clientMOM.up(upOptionsWithForce);

		// 5. Download changes from "MOM" (apply databases and files that will later be DIRTY)
		downOperationResult = clientIH.down();
		assertEquals(0, downOperationResult.getDirtyDatabasesCreated().size());

		clientIHtwo.down(); // same as IH!

		// 6. Rename hidden database (= the later winner!)Now download the changes that
		//    Databases of client "MOM" will be considered "DIRTY"
		hSEDatabaseFileHidden.renameTo(hSEDatabaseFile);

		downOperationResult = clientIH.down();
		assertEquals(2, downOperationResult.getDirtyDatabasesCreated().size());

		clientIHtwo.down(); // same as IH!

		// 7. This should remove DIRTY database versions from the database
		//    and ADD the multichunks from the previous database versions to the new database version (<< this is what kills MOM)
		clientIH.up(upOptionsWithForce);

		clientIHtwo.up(upOptionsWithForce);

		/*clientIH.down();
		clientIH.copyFile("file2.jpg", "file2copy.jpg"); // <<< This copies a file for which the filecontent has been deleted
		clientIH.up();*/

		clientMee.down(); // << This should throw Philipp's stack trace in #227

		// Tear down
		clientIH.deleteTestData();
		clientMOM.deleteTestData();
		clientHSE.deleteTestData();
		clientMee.deleteTestData();
	}
}
