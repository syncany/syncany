package org.syncany.tests.operations;

import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;
import org.syncany.operations.DownOperation;
import org.syncany.operations.UpOperation;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class OperationPerformanceTest {
	private static final Logger logger = Logger.getLogger(OperationPerformanceTest.class.getSimpleName());

	@Test
	public void testOperationPerformance() throws Exception {
		// Setup
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		Config configA = TestConfigUtil.createTestLocalConfig("A", testConnection);
		Config configB = TestConfigUtil.createTestLocalConfig("B", testConnection);
		
		// Add new files on A and upload it 
		TestFileUtil.createRandomFilesInDirectory(configA.getLocalDir(), 5000*1024, 3);
		
		long timeSyncUpStart = System.currentTimeMillis();
		new UpOperation(configA).execute();		
		long timeSyncUpEnd = System.currentTimeMillis();		
		long timeSyncUpTotal = timeSyncUpEnd - timeSyncUpStart;
		
		if (timeSyncUpTotal > 3000) {
			fail("Sync up took: "+timeSyncUpTotal+" ms");
		}
		
		logger.log(Level.INFO, "Sync up performance: "+timeSyncUpTotal+" ms");
		
		// Sync down B
		long timeSyncDownStart = System.currentTimeMillis();
		new DownOperation(configB).execute();
		long timeSyncDownEnd = System.currentTimeMillis();		
		long timeSyncDownTotal = timeSyncDownEnd - timeSyncDownStart;
		
		if (timeSyncDownTotal > 3000) {
			fail("Sync down took: "+timeSyncDownTotal+" ms");
		}
		
		logger.log(Level.INFO, "Sync down performance: "+timeSyncDownTotal+" ms");		
		
		// Cleanup
		TestConfigUtil.deleteTestLocalConfigAndData(configA);
	}	
}
