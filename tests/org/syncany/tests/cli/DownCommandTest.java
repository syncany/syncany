package org.syncany.tests.cli;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class DownCommandTest {	
	@Test
	public void testDownCommandNoArgs() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		Map<String, String> clientB = TestCliUtil.createLocalTestEnvAndConnect("B", connectionSettings);
		
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file1"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file2"), 20*1024);
		TestFileUtil.createRandomFile(new File(clientA.get("localdir")+"/file3"), 20*1024);
				
		// Round 1: No changes
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientB.get("localdir"),
			"down"
		}));
		
		assertEquals("Different number of output lines expected.", 1, cliOut.length);
		
		// Round 2: Only added files
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up" 
		}).start();
		
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientB.get("localdir"),
			"down"
		}));
		
		assertEquals("Different number of output lines expected.", 4, cliOut.length);
		assertEquals("A file1", cliOut[0]);
		assertEquals("A file2", cliOut[1]);
		assertEquals("A file3", cliOut[2]);		
		
		// Round 3: Modified and deleted files
		TestFileUtil.changeRandomPartOfBinaryFile(new File(clientA.get("localdir")+"/file2"));
		new File(clientA.get("localdir")+"/file3").delete();
		
		new CommandLineClient(new String[] { 
			 "--localdir", clientA.get("localdir"),
			 "up",
			 "--force-checksum"
		}).start();
		
		cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] {
			"--localdir", clientB.get("localdir"),
			"down"
		}));
		
		assertEquals("Different number of output lines expected.", 3, cliOut.length);
		assertEquals("M file2", cliOut[0]);
		assertEquals("D file3", cliOut[1]);
		
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
		TestCliUtil.deleteTestLocalConfigAndData(clientB);
	}			
}
