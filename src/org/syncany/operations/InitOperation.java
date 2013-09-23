package org.syncany.operations;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.ConfigTO;
import org.syncany.config.ConfigTO.ConnectionSettings;
import org.syncany.config.ConfigTO.EncryptionSettings;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;

public class InitOperation extends Operation {
	private static final Logger logger = Logger.getLogger(InitOperation.class.getSimpleName());	
	private InitOperationOptions options;
	
	public InitOperation(InitOperationOptions options) {
		super(null);
		this.options = options;
	}	
		
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Init'");
		logger.log(Level.INFO, "--------------------------------------------");
				
		// Create config transfer object
		ConfigTO configTO = new ConfigTO();
		
		configTO.setMachineName(createMachineName());
		configTO.setConnection(createConnectionSettings());
		configTO.setEncryption(createEncryptionSettings());
		
		// Save skeleton config
		File skelConfigFile = new File(options.getLocation()+"/.syncany/config.json"); // TODO [low] duplicate code
		File appDir = skelConfigFile.getParentFile();
		
		if (!appDir.exists()) {
			appDir.mkdir();
		}
		
		ConfigTO.save(configTO, skelConfigFile);
		
		return new InitOperationResult(skelConfigFile);
	}		
	
	private String createMachineName() throws UnknownHostException {
		SecureRandom random = new SecureRandom();
		return InetAddress.getLocalHost().getHostName()+System.getProperty("user.name")+Math.abs(random.nextInt());
	}

	private ConnectionSettings createConnectionSettings() {
		String connectionType = options.getPlugin();
		Map<String, String> connectionSettingsMap = new HashMap<String, String>();
		
		Plugin plugin = Plugins.get(options.getPlugin());		
		Connection connection = plugin.createConnection();
		
		for (String settingKey : connection.getMandatorySettings()) {
			connectionSettingsMap.put(settingKey, "(mandatory)");
		}
		
		for (String settingKey : connection.getOptionalSettings()) {
			connectionSettingsMap.put(settingKey, "(optional)");
		}
		
		return new ConnectionSettings(connectionType, connectionSettingsMap);				
	}
	
	private EncryptionSettings createEncryptionSettings() {
		SecureRandom random = new SecureRandom();
		return new EncryptionSettings(true, "(change this, "+Math.abs(random.nextInt())+")");
	}

	public static class InitOperationOptions implements OperationOptions {
		private String plugin;
		private File location;

		public String getPlugin() {
			return plugin;
		}

		public void setPlugin(String plugin) {
			this.plugin = plugin;
		}
		
		public File getLocation() {
			return location;
		}

		public void setLocation(File location) {
			this.location = location;
		}		
	}
	
	public class InitOperationResult implements OperationResult {
		private File skelConfigFile;
		
		public InitOperationResult(File skelConfigFile) {
			this.skelConfigFile = skelConfigFile;
		}

		public File getSkelConfigFile() {
			return skelConfigFile;
		}
	}
}
