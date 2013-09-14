package org.syncany.tests.operations;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseXmlDAO;
import org.syncany.database.FileVersion;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.StatusOperation;
import org.syncany.operations.SyncUpOperation;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class StatusOperationTest {

	@Test
	public void testStatusOperation() throws Exception {
		// Setup
		Config config = TestConfigUtil.createTestLocalConfig();
		
		// Run
		List<File> originalFiles = TestFileUtil.createRandomFilesInDirectory(config.getLocalDir(), 500*1024, 3);
		
		StatusOperation op = new StatusOperation(config);		
		op.execute();
		
		fail("Some asserts here.");
		
		// Cleanup
		TestConfigUtil.deleteTestLocalConfigAndData(config);
	}
	
}
