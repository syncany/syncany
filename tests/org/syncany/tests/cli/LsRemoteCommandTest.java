package org.syncany.tests.cli;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class LsRemoteCommandTest {	
	@Test
	public void testLsRemoteCommand() throws Exception {
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		Map<String, String> clientB = TestCliUtil.createLocalTestEnvAndConnect("B", connectionSettings);

		// Round 1: No changes / remote databases expected
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls-remote"
		}));
		
		assertEquals("Different number of output lines expected.", 1, cliOut.length);
		
		// Round 2: One new database expected
		TestFileUtil.createRandomFile(new File(clientB.get("localdir")+"/file1"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientB.get("localdir")+"/file2"), 20*1024);
		
		new CommandLineClient(new String[] { 
			 "--localdir", clientB.get("localdir"),
			 "up",
		}).start();
		
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientA.get("localdir"),
			"ls-remote"
		}));
		
		assertEquals("Different number of output lines expected.", 1, cliOut.length);
		assertEquals("? db-B-1", cliOut[0]);
		
		TestCliUtil.deleteTestLocalConfigAndData(clientA);
		TestCliUtil.deleteTestLocalConfigAndData(clientB);
	}	
}
