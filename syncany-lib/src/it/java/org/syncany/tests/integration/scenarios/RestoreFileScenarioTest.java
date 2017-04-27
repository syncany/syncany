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
package org.syncany.tests.integration.scenarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.operations.restore.RestoreOperationOptions;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.unit.util.TestFileUtil;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestSqlUtil;
import org.syncany.util.StringUtil;

public class RestoreFileScenarioTest {
	@Test
	public void testRestoreDeletedFile() throws Exception {
		// Setup 
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile(), false);
				
		// A new/up
		clientA.createNewFile("A-original");		
		clientA.upWithForceChecksum();
		
		String originalFileHistoryStr = TestSqlUtil.runSqlSelect("select filehistory_id from fileversion", databaseConnectionA);
		assertNotNull(originalFileHistoryStr);
		
		FileHistoryId originalFileHistoryId = FileHistoryId.parseFileId(originalFileHistoryStr);
				
		// A "delete"
		File deletedFile = new File(tempDir, "A-original-DELETED");
		FileUtils.moveFile(clientA.getLocalFile("A-original"), deletedFile);
						
		clientA.upWithForceChecksum();
		
		// A restore
		RestoreOperationOptions operationOptions = new RestoreOperationOptions();
		
		operationOptions.setFileHistoryId(originalFileHistoryId);
		operationOptions.setFileVersion(1);
		
		clientA.restore(operationOptions);
		
		assertTrue(clientA.getLocalFile("A-original (restored version 1)").exists());
		assertEquals(
				StringUtil.toHex(TestFileUtil.createChecksum(deletedFile)),
				StringUtil.toHex(TestFileUtil.createChecksum(clientA.getLocalFile("A-original (restored version 1)"))));
		assertEquals(deletedFile.lastModified(), clientA.getLocalFile("A-original (restored version 1)").lastModified());
		assertEquals(deletedFile.length(), clientA.getLocalFile("A-original (restored version 1)").length());
		
		// Tear down
		clientA.deleteTestData();
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
    public void testRestoreDeletedFileWithTargetFile() throws Exception {
		// Setup 
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile(), false);
				
		// A new/up
		clientA.createNewFile("A-original");		
		clientA.upWithForceChecksum();
		
		String originalFileHistoryStr = TestSqlUtil.runSqlSelect("select filehistory_id from fileversion", databaseConnectionA);
		assertNotNull(originalFileHistoryStr);
		
		FileHistoryId originalFileHistoryId = FileHistoryId.parseFileId(originalFileHistoryStr);
				
		// A "delete"
		File deletedFile = new File(tempDir, "A-original-DELETED");
		FileUtils.moveFile(clientA.getLocalFile("A-original"), deletedFile);
						
		clientA.upWithForceChecksum();
		
		// A restore
		RestoreOperationOptions operationOptions = new RestoreOperationOptions();
		
		operationOptions.setFileHistoryId(originalFileHistoryId);
		operationOptions.setFileVersion(1);
		operationOptions.setRelativeTargetPath("restored-file");
		
		clientA.restore(operationOptions);
		
		assertTrue(clientA.getLocalFile("restored-file").exists());
		assertEquals(
				StringUtil.toHex(TestFileUtil.createChecksum(deletedFile)),
				StringUtil.toHex(TestFileUtil.createChecksum(clientA.getLocalFile("restored-file"))));
		assertEquals(deletedFile.lastModified(), clientA.getLocalFile("restored-file").lastModified());
		assertEquals(deletedFile.length(), clientA.getLocalFile("restored-file").length());
		
		// Tear down
		clientA.deleteTestData();
		TestFileUtil.deleteDirectory(tempDir);
	}
	
	@Test
	public void testRestoreDeletedWithSubfolders() throws Exception {
		// Setup 
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
		java.sql.Connection databaseConnectionA = DatabaseConnectionFactory.createConnection(clientA.getDatabaseFile(), false);
				
		// A new/up
		clientA.createNewFolder("folder/subfolder");
		clientA.createNewFile("folder/subfolder/A-original");		
		clientA.upWithForceChecksum();
		
		String originalFileHistoryStr = TestSqlUtil.runSqlSelect("select filehistory_id from fileversion where path='folder/subfolder/A-original'", databaseConnectionA);
		assertNotNull(originalFileHistoryStr);
		
		FileHistoryId originalFileHistoryId = FileHistoryId.parseFileId(originalFileHistoryStr);
				
		// A "delete"
		FileUtils.deleteDirectory(clientA.getLocalFile("folder"));						
		clientA.upWithForceChecksum();
		
		assertFalse(clientA.getLocalFile("folder").exists());

		// A restore
		RestoreOperationOptions operationOptions = new RestoreOperationOptions();
		
		operationOptions.setFileHistoryId(originalFileHistoryId);
		operationOptions.setFileVersion(1);
		
		clientA.restore(operationOptions);
		
		assertTrue(clientA.getLocalFile("folder/subfolder").exists());
		assertTrue(clientA.getLocalFile("folder/subfolder/A-original (restored version 1)").exists());
		
		// Tear down
		clientA.deleteTestData();
		TestFileUtil.deleteDirectory(tempDir);
	}
}
