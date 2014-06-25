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
package org.syncany.tests.plugins.unreliable_local;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.unreliable_local.UnreliableLocalConnection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;

public class UploadInterruptedTest {
	private static final Logger logger = Logger.getLogger(UploadInterruptedTest.class.getSimpleName());
	
	// TODO [medium] Design and implement specific tests for scenarios where uploads fail
	@Test
	public void testUnreliableUpload() throws Exception {
		// Setup 
		UnreliableLocalConnection testConnection = TestConfigUtil.createTestUnreliableLocalConnection(
			Arrays.asList(new String[] { 
				// List of failing operations (regex)
				// Format: abs=<count> rel=<count> op=<connect|init|upload|...> <operation description>
					
				// 1st upload (= multichunk) fails	
				"rel=[234] .+upload.+multichunk", // << 3 retries!!
				
				// Make fourth upload fail
				"rel=[678] .+upload" // << 3 retries!! 
					
			}
		));
		
		TestClient clientA = new TestClient("A", testConnection);
		
		int i = 0;
		while (i++ < 5) {
			clientA.createNewFile("A-original-"+i, 50*1024);
			try {
				Thread.sleep(100);
				clientA.up();
				clientA.down();
			}
			catch (StorageException e) {
				logger.log(Level.INFO, e.getMessage());
			}
		}
				
		assertTrue(new File(testConnection.getRepositoryPath()+"/databases/db-A-0000000001").exists());
		assertTrue(new File(testConnection.getRepositoryPath()+"/databases/db-A-0000000002").exists());
		assertTrue(new File(testConnection.getRepositoryPath()+"/databases/db-A-0000000003").exists());
		assertFalse(new File(testConnection.getRepositoryPath()+"/databases/db-A-0000000004").exists());
		assertFalse(new File(testConnection.getRepositoryPath()+"/databases/db-A-0000000005").exists());
		
		// Tear down
		clientA.deleteTestData();
	}				
}
