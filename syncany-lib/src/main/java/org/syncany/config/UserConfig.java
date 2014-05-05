/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.Map;

import org.syncany.config.Config.ConfigException;
import org.syncany.config.to.UserConfigTO;
import org.syncany.util.EnvironmentUtil;

/**
 * Represents the configuration parameters and application user directory
 * of the currently logged in user, including system properties that will be 
 * set with every application start. 
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class UserConfig {	
	/* 
	 * Note: 
	 *   This class can't have any logging methods, because the init() method is called 
	 *   BEFORE the logging initialization. All errors must be printed to STDERR.
	 *    
	 */
	
	private static final File USER_APP_DIR_WINDOWS = new File(System.getenv("APPDATA") + "\\Syncany");
	private static final File USER_APP_DIR_UNIX_LIKE = new File(System.getProperty("user.home") + "/.config/syncany");
	private static final String USER_PLUGINS_LIB_DIR = "plugins/lib";
	private static final String USER_PLUGINS_USERDATA_DIR_FORMAT = "plugins/userdata/%s";
	private static final String USER_CONFIG_FILE = "userconfig.xml";
	private static final String USER_TRUSTSTORE_FILE = "truststore.jks";
	
	private static File userConfigDir;
	private static File userPluginLibDir;
	private static File userConfigFile;
	private static File userTrustStoreFile;
	private static KeyStore userTrustStore;	
	
	static {
		init();
	}
	
	public static void init() {
		if (userConfigDir == null) {
			initUserAppDirs();	
			initUserConfig();
			initUserTrustStore();
		}
	}

	private static void initUserTrustStore() {
		try {				
			userTrustStoreFile = new File(userConfigDir, USER_TRUSTSTORE_FILE);
			userTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
								
			if (userTrustStoreFile.exists()) {
				FileInputStream trustStoreInputStream = new FileInputStream(userTrustStoreFile); 		 		
				userTrustStore.load(trustStoreInputStream, new char[0]);
				
				trustStoreInputStream.close();
			}	
			else {
				userTrustStore.load(null, new char[0]); // Initialize empty store						
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static File getUserConfigDir() { 
		return userConfigDir;
	}

	public static File getUserPluginLibDir() {
		return userPluginLibDir;
	}
	
	public static File getUserPluginsUserdataDir(String pluginId) {
		File pluginConfigDir = new File(userConfigDir, String.format(USER_PLUGINS_USERDATA_DIR_FORMAT, pluginId));
		pluginConfigDir.mkdirs();

		return pluginConfigDir;
	}
	
	public static File getUserConfigFile() {
		return userConfigFile;
	}
	
	public static File getUserTrustStoreFile() {
		return userTrustStoreFile;
	}
	
	public static KeyStore getUserTrustStore() {
		return userTrustStore;
	}
	
	public static void storeTrustStore() {
		try {
			FileOutputStream trustStoreOutputStream = new FileOutputStream(userTrustStoreFile);
			userTrustStore.store(trustStoreOutputStream, new char[0]);
			
			trustStoreOutputStream.close();
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot store truststore to file " + userTrustStoreFile, e);
		}		
	}
	
	private static void initUserAppDirs() {
		userConfigDir = (EnvironmentUtil.isWindows()) ? USER_APP_DIR_WINDOWS : USER_APP_DIR_UNIX_LIKE;
		userConfigDir.mkdirs();
		
		userPluginLibDir = new File(userConfigDir, USER_PLUGINS_LIB_DIR);		
		userPluginLibDir.mkdirs();
	}

	private static void initUserConfig() {
		userConfigFile = new File(userConfigDir, USER_CONFIG_FILE);
		
		if (userConfigFile.exists()) {
			loadAndInitUserConfigFile(userConfigFile);			
		}
		else {
			writeExampleUserConfigFile(userConfigFile);
		}
	}

	private static void loadAndInitUserConfigFile(File userConfigFile) {
		try {
			UserConfigTO userConfigTO = UserConfigTO.load(userConfigFile);
			
			for (Map.Entry<String, String> systemProperty : userConfigTO.getSystemProperties().entrySet()) {
				System.setProperty(systemProperty.getKey(), systemProperty.getValue());
			}
		}
		catch (ConfigException e) {
			System.err.println("ERROR: " + e.getMessage());
			System.err.println("       Ignoring user config file!");
			System.err.println();
		}
	}	

	private static void writeExampleUserConfigFile(File userConfigFile) {
		UserConfigTO userConfigTO = new UserConfigTO();
		
		userConfigTO.getSystemProperties().put("example.property", "This is a demo property. You can delete it.");
		userConfigTO.getSystemProperties().put("syncany.rocks", "Yes, it does!");
		
		try {
			UserConfigTO.save(userConfigTO, userConfigFile);
		}
		catch (Exception e) {
			// Don't care!
		}
	}
}
