package org.syncany.tests.operations;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;
import org.syncany.config.Config;
import org.syncany.config.EncryptionException;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginInfo;
import org.syncany.connection.plugins.Plugins;
import org.syncany.operations.SyncUpOperation;
import org.syncany.tests.util.TestUtil;

public class SyncUpOperationTest {
	private File tempLocalDir;
	private File tempRepoDir;
	private File tempCacheDir; 
	private File tempDBDir; 
	
	private final String machineName = "syncUpMachine1";

	@Before
	public void setUp() throws Exception {
		tempLocalDir = TestUtil.createTempDirectoryInSystemTemp();
		tempRepoDir = TestUtil.createTempDirectoryInSystemTemp();
		tempCacheDir = TestUtil.createTempDirectoryInSystemTemp();
		tempDBDir = TestUtil.createTempDirectoryInSystemTemp();
	}
	
	@After
	public void tearDown() throws Exception {
		TestUtil.deleteDirectory(tempLocalDir);
		TestUtil.deleteDirectory(tempRepoDir);
		TestUtil.deleteDirectory(tempCacheDir);
		TestUtil.deleteDirectory(tempDBDir);
	}

	@Test
	public void testUploadLocalDatabase() throws Exception {
		int fileSize = 1230;
		int fileAmount = 3;

		List<File> originalFiles = TestUtil.generateRandomBinaryFilesInDirectory(tempLocalDir, fileSize,
				fileAmount);
		
		Config config = createTestConfig();
		
		SyncUpOperation op = new SyncUpOperation(config);
		
		op.execute();

		//Compare dbs
		File localDatabaseFile = new File(tempLocalDir.getAbsoluteFile() + "/" + machineName);
		File remoteDatabaseFile = new File(tempRepoDir.getAbsoluteFile() + "/" + machineName);
		assertTrue(localDatabaseFile.exists());
		assertTrue(remoteDatabaseFile.exists());
		
		//compare files listed in db remote & local 
	}

	private Config createTestConfig() throws EncryptionException {
		Config config = new Config();
		config.setMachineName(machineName);
		config.setAppCacheDir(tempCacheDir);
		config.setAppDatabaseDir(tempDBDir);
		config.setLocalDir(tempLocalDir);
		
		Connection conn = createTestLocalConnection();
		
		config.setConnection(conn);
		return config;
	}

	private Connection createTestLocalConnection() {
		PluginInfo pluginInfo = Plugins.get("local");
		Map<String, String> pluginSettings = new HashMap<String, String>();
		pluginSettings.put("path", tempRepoDir.getAbsolutePath());

		Connection conn = pluginInfo.createConnection();
		return conn;
	}

}
