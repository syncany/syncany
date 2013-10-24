package org.syncany.tests.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.local.LocalConnection;
import org.syncany.util.FileUtil;

public class TestConfigUtil {
	private static final String RUNDATE = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
	
	public static Map<String, String> createTestLocalConnectionSettings() throws Exception {
		Map<String, String> pluginSettings = new HashMap<String, String>();

		File tempRepoDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("repo", pluginSettings));		
		pluginSettings.put("path", tempRepoDir.getAbsolutePath());
		
		return pluginSettings;
	}
	
	public static Map<String, String> createTestLocalConfigFile(String machineName, Map<String, String> connectionProperties) throws Exception {
		Map<String, String> clientSettings = new HashMap<String, String>();
		
		File tempClientDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("client-"+machineName, connectionProperties));
		File tempLocalDir = new File(tempClientDir+"/local");
		File tempAppDir = new File(tempClientDir+"/app"); // Warning: check delete method below if this is changed!
		File tempCacheDir = new File(tempAppDir+"/cache");
		File tempDatabaseDir = new File(tempAppDir+"/db");
		File tempConfigFile = new File(tempAppDir+"/config.xml");
		
		tempLocalDir.mkdirs();
		tempAppDir.mkdirs();
		tempCacheDir.mkdirs();
		tempDatabaseDir.mkdirs();
		
		// Client settings 
		clientSettings.put("machinename", machineName);
		clientSettings.put("localdir", tempLocalDir.getAbsolutePath());
		clientSettings.put("appdir", tempAppDir.getAbsolutePath());
		clientSettings.put("cachedir", tempCacheDir.getAbsolutePath());
		clientSettings.put("databasedir", tempDatabaseDir.getAbsolutePath());

		clientSettings.put("repopath", connectionProperties.get("path"));
		clientSettings.put("configFile", tempConfigFile.getAbsolutePath());
		
		// Make config file from skeleton
		String configXmlSkel = FileUtils.readFileToString(new File("tests/config-local.xml.skel"));		
		String configXml = configXmlSkel;
		
		for (Map.Entry<String, String> clientSetting : clientSettings.entrySet()) {
			String quoteReplaced = clientSetting.getValue();
			configXml = configXml.replaceAll("\\$"+clientSetting.getKey(), quoteReplaced);
		}
		
		FileUtil.writeToFile(configXml.getBytes(), tempConfigFile);		
		
		return clientSettings;
	}	
	
	public static void deleteTestLocalConfigAndData(Map<String, String> clientSettings) throws ConfigException {		
		if (clientSettings.get("localDir") != null) TestFileUtil.deleteDirectory(new File(clientSettings.get("localDir")));
		if (clientSettings.get("cacheDir") != null) TestFileUtil.deleteDirectory(new File(clientSettings.get("cacheDir")));
		if (clientSettings.get("databaseDir") != null) TestFileUtil.deleteDirectory(new File(clientSettings.get("databaseDir")));
		if (clientSettings.get("configFile") != null) TestFileUtil.deleteDirectory(new File(clientSettings.get("configFile")));
		if (clientSettings.get("appDir") != null) TestFileUtil.deleteDirectory(new File(clientSettings.get("appDir")));
		if (clientSettings.get("repoPath") != null) TestFileUtil.deleteDirectory(new File(clientSettings.get("repoPath")));
	}
	
	public static Config createTestLocalConfig() throws Exception {
		return createTestLocalConfig("syncanyclient");
	}
	
	public static Config createTestLocalConfig(String machineName) throws Exception {
		return createTestLocalConfig(machineName, createTestLocalConnection());
	}
	
	public static Config createTestLocalConfig(String machineName, Connection connection) throws Exception {
		File tempClientDir = TestFileUtil.createTempDirectoryInSystemTemp(createUniqueName("client-"+machineName, connection));
		File tempLocalDir = new File(tempClientDir+"/local");
		File tempAppDir = new File(tempClientDir+"/app");
		File tempCacheDir = new File(tempClientDir+"/app/cache");
		File tempDatabaseDir = new File(tempClientDir+"/app/db");
		File tempLogDir = new File(tempClientDir+"/app/logs");
		
		tempLocalDir.mkdirs();
		tempAppDir.mkdirs();
		tempCacheDir.mkdirs();
		tempDatabaseDir.mkdirs();
		tempLogDir.mkdirs();
		
		// Create Repo TO
		RepoTO repoTO = new RepoTO();
		
		repoTO.setChunker(null); // TODO [low] Chunker not configurable right now. Not used.
		repoTO.setMultiChunker(null); // TODO [low] Chunker not configurable right now. Not used.
		repoTO.setTransformers(null);
		
		// Create config TO
		ConfigTO configTO = new ConfigTO();
		
		configTO.setMachineName(machineName+Math.abs(new Random().nextInt()));
		configTO.setCacheDir(tempCacheDir.getAbsolutePath());
		configTO.setDatabaseDir(tempDatabaseDir.getAbsolutePath());
		configTO.setLocalDir(tempLocalDir.getAbsolutePath());
		configTO.setLogDir(tempLogDir.getAbsolutePath());
		configTO.setAppDir(tempAppDir.getAbsolutePath());
		
		configTO.setPassword(null);
		
		
		// Skip configTO.setConnection()		
		
		Config config = new Config(configTO, repoTO);
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
		TestFileUtil.deleteDirectory(config.getCacheDir());
		TestFileUtil.deleteDirectory(config.getDatabaseDir());

		if (config.getAppDir() != null) {
			TestFileUtil.deleteDirectory(config.getAppDir());
		}
		
		// TODO [low] workaround: delete empty parent folder of getAppDir() --> ROOT/app/.. --> ROOT/
		config.getLocalDir().getParentFile().delete(); // if empty!
		
		deleteTestLocalConnection(config);
	}

	private static void deleteTestLocalConnection(Config config) {
		LocalConnection connection = (LocalConnection) config.getConnection();
		TestFileUtil.deleteDirectory(connection.getRepositoryPath());		
	}
	
	private static String createUniqueName(String name, Object uniqueHashObj) {
		return String.format("syncany-%s-%d-%s", RUNDATE, uniqueHashObj.hashCode() % 1024, name);
	}

}
