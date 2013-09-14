package org.syncany.tests.operations;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.operations.StatusOperation;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.SyncUpOperation;
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
		StatusOperation statusOperation = new StatusOperation(config);		
		ChangeSet changeSet = ((StatusOperationResult) statusOperation.execute()).getChangeSet();				
		
		assertEquals(changeSet.getNewFiles().size(), originalFiles.size());
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);
				
		// Up
		new SyncUpOperation(config).execute();		
				
		// Status
		changeSet = ((StatusOperationResult) statusOperation.execute()).getChangeSet();		
		
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), 0);

		// Change all files, run 'status'
		Thread.sleep(1000); // TODO [low] StatusOperation relies on file modified time and size, any other methods?

		for (File file : originalFiles) {
			TestFileUtil.changeRandomPartOfBinaryFile(file);
		}		
		
		changeSet = ((StatusOperationResult) statusOperation.execute()).getChangeSet();
		
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), originalFiles.size());
		assertEquals(changeSet.getDeletedFiles().size(), 0);
		
		// Up
		new SyncUpOperation(config).execute();
				
		// Delete all files, run 'status' again
		for (File file : originalFiles) {
			TestFileUtil.deleteFile(file);
		}
				
		changeSet = ((StatusOperationResult) statusOperation.execute()).getChangeSet();
		
		assertEquals(changeSet.getNewFiles().size(), 0);
		assertEquals(changeSet.getChangedFiles().size(), 0);
		assertEquals(changeSet.getDeletedFiles().size(), originalFiles.size());
				
		// Cleanup
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
}
