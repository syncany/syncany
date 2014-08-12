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
package org.syncany.tests.operations;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.operations.ChangeSet;
import org.syncany.operations.status.StatusOperation;
import org.syncany.operations.status.StatusOperationOptions;
import org.syncany.operations.up.UpOperation;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class StatusOperationTest {

	@Test
	public void testStatusOperation() throws Exception {
		// Setup
		Config config = TestConfigUtil.createTestLocalConfig();
		
		// Add new files 
		List<File> originalFiles = TestFileUtil.createRandomFilesInDirectory(config.getLocalDir(), 500*1024, 3);
		 
		// Status
		ChangeSet changeSet = (new StatusOperation(config).execute()).getChangeSet();				
		
		assertEquals(changeSet.getNewFiles().size(), originalFiles.size());
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);
				
		// Up
		new UpOperation(config).execute();		
				
		// Status
		changeSet = (new StatusOperation(config).execute()).getChangeSet();		
		
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);

		// Change all files, run 'status'
		Thread.sleep(1000); // TODO [low] StatusOperation relies on file modified time and size, any other methods?

		for (File file : originalFiles) {
			TestFileUtil.changeRandomPartOfBinaryFile(file);
		}		
		
		changeSet = (new StatusOperation(config).execute()).getChangeSet();
		
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), originalFiles.size());
		assertEquals(changeSet.getDeletedFiles().size(), 0);
		
		// Up
		new UpOperation(config).execute();
				
		// Delete all files, run 'status' again
		for (File file : originalFiles) {
			TestFileUtil.deleteFile(file);
		}
				
		changeSet = (new StatusOperation(config).execute()).getChangeSet();
		
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), originalFiles.size());
				
		// Cleanup
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
	@Test
	public void testVeryRecentFileModificationWithoutSizeOrModifiedDateChange() throws Exception {
		// Setup
		Config config = TestConfigUtil.createTestLocalConfig();		
		File testFile = TestFileUtil.createRandomFileInDirectory(config.getLocalDir(), 40);
		
		StatusOperationOptions statusOptions = new StatusOperationOptions();
		statusOptions.setForceChecksum(true);

		UpOperationOptions syncUpOptions = new UpOperationOptions();
		syncUpOptions.setStatusOptions(statusOptions);			
		
		// Perform 'up' and immediately change test file
		// IMPORTANT: Do NOT sleep to enforce checksum-based comparison in 'status'
		new UpOperation(config, syncUpOptions, null).execute();		
		TestFileUtil.changeRandomPartOfBinaryFile(testFile);
		
		// Run 'status', this should run a checksum-based file comparison
		ChangeSet changeSet = (new StatusOperation(config, statusOptions).execute()).getChangeSet();						
		assertEquals(changeSet.getChangedFiles().size(), 1);
				
		// Cleanup 
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
	@Test
	public void testNotSoRecentFileModificationWithoutSizeOrModifiedDateChange() throws Exception {
		// Setup
		Config config = TestConfigUtil.createTestLocalConfig();
		File testFile = TestFileUtil.createRandomFileInDirectory(config.getLocalDir(), 40);
		
		// Perform 'up', wait a second and then change test file
		// IMPORTANT: Sleep to prevent detailed checksum-based update check in 'status' operation
		new UpOperation(config).execute();				
		Thread.sleep(1500);
		TestFileUtil.changeRandomPartOfBinaryFile(testFile);
		
		// Run 'status', this should NOT run a checksum-based file comparison
		ChangeSet changeSet = (new StatusOperation(config).execute()).getChangeSet();						
		assertEquals(changeSet.getChangedFiles().size(), 1);
				
		// Cleanup 
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
	@Test
	public void testCreateFolderAndRunStatus() throws Exception {
		// Setup
		Config config = TestConfigUtil.createTestLocalConfig();
		new File(config.getLocalDir()+"/somefolder").mkdir();
				
		// Run 'status', this SHOULD list the folder
		ChangeSet changeSet = (new StatusOperation(config).execute()).getChangeSet();						
		assertEquals(changeSet.getNewFiles().size(), 1);
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);
		assertEquals(changeSet.getUnchangedFiles().size(), 0);	
		
		// Run 'up' to check in the folder
		new UpOperation(config).execute();
		
		// Run 'status', this SHOULD NOT list the folder in the changed/new files
		changeSet = (new StatusOperation(config).execute()).getChangeSet();						
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);
		assertEquals(changeSet.getUnchangedFiles().size(), 1);		
				
		// Cleanup 
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
}
