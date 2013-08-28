package org.syncany.tests.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.syncany.config.Config;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.local.LocalConnection;

public class TestConfigUtil {
	private static final String RUNDATE = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
	
	public static Config createTestLocalConfig() throws Exception {
		return createTestLocalConfig("syncanyclient");
	}
	
	public static Config createTestLocalConfig(String machineName) throws Exception {
		return createTestLocalConfig(machineName, createTestLocalConnection());
	}
	
	public static Config createTestLocalConfig(String machineName, Connection connection) throws Exception {
		File tempClientDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("client-"+machineName, connection));
		File tempLocalDir = new File(tempClientDir+"/local");
		File tempAppDir = new File(tempClientDir+"/app"); // Warning: check delete method below if this is changed!
		File tempAppCacheDir =new File(tempAppDir+"/cache");
		File tempAppDatabaseDir = new File(tempAppDir+"/db");
		
		tempLocalDir.mkdirs();
		tempAppDir.mkdirs();
		tempAppCacheDir.mkdirs();
		tempAppDatabaseDir.mkdirs();
		
		Config config = new Config("Password");
		config.setMachineName(machineName+Math.abs(new Random().nextInt()));
		config.setAppDir(tempAppDir);
		config.setAppCacheDir(tempAppCacheDir);
		config.setAppDatabaseDir(tempAppDatabaseDir);
		config.setLocalDir(tempLocalDir);			
		config.setConnection(connection);

		return config;		
	}
	
	public static Connection createTestLocalConnection() throws Exception {
		Plugin plugin = Plugins.get("local");
		Connection conn = plugin.createConnection();

		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("repo", conn));
		
		Map<String, String> pluginSettings = new HashMap<String, String>();
		pluginSettings.put("path", tempRepoDir.getAbsolutePath());

		conn.init(pluginSettings);
		
		return conn;
	}	

	public static void deleteTestLocalConfigAndData(Config config) {
		TestFileUtil.deleteDirectory(config.getLocalDir());
		TestFileUtil.deleteDirectory(config.getAppDir());
		TestFileUtil.deleteDirectory(config.getAppCacheDir());
		TestFileUtil.deleteDirectory(config.getAppDatabaseDir());
		
		// TODO [low] workaround: delete empty parent folder of getAppDir() --> ROOT/app/.. --> ROOT/
		config.getAppDir().getParentFile().delete(); // if empty!
		
		deleteTestLocalConnection(config);
	}

	private static void deleteTestLocalConnection(Config config) {
		LocalConnection connection = (LocalConnection) config.getConnection();
		TestFileUtil.deleteDirectory(connection.getRepositoryPath());		
	}
	
	private static String createUniqueName(String name, Connection connection) {
		return String.format("syncany-%s-%d-%s", RUNDATE, connection.hashCode() % 1024, name);
	}
}
