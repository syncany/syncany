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
package org.syncany.tests.integration.operations;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.operations.log.LightweightDatabaseVersion;
import org.syncany.operations.log.LogOperation;
import org.syncany.operations.log.LogOperationOptions;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.operations.up.UpOperation;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;

public class LogOperationTest {
	@Test
	public void testLogOperation() throws Exception {
		// Setup
		Config config = TestConfigUtil.createTestLocalConfig();

		StatusOperationOptions statusOptions = new StatusOperationOptions();
		statusOptions.setForceChecksum(true);
		
		UpOperationOptions upOptions = new UpOperationOptions();
		upOptions.setStatusOptions(statusOptions);
		upOptions.setForceUploadEnabled(true);

		// First, do some uploading
		List<File> originalFiles = TestFileUtil.createRandomFilesInDirectory(config.getLocalDir(), 5*1024, 5);				
		new UpOperation(config, upOptions).execute();		
								
		// And some more
		for (File file : originalFiles) {
			TestFileUtil.changeRandomPartOfBinaryFile(file);
		}		

		new UpOperation(config, upOptions).execute();		

		// And some more
		for (File file : originalFiles) {
			file.delete();
		}		

		new UpOperation(config, upOptions).execute();		

		// Then, check the log
		LogOperationOptions logOptions = new LogOperationOptions();
		
		logOptions.setStartDatabaseVersionIndex(0);
		logOptions.setMaxDatabaseVersionCount(99);
		logOptions.setMaxFileHistoryCount(99);
		
		List<LightweightDatabaseVersion> databaseVersions = (new LogOperation(config, logOptions).execute()).getDatabaseVersions();
		
		assertEquals(3, databaseVersions.size());

		assertEquals(5, databaseVersions.get(2).getChangeSet().getNewFiles().size());
		assertEquals(0, databaseVersions.get(2).getChangeSet().getChangedFiles().size());
		assertEquals(0, databaseVersions.get(2).getChangeSet().getDeletedFiles().size());
				
		assertEquals(0, databaseVersions.get(1).getChangeSet().getNewFiles().size());
		assertEquals(5, databaseVersions.get(1).getChangeSet().getChangedFiles().size());
		assertEquals(0, databaseVersions.get(1).getChangeSet().getDeletedFiles().size());

		assertEquals(0, databaseVersions.get(0).getChangeSet().getNewFiles().size());
		assertEquals(0, databaseVersions.get(0).getChangeSet().getChangedFiles().size());
		assertEquals(5, databaseVersions.get(0).getChangeSet().getDeletedFiles().size());
		
		// Cleanup
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
}
