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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.syncany.connection.plugins.Connection;
import org.syncany.operations.RestoreOperation.RestoreOperationOptions;
import org.syncany.operations.RestoreOperation.RestoreOperationStrategy;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.StringUtil;

public class RestoreFileScenarioTest {
	// TODO [medium] Also test restoring old versions of a file; not only deleted files
	
	@Test
	public void testRestoreDeletedFile() throws Exception {
		// Scenario: A uploads a file, uploads a new version, then restores the old one
		
		// Setup 
		File tempDir = TestFileUtil.createTempDirectoryInSystemTemp();
		Connection testConnection = TestConfigUtil.createTestLocalConnection();		
		TestClient clientA = new TestClient("A", testConnection);
				
		// A new/up
		clientA.createNewFile("A-original");		
		clientA.up();
		
		Date restoreMoment = new Date(System.currentTimeMillis());
		Thread.sleep(50);
		
		// A "delete"
		File deletedFile = new File(tempDir, "A-original-DELETED");
		FileUtils.moveFile(clientA.getLocalFile("A-original"), deletedFile);
						
		clientA.up();
		
		// A restore
		RestoreOperationOptions operationOptions = new RestoreOperationOptions();
		operationOptions.setDatabaseBeforeDate(restoreMoment);
		operationOptions.setRestoreFilePaths(Arrays.asList("A-original"));
		operationOptions.setStrategy(RestoreOperationStrategy.DATABASE_DATE);
		
		clientA.restore(operationOptions);
		
		assertTrue(clientA.getLocalFile("A-original").exists());
		assertEquals(
				StringUtil.toHex(TestFileUtil.createChecksum(deletedFile)),
				StringUtil.toHex(TestFileUtil.createChecksum(clientA.getLocalFile("A-original"))));
		assertEquals(deletedFile.lastModified(), clientA.getLocalFile("A-original").lastModified());
		assertEquals(deletedFile.length(), clientA.getLocalFile("A-original").length());
		
		// Tear down
		clientA.deleteTestData();
		TestFileUtil.deleteDirectory(tempDir);
	}
}
