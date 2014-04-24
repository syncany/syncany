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
	private static final File USER_APP_DIR_WINDOWS = new File(System.getenv("APPDATA") + "\\Syncany");
	private static final File USER_APP_DIR_UNIX_LIKE = new File(System.getProperty("user.home") + "/.config/syncany");
	private static final String USER_PLUGINS_DIR = "plugins";
	private static final String USER_CONFIG_FILE = "userconfig.xml";
	
	private static File userAppDir;
	private static File userPluginsDir;
	
	static {
		init();
	}
	
	public static void init() {
		if (userAppDir == null) {
			initUserAppDirs();	
			initUserConfig();
		}
	}

	public static File getUserAppDir() { 
		return userAppDir;
	}

	public static File getUserPluginDir() {
		return userPluginsDir;
	}	
	
	private static void initUserAppDirs() {
		userAppDir = (EnvironmentUtil.isWindows()) ? USER_APP_DIR_WINDOWS : USER_APP_DIR_UNIX_LIKE;
		userAppDir.mkdirs();
		
		userPluginsDir = new File(userAppDir, USER_PLUGINS_DIR);		
		userPluginsDir.mkdirs();
	}

	private static void initUserConfig() {
		File userConfigFile = new File(userAppDir, USER_CONFIG_FILE);
		
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
