package org.syncany.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.syncany.util.FileUtil;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Config {
	private String rootDir;
	private String appDir;
	private String cacheDir;
	private String machineName;
	private ConnectionSettings connection;
	private EncryptionSettings encryption;

	public Config() {
		// Fressen
	}
	
	public Config(File file) throws JsonSyntaxException, IOException {
		load(file);
	}
	
	public String getRootDir() {
		return rootDir;
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
		
	    public String getType() {
			return type;
		}
		public Map<String, String> getSettings() {
			return settings;
		}
	}
	
	public static class EncryptionSettings {
	    private String pass;

		public String getPass() {
			return pass;
		}
	}
	
}

