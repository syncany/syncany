package org.syncany.tests.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.config.Config;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;

public class StatusCommandTest {		
	@Test
	public void testStatusCommandWithLogLevelOff() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);

		// Run!
		new File(clientA.get("localdir")+"/somefolder1").mkdir();
		new File(clientA.get("localdir")+"/somefolder2").mkdir();
				
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] { 
			"--loglevel", "OFF", 
			"--localdir", clientA.get("localdir"),
			"status" 
		}));
		
		// Test
		assertEquals("Different number of output lines expected.", 2, cliOut.length);
		assertEquals("? somefolder1", cliOut[0]);
		assertEquals("? somefolder2", cliOut[1]);
		// TODO [medium] This test case does NOT test the loglevel option
		
		// Cleanup
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}	
	
	@Test
	public void testStatusCommandWithLogFile() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);

		File tempLogFile = new File(clientA.get("localdir")+"/"+Config.DEFAULT_DIR_APPLICATION
				+"/"+Config.DEFAULT_DIR_LOG+"/templogfile");
		
		// Run!
		new File(clientA.get("localdir")+"/somefolder1").mkdir();
		new File(clientA.get("localdir")+"/somefolder2").mkdir();
				
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] { 
			"--log", tempLogFile.getAbsolutePath(), 
			"--localdir", clientA.get("localdir"),
			"status"
		}));
		
		// Test		
		assertTrue("Log file should exist.", tempLogFile.exists());
		assertEquals(2, cliOut.length);
		assertEquals("? somefolder1", cliOut[0]);
		assertEquals("? somefolder2", cliOut[1]);
		
		// Cleanup
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}			
}
