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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.operations.cleanup.CleanupOperationOptions;
import org.syncany.operations.cleanup.CleanupOperationResult;
import org.syncany.operations.restore.RestoreOperation.RestoreOperationOptions;
import org.syncany.operations.restore.RestoreOperation.RestoreOperationStrategy;
import org.syncany.operations.status.StatusOperation.StatusOperationOptions;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.tests.util.TestAssertUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.StringUtil;

public class CleanupMergeDatabaseFilesScenarioTest {
	@Test
	public void testCleanupMergeDatabaseFilesScenario1() throws Exception {
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		CleanupOperationOptions cleanupOptionsOnlyMergeDatabases = new CleanupOperationOptions();
		cleanupOptionsOnlyMergeDatabases.setMergeRemoteFiles(true);
		cleanupOptionsOnlyMergeDatabases.setRemoveOldVersions(false);
		cleanupOptionsOnlyMergeDatabases.setRepackageMultiChunks(false);
		
		UpOperationOptions upOperationOptionsNoCleanup = new UpOperationOptions();
		upOperationOptionsNoCleanup.setForceUploadEnabled(true);
		upOperationOptionsNoCleanup.setCleanupEnabled(true);	
		upOperationOptionsNoCleanup.setCleanupOptions(cleanupOptionsOnlyMergeDatabases);

		// Run preparations
		int[] clientUpSequence = new int[] {
				// Modeled after a crashing real-world scenario
				// 1 = A down+up, 2 = B down+up

				// The actual sequence was:
				// 1, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 1, 2, 1, 1, 1, 2,
				// 2, 2, 2, 1, 1, 1, 2, 1, 1, 1, 2, 2, 2, 1, 1, 2, 1, 1

				// This simplified sequence also crashes/crashed
				// 16x "1", merge happens after 15!
				1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1
		};

		for (int i=0; i<clientUpSequence.length; i++) {
			if (clientUpSequence[i] == 1) {
				clientA.down();

				clientA.createNewFile("A-file"+i+".jpg", i);
				clientA.up(upOperationOptionsNoCleanup);
			}
			else {
				clientB.down();
				
				clientB.createNewFile("B-file"+i+".jpg", i);
				clientB.up(upOperationOptionsNoCleanup);				
			}
		}
		
		clientA.down();
		clientB.down();
		
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		
		// Make sure the "merge" process actually happened
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000001").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000005").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000010").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());
		
		// Run
		clientC.down(); // <<< Here is/was the issue: Client C failed when downloading 
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientC.getDatabaseFile());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
	
	@Test
	@Ignore
	public void testIssue58_1() throws Exception {
		/*
		 * This is the attempt to reproduce issue #58
		 * https://github.com/syncany/syncany/issues/58
		 * 
		 */
		
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile());
		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile());
		
		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);
		
		UpOperationOptions upOperationOptionsForceUpload = new UpOperationOptions();
		upOperationOptionsForceUpload.setForceUploadEnabled(true);		
		upOperationOptionsForceUpload.setStatusOptions(statusOptionsForceChecksum);
		
		// Run preparations
		
		clientA.down();
		clientA.createNewFolder("Untitled Folder");
		clientA.up(upOperationOptionsForceUpload); // 0 (A1)

		clientA.down();
		clientA.createNewFile("131108 Syncany Screencast Conflict Raw.mp4");
		clientA.up(upOperationOptionsForceUpload); // 1 (A2)
		
		clientB.down();
		clientB.createNewFile("domain-driven-design-tackling-complexity-in-the-heart-of-software.9780321125217.24620.pdf");
		clientB.up(upOperationOptionsForceUpload); // 2 (A2,B1)

		clientA.down();
		clientA.moveFile("domain-driven-design-tackling-complexity-in-the-heart-of-software.9780321125217.24620.pdf",
				"MOVED domain-driven-design-tackling-complexity-in-the-heart-of-software.9780321125217.24620.pdf");
		clientA.up(upOperationOptionsForceUpload); // 3 (A3,B1)

		clientA.down();
		clientA.createNewFile("Syncany Crypto.jpg");
		clientA.createNewFile("Syncany Crypto (1).jpg"); // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Added "6262ef818c4f72bd8a69768aa261c2eb16a6c1a5"				
		clientA.createNewFile("Syncany Crypto (2).jpg");
		clientA.createNewFile("Syncany Crypto (3).jpg");
		clientA.up(upOperationOptionsForceUpload); // 4 (A4,B1)

		clientB.down();
		clientB.createNewFolder("python_ctf_workshop");
		clientB.createNewFile("python_ctf_workshop/level01.py");		
		clientB.createNewFile("python_ctf_workshop/level02.py");
		// ...
		clientB.up(upOperationOptionsForceUpload); // 5 (A4,B2)

		clientA.down();
		clientA.createNewFile("Wordlists.gz");
		clientA.up(upOperationOptionsForceUpload); // 6 (A5,B2)

		clientB.down();
		clientB.createNewFolder("Untitled Folder/workspace/.metadata/.plugins/org.eclipse.core.resources/.history/a8/");		
		clientB.createNewFolder("Untitled Folder/workspace/.metadata/.plugins/org.eclipse.mylyn.tasks.ui");		
		clientB.createNewFile("Untitled Folder/workspace/.metadata/.plugins/org.eclipse.core.resources/.history/a8/b0cc654a817b001310f7c9cad6a53f98");
		// ...
		clientB.up(upOperationOptionsForceUpload); // 7 (A5,B3)

		clientA.down();
		clientA.moveFile("Syncany Crypto.jpg", "Untitled Folder/Syncany Crypto.jpg");
		clientA.moveFile("Syncany Crypto (1).jpg", "Untitled Folder/Syncany Crypto (1).jpg");  // <<<<<<<<<<<<<< Moved first here
		clientA.moveFile("Syncany Crypto (2).jpg", "Untitled Folder/Syncany Crypto (2).jpg"); 
		clientA.moveFile("Syncany Crypto (3).jpg", "Untitled Folder/Syncany Crypto (3).jpg");
		clientA.moveFile("Wordlists.gz", "Untitled Folder/Wordlists.gz");
		clientA.moveFile("python_ctf_workshop", "Untitled Folder/python_ctf_workshop");
		// ...
		clientA.up(upOperationOptionsForceUpload); // 8 (A6,B3)

		clientB.down();
		clientB.moveFile("Untitled Folder", "Untitled Folder2");				
		Files.setPosixFilePermissions(clientB.getLocalFile("Untitled Folder2/workspace").toPath(), PosixFilePermissions.fromString("rwxrwxrwx"));
		Files.setPosixFilePermissions(clientB.getLocalFile("Untitled Folder2/python_ctf_workshop").toPath(), PosixFilePermissions.fromString("rwxrwxrwx"));
		clientB.up(upOperationOptionsForceUpload); // 9 (A6,B4) <<<< wins the conflict
		
		Thread.sleep(100);
		
		//clientA.down();
		clientA.moveFile("Untitled Folder", "philipp");  // <<<<<<<<<<<<<< Moved again in losing DIRTY version
		clientA.up(upOperationOptionsForceUpload); // 10 (A7,B3) <<<< loses the conflict (later marked DIRTY by A)

		clientB.down();
		clientB.createNewFolder("Untitled Folder2/workspace/.metadata/.plugins/org.eclipse.core.resources/.projects/syncany-cli");
		clientB.createNewFile("Untitled Folder2/workspace/.metadata/.plugins/org.eclipse.core.resources/.projects/syncany-cli/.syncinfo.snap");
		// ... (finishing up move; this should also not happen in a perfect world)
		clientB.up(upOperationOptionsForceUpload); // 11 (A6,B5) <<<<<< This is where C should have a conflict

		clientA.down();
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from databaseversion where status='DIRTY'", databaseConnectionA));
		TestAssertUtil.assertConflictingFileExists("workspace", clientA.getLocalFiles());		
		TestAssertUtil.assertConflictingFileExists("python_ctf_workshop", clientA.getLocalFiles());		
		clientA.up(upOperationOptionsForceUpload); // 12 (A8,B5)  Fixes DIRTY version
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from databaseversion where status='DIRTY'", databaseConnectionA));
				
		clientA.down();
		UpOperationResult upResultA9 = clientA.up(upOperationOptionsForceUpload); // 13 (A9,B5)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000009").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000010").exists());
		
		// assertEquals(false, upResultA9.getStatusResult().getChangeSet().hasChanges());
		// assertEquals(false, upResultA9.getChangeSet().hasChanges());
		
		//      ^^^^^^^^^^^
		// TODO [medium] Here, file following file is somehow not added or double-added?!
		//       Untitled Folder2/workspace (A's conflicted copy, 20 Mar 14, 11-07 PM)/.metadata/.plugins/org.eclipse.core.resources/.history/a8/b0cc654a817b001310f7c9cad6a53f98
		//       ..
		
		clientB.down();
		for (File fileInDir : clientB.getLocalFile("Untitled Folder2").listFiles()) {
			TestFileUtil.deleteDirectory(fileInDir);
		}
		clientB.up(upOperationOptionsForceUpload); // 14 (A9,B6)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000006").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000007").exists());
		
		clientA.down();
		clientA.createNewFolder("Wordlists");
		clientA.createNewFile("Wordlists/wordlist_dict_german_skullsecurity.org.txt");
		clientA.createNewFile("Wordlists/wordlist_500-worst-passwords_skullsecurity.org.txt");
		// ...
		clientA.up(upOperationOptionsForceUpload); // 15 (A10,B6)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000010").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());

		clientA.down();
		clientA.createNewFolder("Untitled Folder 2");
		clientA.deleteFile("Wordlists");
		clientA.up(upOperationOptionsForceUpload); // 16 (A11,B6)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000012").exists());
		
		Date restoreMoment = new Date(System.currentTimeMillis()); // for later "restore"
		
		clientA.down();
		clientA.changeFile("philipp/python_ctf_workshop/level01.py");
		clientA.up(upOperationOptionsForceUpload); // 17 (A12,B6)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000012").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000013").exists());
		
		clientB.changeFile("philipp/python_ctf_workshop/level01.py"); // << created "conflicted copy"
		clientB.down();
		TestAssertUtil.assertConflictingFileExists("level01.py", clientB.getLocalFiles());
		clientB.up(upOperationOptionsForceUpload); // 18 (A12,B7)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000007").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000008").exists());
		
		clientB.down();
		File[] conflictingFiles = clientB.getLocalFile("philipp/python_ctf_workshop").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File base, String name) {
				return name.contains("conflicted");
			}
			
		});
		assertEquals(1, conflictingFiles.length);
		File conflictingLevel01 = conflictingFiles[0];
		conflictingLevel01.delete(); // <<<<<<<<<< Delete conflict file
		clientB.up(upOperationOptionsForceUpload); // 19 (A12,B8)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000008").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000009").exists());
		
		clientB.down();
		clientB.changeFile("philipp/python_ctf_workshop/level01.py");
		UpOperationResult upResultB910 = clientB.up(upOperationOptionsForceUpload); // 20 (A12,B9)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000009").exists()); 
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000010").exists());	// << 21 (A12,B19) PURGE database
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000011").exists());
		assertNotNull(upResultB910.getCleanupResult());
		assertEquals(1, upResultB910.getCleanupResult().getRemovedOldVersionsCount());
		
		clientA.down();
		clientA.changeFile("philipp/python_ctf_workshop/level01.py");
		UpOperationResult upResult1314 = clientA.up(upOperationOptionsForceUpload); // 22 (A13,B10)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000013").exists()); 
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000014").exists());	// << 21 (A14,B10) PURGE database
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000015").exists());
		assertNotNull(upResult1314.getCleanupResult());
		assertEquals(1, upResult1314.getCleanupResult().getRemovedOldVersionsCount());
		
		clientA.down();
		clientA.createNewFolder("Untitled Folder");
		clientA.createNewFolder("Untitled Folder 3");
		clientA.createNewFolder("Untitled Folder 4");
		clientA.createNewFolder("Untitled Folder 5");
		clientA.createNewFolder("Untitled Folder 6");
		clientA.up(upOperationOptionsForceUpload); // 24 (A15,B10)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000015").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000016").exists());
		
		clientB.down();
		clientB.deleteFile("philipp/Syncany Crypto.jpg");
		clientB.deleteFile("philipp/Syncany Crypto (1).jpg");
		clientB.deleteFile("philipp/Syncany Crypto (2).jpg");
		clientB.deleteFile("philipp/Syncany Crypto (3).jpg");
		clientB.up(upOperationOptionsForceUpload); // 25 (A15,B11)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000011").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000012").exists());
		
		clientA.down();
		RestoreOperationOptions restoreOptions = new RestoreOperationOptions();
		restoreOptions.setStrategy(RestoreOperationStrategy.DATABASE_DATE);
		restoreOptions.setRestoreFilePaths(Arrays.asList(new String[] { "philipp/python_ctf_workshop/level01.py" }));
		restoreOptions.setDatabaseBeforeDate(restoreMoment);
		clientA.restore(restoreOptions);
		TestAssertUtil.assertConflictingFileExists("level01.py", clientA.getLocalFiles());
		// ^^ The "conflicted copy" file is actually not the right behavior, but this is what happens right now
				
		// Do nothing; upload restored file + conflicting file 
		UpOperationResult upResult1617 = clientA.up(upOperationOptionsForceUpload); // 26 (A16,B11)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000016").exists()); 
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000017").exists());	// 27 (A17,B11) <<< PURGE database
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000018").exists());
		assertNotNull(upResult1617.getCleanupResult());
		assertEquals(1, upResult1617.getCleanupResult().getRemovedOldVersionsCount());		
		
		clientA.down();
		clientA.createNewFolder("renamed folder HALLO GREGORRRRRRRRRRRRRRRRRRRR");
		UpOperationResult upResultMerge1to13 = clientA.up(upOperationOptionsForceUpload); // 28 (A18,B11)
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000001").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000005").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000012").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000013").exists());	
		assertNotNull(upResultMerge1to13.getCleanupResult());
		assertEquals(12, upResultMerge1to13.getCleanupResult().getMergedDatabaseFilesCount());
		assertEquals(0, upResultMerge1to13.getCleanupResult().getRemovedOldVersionsCount());
		
		clientB.down();
		clientB.moveFile("philipp", "philipp Promi");
		clientB.up(upOperationOptionsForceUpload); // 29 (A18,B12)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000012").exists()); 
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000013").exists());

		clientB.down();
		clientB.createNewFolder("philipp Promi/workspace/.metadata/.plugins/org.eclipse.core.resources/.projects/syncany-lib/");
		// ^^ again, this continues the "up" process; this should not happen, but happens because the watcher doesnt wait for file movement to finish
		UpOperationResult upResultB1314 = clientB.up(upOperationOptionsForceUpload); // 30 (A18,B13)#
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000013").exists()); 
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000014").exists());	// 27 (A18,B14) <<< PURGE database
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000015").exists());
		assertNotNull(upResultB1314.getCleanupResult());
		assertEquals(2, upResultB1314.getCleanupResult().getRemovedOldVersionsCount());
		
		// Client A stops the watcher, and runs: sy cleanup -k1

		CleanupOperationOptions cleanupOptionsRemoveAllButOne = new CleanupOperationOptions();
		cleanupOptionsRemoveAllButOne.setMergeRemoteFiles(true);
		cleanupOptionsRemoveAllButOne.setRemoveOldVersions(true);
		cleanupOptionsRemoveAllButOne.setKeepVersionsCount(1);
		cleanupOptionsRemoveAllButOne.setRepackageMultiChunks(false);
		
		clientA.down();
		CleanupOperationResult cleanupResultA19 = clientA.cleanup(cleanupOptionsRemoveAllButOne); // 32 (A19,B14) <<< PURGE database
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000019").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000020").exists());
		assertEquals(20, cleanupResultA19.getRemovedOldVersionsCount());
		
		clientA.down();
		clientA.deleteFile("philipp Promi");
		clientA.up(upOperationOptionsForceUpload); // 33 (A20,B14)		
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000020").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000021").exists());
		
		clientB.down();
		clientB.cleanup(cleanupOptionsRemoveAllButOne); // 34 (A20,B15) <<<<<< PURGE database (sy cleanup -k1)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000015").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000016").exists());
		
		clientA.down();
		clientA.createNewFile("spring-ws-2.1.3.RELEASE-full.zip");
		clientA.createNewFile("eclipse-standard-kepler-SR1-linux-gtk-x86_64.tar.gz");
		clientA.createNewFile("apache-tomcat-7.0.39.tar.gz");
		clientA.up(upOperationOptionsForceUpload); // 35 (A21,B15)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000021").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000022").exists());
		
		clientA.down();		
		clientA.createNewFolder("sphinxbase-0.8/include");
		clientA.createNewFolder("sphinxbase-0.8/python/build");
		clientA.createNewFile("sphinxbase-0.8/autom4te.cache");
		clientA.createNewFile("sphinxbase-0.8/python/build/temp.linux-x86_64-2.7");		
		// ...
		clientA.createNewFolder("☎");
		clientA.createNewFolder("\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ ''''``````````");		
		clientA.up(upOperationOptionsForceUpload); // 36 (A22,B15)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000022").exists()); 
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000023").exists());

		// Sync them up
		clientA.down();
		clientB.down();
		
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());		
		
		// Run
		clientC.down(); // <<< Here is/was the issue: Client C failed when downloading 
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientC.getDatabaseFile());

		
		fail("implement this");
		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
	
	@Test
	public void testIssue58_2() throws Exception {
		/*
		 * This is the attempt to reproduce issue #58
		 * https://github.com/syncany/syncany/issues/58
		 * 
		 */
		
		// Setup 
		LocalConnection testConnection = (LocalConnection) TestConfigUtil.createTestLocalConnection();

		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);
		TestClient clientC = new TestClient("C", testConnection);
		
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile());
		java.sql.Connection databaseConnectionB = DatabaseConnectionFactory.createConnection(clientB.getDatabaseFile());

		CleanupOperationOptions cleanupOptionsOnlyMergeDatabases = new CleanupOperationOptions();
		cleanupOptionsOnlyMergeDatabases.setMergeRemoteFiles(true);
		cleanupOptionsOnlyMergeDatabases.setRemoveOldVersions(true);
		cleanupOptionsOnlyMergeDatabases.setRepackageMultiChunks(false);
		cleanupOptionsOnlyMergeDatabases.setKeepVersionsCount(5);		
				
		StatusOperationOptions statusOptionsForceChecksum = new StatusOperationOptions();
		statusOptionsForceChecksum.setForceChecksum(true);
		
		UpOperationOptions upOperationOptionsWithCleanupForce = new UpOperationOptions();
		upOperationOptionsWithCleanupForce.setStatusOptions(statusOptionsForceChecksum);
		upOperationOptionsWithCleanupForce.setForceUploadEnabled(true);		
		upOperationOptionsWithCleanupForce.setCleanupEnabled(true);	
		upOperationOptionsWithCleanupForce.setCleanupOptions(cleanupOptionsOnlyMergeDatabases);


		// Run preparations
		
		clientA.down();
		clientA.createNewFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A1)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000001").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000002").exists());

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A2)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000002").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000003").exists());
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A2,B1)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000001").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000002").exists());

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A3,B1)
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000003").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000004").exists());

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A4,B1) 
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000004").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000005").exists());

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A4,B2) + (A4,B3) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000002").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000003").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000004").exists());
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A5,B3) + (A6,B3) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000005").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000006").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000007").exists());
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A6,B4) + (A6,B5) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000004").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000005").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000006").exists());

		
		/*
		 * For some reason, this chunk checksum in the following commit is the reason for the exception.
		 * So we record it here to see where it vanishes from the databases.
		 */
		
		clientA.down();
		clientA.changeFile("A-file.jpg");

		String fileAndChunkChecksumThatRaisesException = StringUtil.toHex(TestFileUtil.createChecksum(clientA.getLocalFile("A-file.jpg")));
		System.out.println("Chunk/File checksum that raises the issue: "+fileAndChunkChecksumThatRaisesException);

		clientA.createNewFile("ADDED_IN_DBV_A7_B5");		
		clientA.up(upOperationOptionsWithCleanupForce); // (A7,B5) + (A8,B5) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000007").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000008").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000009").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A8,B6) + (A8,B7) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000006").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000007").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000008").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));
		
		clientA.down();
		clientA.changeFile("A-file.jpg");		
		clientA.up(upOperationOptionsWithCleanupForce); // (A9,B7) + (A10,B7) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000009").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000010").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));

		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A10,B8) + (A10,B9) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000008").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000009").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000010").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));
				
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A10,B10) + (A10,B11) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000010").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000011").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000012").exists());
		assertEquals("1", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A11,B11) + (A12,B11) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000011").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000012").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000013").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));

		// ^^^ Old chunk deleted!
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A13,B11) + (A14,B11) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000013").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000014").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000015").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));
		
		clientB.down();
		clientB.changeFile("A-file.jpg");
		clientB.up(upOperationOptionsWithCleanupForce); // (A14,B12) + (A14,B13) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000012").exists());
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000013").exists());
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-B-0000000014").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='" + fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));

		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A15,B13) + (A16,B13) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000015").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000016").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000017").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));
		
		clientA.down();
		clientA.changeFile("A-file.jpg");
		clientA.up(upOperationOptionsWithCleanupForce); // (A17,B13) + (A18,B13) [PURGE]
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000017").exists());	
		assertTrue(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000018").exists());	
		assertFalse(new File(testConnection.getRepositoryPath(), "databases/db-A-0000000019").exists());
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));		
		
		// Sync them up
		clientA.down();
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionA));
		
		clientB.down();
		assertEquals("0", TestAssertUtil.runSqlQuery("select count(*) from chunk where checksum='"+fileAndChunkChecksumThatRaisesException+"'", 
				databaseConnectionB));
		
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());		
		
		// Run
		clientC.down(); // <<< Here is/was the issue: Client C failed when downloading 
		assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientC.getDatabaseFile());

		// Tear down
		clientA.deleteTestData();
		clientB.deleteTestData();
		clientC.deleteTestData();
	}	
}
