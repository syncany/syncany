package org.syncany.tests.operations;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.operations.StatusOperation;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.SyncUpOperationOptions;
import org.syncany.operations.UpOperation;
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
		ChangeSet changeSet = ((StatusOperationResult) new StatusOperation(config).execute()).getChangeSet();				
		
		assertEquals(changeSet.getNewFiles().size(), originalFiles.size());
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);
				
		// Up
		new UpOperation(config).execute();		
				
		// Status
		changeSet = ((StatusOperationResult) new StatusOperation(config).execute()).getChangeSet();		
		
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);

		// Change all files, run 'status'
		Thread.sleep(1000); // TODO [low] StatusOperation relies on file modified time and size, any other methods?

		for (File file : originalFiles) {
			TestFileUtil.changeRandomPartOfBinaryFile(file);
		}		
		
		changeSet = ((StatusOperationResult) new StatusOperation(config).execute()).getChangeSet();
		
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), originalFiles.size());
		assertEquals(changeSet.getDeletedFiles().size(), 0);
		
		// Up
		new UpOperation(config).execute();
				
		// Delete all files, run 'status' again
		for (File file : originalFiles) {
			TestFileUtil.deleteFile(file);
		}
				
		changeSet = ((StatusOperationResult) new StatusOperation(config).execute()).getChangeSet();
		
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

		SyncUpOperationOptions syncUpOptions = new SyncUpOperationOptions();
		syncUpOptions.setStatusOptions(statusOptions);			
		
		// Perform 'up' and immediately change test file
		// IMPORTANT: Do NOT sleep to enforce checksum-based comparison in 'status'
		new UpOperation(config, null, syncUpOptions).execute();		
		TestFileUtil.changeRandomPartOfBinaryFile(testFile);
		
		// Run 'status', this should run a checksum-based file comparison
		ChangeSet changeSet = ((StatusOperationResult) new StatusOperation(config, null, statusOptions).execute()).getChangeSet();						
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
		ChangeSet changeSet = ((StatusOperationResult) new StatusOperation(config).execute()).getChangeSet();						
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
		ChangeSet changeSet = ((StatusOperationResult) new StatusOperation(config).execute()).getChangeSet();						
		assertEquals(changeSet.getNewFiles().size(), 1);
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);
		assertEquals(changeSet.getUnchangedFiles().size(), 0);	
		
		// Run 'up' to check in the folder
		new UpOperation(config).execute();
		
		// Run 'status', this SHOULD NOT list the folder in the changed/new files
		changeSet = ((StatusOperationResult) new StatusOperation(config).execute()).getChangeSet();						
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);
		assertEquals(changeSet.getUnchangedFiles().size(), 1);		
				
		// Cleanup 
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
}
