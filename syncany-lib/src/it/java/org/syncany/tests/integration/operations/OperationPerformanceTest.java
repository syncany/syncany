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

import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.up.UpOperation;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.tests.util.TestConfigUtil;

public class OperationPerformanceTest {
	private static final Logger logger = Logger.getLogger(OperationPerformanceTest.class.getSimpleName());

	@Test
	public void testOperationPerformance() throws Exception {
		// Setup
		TransferSettings testConnection = TestConfigUtil.createTestLocalConnection();
		
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
		TestConfigUtil.deleteTestLocalConfigAndData(configB);
	}	
}
