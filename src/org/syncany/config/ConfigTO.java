package org.syncany.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.syncany.config.Config.ConfigException;
import org.syncany.util.FileUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigTO {
	private String machineName;
	private String configFile;
	private String localDir;
	private String appDir;
	private String databaseDir;
	private String cacheDir;
	private String logDir;	
	private ConnectionSettings connection;
	private EncryptionSettings encryption;
	
	public static ConfigTO load(File file) throws ConfigException {
		try {
			ConfigTO configTO = new Gson().fromJson(FileUtil.readFileToString(file), ConfigTO.class);
			configTO.configFile = file.getAbsolutePath();
			
			return configTO;
		}
		catch (Exception ex) {
			throw new ConfigException("Config file does not exist or is invalid: "+file, ex);
		}
	}	
	
	public static void save(ConfigTO configTO, File file) throws IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String configFileStr = gson.toJson(configTO);
		
		FileUtil.writeToFile(configFileStr.getBytes(), file);
	}
		
	public String getConfigFile() {
		return configFile;
	}
	
	public String getAppDir() {
		return appDir;
	}

	public String getLocalDir() {
		return localDir;
	}
	
	public String getCacheDir() {
		return cacheDir;
	}
	
	public String getLogDir() {
		return logDir;
	}

	public ConnectionSettings getConnection() {
		return connection;
	}
	
	public String getMachineName() {
		return machineName;
	}

	public EncryptionSettings getEncryption() {
		return encryption;
	}
	
	public static class ConnectionSettings {
	    private String type;
	    private Map<String, String> settings;
	    
	    public ConnectionSettings(String type, Map<String, String> settings) {
			this.type = type;
			this.settings = settings;
		}
	    
		public String getType() {
			return type;
		}
		public Map<String, String> getSettings() {
			return settings;
		}
	}
	
	public static class EncryptionSettings {
		private Boolean enabled;
	    private String pass;
	    private String cipherStr;
	    private Integer keySize;
	    private Boolean ivNeeded;
	    private Boolean unlimitedNeeded;
	    
	    public EncryptionSettings(Boolean enabled, String pass) {
	    	this(enabled, pass, null, null, null, null);
	    }
	    
	    public EncryptionSettings(Boolean enabled, String pass, String cipherStr, Integer keySize, Boolean ivNeeded, Boolean unlimitedNeeded) {
			this.enabled = enabled;
			this.pass = pass;
			this.cipherStr = cipherStr;
			this.keySize = keySize;
			this.ivNeeded = ivNeeded;
			this.unlimitedNeeded = unlimitedNeeded;
		}

		public Boolean isEnabled() {
			return enabled;
		}
		
		public String getPass() {
			return pass;
		}

		public String getCipherStr() {
			return cipherStr;
		}

		public Integer getKeySize() {
			return keySize;
		}

		public Boolean isIvNeeded() {
			return ivNeeded;
		}		
		
		public Boolean isUnlimitedCryptoNeeded() {
			return unlimitedNeeded;
		}
	}

	public String getDatabaseDir() {
		return databaseDir;
	}

	public void setLocalDir(String localDir) {
		this.localDir = localDir;
	}

	public void setDatabaseDir(String databaseDir) {
		this.databaseDir = databaseDir;
	}

	public void setCacheDir(String cacheDir) {
		this.cacheDir = cacheDir;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	public void setConnection(ConnectionSettings connection) {
		this.connection = connection;
	}

	public void setEncryption(EncryptionSettings encryption) {
		this.encryption = encryption;
	}
}

