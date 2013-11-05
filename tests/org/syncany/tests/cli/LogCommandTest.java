package org.syncany.tests.cli;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class LogCommandTest {	
	@Test
	public void testLogCommandNoArgs() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file1"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file2"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file3"), 20*1024);
						
		// Prepare (create some database entries)
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up",
			 "--force-checksum"
		}).start();
		
		TestFileUtil.changeRandomPartOfBinaryFile(new File(clientA.get("localdir")+"/file1"));

		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up",
			 "--force-checksum"
		}).start();		
		
		// Run 
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"log"
		}));
		
		assertEquals("Different number of output lines expected.", 4, cliOut.length);
		// TODO [low] How to test the log command any further? Non-deterministic output!
		
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}			
}
