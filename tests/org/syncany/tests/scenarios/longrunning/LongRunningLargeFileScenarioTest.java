package org.syncany.tests.scenarios.longrunning;

import static junit.framework.Assert.assertEquals;
import static org.syncany.tests.util.TestAssertUtil.assertDatabaseFileEquals;

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
			assertDatabaseFileEquals(clientA.getLocalDatabaseFile(), clientB.getLocalDatabaseFile(), clientA.getConfig().getTransformer());
		}
		finally {
			// Tear down
			clientA.cleanup();
			clientB.cleanup();
		}	
	}	
	
	private void logln(String s) {
		System.out.println(new Date()+" "+s);
	}
}
