package org.syncany.tests.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.local.LocalConnection;

public class TestConfigUtil {
	public static Config createTestLocalConfig() throws Exception {
		File tempLocalDir = TestFileUtil.createTempDirectoryInSystemTemp();
		File tempAppDir = TestFileUtil.createTempDirectoryInSystemTemp();
		File tempAppCacheDir = TestFileUtil.createTempDirectoryInSystemTemp();
		File tempAppDatabaseDir = TestFileUtil.createTempDirectoryInSystemTemp();
		
		Config config = new Config("Password");
		config.setMachineName("MachineName"+Math.abs(new Random().nextInt()));
		config.setAppDir(tempAppDir);
		config.setAppCacheDir(tempAppCacheDir);
		config.setAppDatabaseDir(tempAppDatabaseDir);
		config.setLocalDir(tempLocalDir);
		
		Connection conn = createTestLocalConnection();		
		config.setConnection(conn);

		return config;		
	}
	
	public static Connection createTestLocalConnection() throws Exception {
		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp();

		Plugin plugin = Plugins.get("local");
		
		Map<String, String> pluginSettings = new HashMap<String, String>();
		pluginSettings.put("path", tempRepoDir.getAbsolutePath());

		Connection conn = plugin.createConnection();
		conn.init(pluginSettings);
		
		return conn;
	}	

	public static void deleteTestLocalConfigAndData(Config config) {
		TestFileUtil.deleteDirectory(config.getLocalDir());
		TestFileUtil.deleteDirectory(config.getAppDir());
		TestFileUtil.deleteDirectory(config.getAppCacheDir());
		TestFileUtil.deleteDirectory(config.getAppDatabaseDir());
		
		deleteTestLocalConnection(config);
	}

	private static void deleteTestLocalConnection(Config config) {
		LocalConnection connection = (LocalConnection) config.getConnection();
		TestFileUtil.deleteDirectory(connection.getRepositoryPath());		
	}
}
