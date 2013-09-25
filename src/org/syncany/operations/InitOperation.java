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
		initMachineName();
		initConnectionSettings();
		initEncryptionSettings();
				
		// Create local .syncany directory
		File skelConfigFile = new File(options.getLocalDir()+"/.syncany/config.json"); // TODO [low] duplicate code
		File appDir = skelConfigFile.getParentFile();
		
		if (!appDir.exists()) {
			appDir.mkdir();
		}
		
		// Unset 'localDir' (we don't want to save it!)
		options.setLocalDir(null);
		
		// Save skeleton config
		ConfigTO.save(options, skelConfigFile);
		
		return new InitOperationResult(skelConfigFile);
	}		
	
	private void initMachineName() throws UnknownHostException {
		SecureRandom random = new SecureRandom();
		options.setMachineName(InetAddress.getLocalHost().getHostName()+System.getProperty("user.name")+Math.abs(random.nextInt()));
	}

	private void initConnectionSettings() {
		Map<String, String> connectionSettings = options.getConnection().getSettings();
		
		if (connectionSettings == null) {
			connectionSettings = new HashMap<String, String>();
			
			Plugin plugin = Plugins.get(options.getConnection().getType());		
			Connection connection = plugin.createConnection();

			for (String settingKey : connection.getMandatorySettings()) {
				if (!connectionSettings.containsKey(settingKey)) {
					connectionSettings.put(settingKey, "(mandatory)");
				}
			}
			
			for (String settingKey : connection.getOptionalSettings()) {
				connectionSettings.put(settingKey, "(optional)");
			}
			
			options.setConnection(new ConnectionSettings(options.getConnection().getType(), connectionSettings));
		}						
	}
	
	private void initEncryptionSettings() {
		EncryptionSettings encryptionSettings = options.getEncryption();
		
		if (encryptionSettings == null) {
			SecureRandom random = new SecureRandom();
			options.setEncryption(new EncryptionSettings(true, "(this is the password, CHANGE IT! "+Math.abs(random.nextInt())+")"));
		}
	}

	public static class InitOperationOptions extends ConfigTO implements OperationOptions {
		// Inherited ConfigTO
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
