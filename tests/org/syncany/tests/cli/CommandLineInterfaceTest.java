package org.syncany.tests.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.config.Config;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.util.StringUtil;

public class CommandLineInterfaceTest {	
	private static final Logger logger = Logger.getLogger(CommandLineInterfaceTest.class.getSimpleName());
	
	@Test
	public void testCliInitAndConnect() throws Exception {
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnv("A", connectionSettings);
		Map<String, String> clientB = TestCliUtil.createLocalTestEnv("B", connectionSettings);

		// Init
		String[] initArgs = new String[] { 			 
			 "--localdir", clientA.get("localdir"),
			 "init",
			 "--plugin", "local", 
			 "--plugin-option", "path="+clientA.get("repopath"),
			 "--no-encryption", 
			 "--no-gzip" 
		}; 
		
		logger.log(Level.INFO, "Running syncany with argument: "+StringUtil.join(initArgs, " "));		
		new CommandLineClient(initArgs).start();

		assertTrue("Repo file in repository should exist.", new File(clientA.get("repopath")+"/repo").exists());
		assertTrue("Repo file in local client should exist.", new File(clientA.get("localdir")+"/"+Config.DEFAULT_DIR_APPLICATION+"/"+Config.DEFAULT_FILE_REPO).exists());
		assertTrue("Config file in local client should exist.", new File(clientA.get("localdir")+"/"+Config.DEFAULT_DIR_APPLICATION+"/"+Config.DEFAULT_FILE_CONFIG).exists());
				
		// Connect
		String[] connectArgs = new String[] { 			 
			 "connect",
			 "--localdir", clientB.get("localdir"),
			 "--plugin", "local", 
			 "--plugin-option", "path="+clientB.get("repopath"),
		};
		
		logger.log(Level.INFO, "Running syncany with argument: "+StringUtil.join(connectArgs, " "));		
		new CommandLineClient(connectArgs).start();

		assertTrue("Repo file in local client should exist.", new File(clientB.get("localdir")+"/"+Config.DEFAULT_DIR_APPLICATION+"/"+Config.DEFAULT_FILE_REPO).exists());
		assertTrue("Config file in local client should exist.", new File(clientB.get("localdir")+"/"+Config.DEFAULT_DIR_APPLICATION+"/"+Config.DEFAULT_FILE_CONFIG).exists());

		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
		TestCliUtil.deleteTestLocalConfigAndData(clientB);		
	}	
	 
	@Test
	public void testAppFoldersExist() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);

		// Run!
		new File(clientA.get("localdir")+"/somefolder").mkdir();
	
		String[] cliOut = TestCliUtil.runAndCaptureOutput(new CommandLineClient(new String[] { 
			"--localdir", clientA.get("localdir"),
			"up",
			"--no-cleanup" 
		}));
		
		
		// Test folder existence
		File appFolder = new File(clientA.get("localdir")+"/"+Config.DEFAULT_DIR_APPLICATION);
		File logFolder = new File(appFolder+"/"+Config.DEFAULT_DIR_LOG);
		File dbFolder = new File(appFolder+"/"+Config.DEFAULT_DIR_DATABASE);
		File cacheFolder = new File(appFolder+"/"+Config.DEFAULT_DIR_CACHE);
		
		assertTrue("App folder should exist", appFolder.exists());
		assertTrue("Logs folder should exist", logFolder.exists());
		assertTrue("Log file should exist", logFolder.list().length > 0);
		assertTrue("Database folder should exist", dbFolder.exists());
		assertTrue("Cache folder should exist", cacheFolder.exists());
				
		// Test output
		assertEquals("Different number of output lines expected.", 2, cliOut.length);
		assertEquals("A somefolder", cliOut[0]);
		
		// Cleanup
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}		
}
