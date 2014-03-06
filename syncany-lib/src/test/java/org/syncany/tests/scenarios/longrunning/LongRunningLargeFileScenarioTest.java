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
package org.syncany.tests.scenarios.longrunning;

import static junit.framework.Assert.assertEquals;
import static org.syncany.tests.util.TestAssertUtil.assertSqlDatabaseEquals;

import java.util.Date;

import org.junit.Test;
import org.syncany.config.Logging;
import org.syncany.connection.plugins.Connection;
import org.syncany.tests.util.TestClient;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;
import org.syncany.util.StringUtil;

public class LongRunningLargeFileScenarioTest {
	private static final long SIZE_800_MB = 800L*1024*1024;
	private static final long SIZE_2500_MB = 2500L*1024*1024;
	
	@Test
	public void test800MBFile() throws Exception {
		testLargeFile(SIZE_800_MB);
	}

	@Test
	public void test2500MBFile() throws Exception {
		// Note: This has caused problems in the past, because it exceeds the integer
		//       length. Sizes are now long, but keep this test!
		
		testLargeFile(SIZE_2500_MB);
	}
	
	private void testLargeFile(long size) throws Exception {
		// Setup 
		Connection testConnection = TestConfigUtil.createTestLocalConnection();
		
		TestClient clientA = new TestClient("A", testConnection);
		TestClient clientB = new TestClient("B", testConnection);			
		
		Logging.disableLogging();
		
		logln("NOTE: This test can take several minutes!");
		logln("Disabling console logging for this test; Too much output/overhead.");
		logln("");		
		
		// A		
		logln("Creating large file for scenario ("+((long) size/1024L/1024L)+" MB) ...");
		clientA.createNewFile("A-large-file.zip", size);	
		logln("Done creating large file. Now creating checksum ...");		
		String checksumFileA = StringUtil.toHex(TestFileUtil.createChecksum(clientA.getLocalFile("A-large-file.zip")));
		logln("Done. Checksum is: "+checksumFileA);	
		
		logln("clientA.up() started.");
		clientA.up();		
		logln("clientA.up() completed.");

		logln("Freeing up space, deleting file at client A ...");
		clientA.deleteFile("A-large-file.zip");
		logln("Deleting done.");		
		
		// B
		logln("clientB.down(); started.");
		clientB.down(); 
		logln("clientB.down(); completed.");
		String checksumFileB = StringUtil.toHex(TestFileUtil.createChecksum(clientB.getLocalFile("A-large-file.zip")));
		logln("Done. Checksum is: "+checksumFileB);	
		
		logln("Freeing up space, deleting file at client B ...");
		clientB.deleteFile("A-large-file.zip");
		logln("Deleting done.");				
		
		try {
			assertEquals("File checksum should be equal. ", checksumFileA, checksumFileB);
			assertSqlDatabaseEquals(clientA.getDatabaseFile(), clientB.getDatabaseFile());
		}
		finally {
			// Tear down
			clientA.deleteTestData();
			clientB.deleteTestData();
		}	
	}	
	
	private void logln(String s) {
		System.out.println(new Date()+" "+s);
	}
}
