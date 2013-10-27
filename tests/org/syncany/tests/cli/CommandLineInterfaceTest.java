package org.syncany.tests.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.output.ByteArrayOutputStream;
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
			 "init",
			 "--folder", clientA.get("localdir"),
			 "--plugin", "local", 
			 "--plugin-option", "path="+clientA.get("repopath"),
			 "--no-encryption", 
			 "--no-gzip" 
		}; 
		
		logger.log(Level.INFO, "Running syncany with argument: "+StringUtil.join(initArgs, " "));		
		new CommandLineClient(initArgs).start();

		assertTrue("Repo file in repository should exist.", new File(clientA.get("repopath")+"/repo").exists());
		assertTrue("Repo file in local client should exist.", new File(clientA.get("localdir")+"/.syncany/repo").exists());
		assertTrue("Config file in local client should exist.", new File(clientA.get("configfile")).exists());
				
		// Connect
		String[] connectArgs = new String[] { 			 
			 "connect",
			 "--folder", clientB.get("localdir"),
			 "--plugin", "local", 
			 "--plugin-option", "path="+clientB.get("repopath"),
		};
		
		logger.log(Level.INFO, "Running syncany with argument: "+StringUtil.join(connectArgs, " "));		
		new CommandLineClient(connectArgs).start();

		assertTrue("Repo file in local client should exist.", new File(clientB.get("repofile")).exists());
		assertTrue("Config file in local client should exist.", new File(clientB.get("configfile")).exists());

		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
		TestCliUtil.deleteTestLocalConfigAndData(clientB);		
	}	
	 
	@Test
	public void testCliSyncUpWithNoCleanup() throws Exception {
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);

		new CommandLineClient(new String[] { 
				 "--folder", clientA.get("localdir"), "up", "--no-cleanup" }).start();

		for (int i=1; i<=20; i++) {
			new File(clientA.get("localDir")+"/somefolder"+i).mkdir();

			new CommandLineClient(new String[] { 
					 "--config", clientA.get("configfile"), "up", "--no-cleanup" }).start();
		}
		
		for (int i=1; i<=20; i++) {
			File databaseFileInRepo = new File(connectionSettings.get("path")+"/db-A-"+i);			
			assertTrue("Database file SHOULD exist: "+databaseFileInRepo, databaseFileInRepo.exists());
		}
				
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}	
	
	// TODO [low] write test for default config settings
	// TODO [low] write test for init operation
	
	@Test
	public void testAppFoldersExist() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		ByteArrayOutputStream cliOut = new ByteArrayOutputStream();

		// Run!
		new File(clientA.get("localDir")+"/somefolder").mkdir();
	
		CommandLineClient cli = new CommandLineClient(new String[] { 
				"--config", clientA.get("configfile"), "up", "--no-cleanup" });
		
		cli.setOut(cliOut);
		cli.start();
		
		logger.log(Level.INFO, "CLI output: ");
		logger.log(Level.INFO, toString(cliOut));		
		
		// Test folder existence
		File appFolder = new File(clientA.get("configfile")).getParentFile();
		File logFolder = new File(appFolder+"/"+Config.DEFAULT_DIR_LOG);
		File dbFolder = new File(appFolder+"/"+Config.DEFAULT_DIR_DATABASE);
		File cacheFolder = new File(appFolder+"/"+Config.DEFAULT_DIR_CACHE);
		
		assertTrue("App folder should exist", appFolder.exists());
		assertTrue("Logs folder should exist", logFolder.exists());
		assertTrue("Log file should exist", logFolder.list().length > 0);
		assertTrue("Database folder should exist", dbFolder.exists());
		assertTrue("Cache folder should exist", cacheFolder.exists());
				
		// Test output
		String out[] = toStringArray(cliOut);

		assertEquals("Different number of output lines expected.", 2, out.length);
		assertEquals("A somefolder", out[0]);
		
		// Cleanup
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}	
	
	@Test
	public void testSyncanyCliWithLogLevelOff() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		ByteArrayOutputStream cliOut = new ByteArrayOutputStream();

		// Run!
		new File(clientA.get("localDir")+"/somefolder1").mkdir();
		new File(clientA.get("localDir")+"/somefolder2").mkdir();
				
		CommandLineClient cli = new CommandLineClient(new String[] { 
				"--loglevel", "OFF", 
				"--config", clientA.get("configfile"), "status" });
		
		cli.setOut(cliOut);
		cli.start();
		
		logger.log(Level.INFO, "CLI output: ");
		logger.log(Level.INFO, toString(cliOut));			
		
		// Test output
		String out[] = toStringArray(cliOut);

		assertEquals("Different number of output lines expected.", 2, out.length);
		assertEquals("? somefolder1", out[0]);
		assertEquals("? somefolder2", out[1]);
		
		// Cleanup
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}	
	
	@Test
	public void testSyncanyCliWithLogFile() throws Exception {
		// Setup
		Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		ByteArrayOutputStream cliOut = new ByteArrayOutputStream();

		File tempLogFile = new File(clientA.get("appDir")+"/log");
		
		// Run!
		new File(clientA.get("localDir")+"/somefolder1").mkdir();
		new File(clientA.get("localDir")+"/somefolder2").mkdir();
				
		CommandLineClient cli = new CommandLineClient(new String[] { 
				"--log", tempLogFile.getAbsolutePath(), 
				"--config", clientA.get("configfile"), "status" });
		
		cli.setOut(cliOut);
		cli.start();
		
		logger.log(Level.INFO, "CLI output: ");
		logger.log(Level.INFO, toString(cliOut));		

		// Test		
		assertTrue("Log file should exist.", tempLogFile.exists());
		
		// Cleanup
		TestCliUtil.deleteTestLocalConfigAndData(clientA);		
	}		
	
	public String toString(ByteArrayOutputStream bos) {
		return new String(bos.toByteArray());
	}
	
	public String[] toStringArray(ByteArrayOutputStream bos) {		
		return toString(bos).split("[\\r\\n]+|[\\n\\r]+|[\\n]+");
	}
}
