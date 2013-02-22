package org.syncany.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.syncany.util.FileUtil;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


public class Config {
	//Directory which contains all files which shall be synced with remote repo 
	private String localDir;
	//Home / Config directory of syncany 
	private String appDir;
	//Synched db-versions 
	private String synchedDatabaseDir;
	//Local cache for chunks/multichunks/dbversions
	private String cacheDir;
	//local identifier for user
	private String machineName;
	private ConnectionSettings connection;
	private EncryptionSettings encryption;

	public Config(String localDir, String appDir, String synchedDatabaseDir,
			String cacheDir, String machineName, ConnectionSettings connection,
			EncryptionSettings encryption) {
		super();
		this.localDir = localDir;
		this.appDir = appDir;
		this.synchedDatabaseDir = synchedDatabaseDir;
		this.cacheDir = cacheDir;
		this.machineName = machineName;
		this.connection = connection;
		this.encryption = encryption;
	}

	public Config(File file) throws JsonSyntaxException, IOException {
		load(file);
	}
	
	public String getRootDir() {
		return localDir;
	}
	
	public String getAppDir() {
		return appDir;
	}
	
	public String getCacheDir() {
		return cacheDir;
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

	public void load(File file) throws JsonSyntaxException, IOException {
		Gson gson = new Gson();		
		gson.fromJson(FileUtil.readFileToString(file), Config.class);
	}
	
	public static class ConnectionSettings {
	    private String type;
	    private Map<String, String> settings;
	    
	    public ConnectionSettings(String type, Map<String, String> settings) {
			super();
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
	    private String pass;
	    
		public EncryptionSettings(String pass) {
			super();
			this.pass = pass;
		}

		public String getPass() {
			return pass;
		}
	}

	public String getSynchedDatabaseDir() {
		return synchedDatabaseDir;
	}

	
}

