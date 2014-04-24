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
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.syncany.Client;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.to.GlobalConfigTO;
import org.syncany.util.EnvironmentUtil;

/**
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class GlobalConfig {
	private static final String APPLICATION_PROPERTIES_RESOURCE = "/application.properties";
	private static final String APPLICATION_PROPERTIES_RELEASE_KEY = "applicationRelease";
	private static final String APPLICATION_PROPERTIES_VERSION_KEY = "applicationVersion";
	private static final String APPLICATION_PROPERTIES_REVISION_KEY = "applicationRevision";
	
	private static Properties applicationProperties;	
	private static File userAppDir;
	private static File userPluginsDir;
	
	public static void init() {
		initUserAppDirs();	
		initGlobalConfig();
		initApplicationProperties();
	}

	public static Properties getApplicationProperties() {
		return applicationProperties;
	}	

	public static boolean isApplicationRelease() {
		return Boolean.parseBoolean(applicationProperties.getProperty(APPLICATION_PROPERTIES_RELEASE_KEY));
	}

	public static String getApplicationVersion() {
		return applicationProperties.getProperty(APPLICATION_PROPERTIES_VERSION_KEY);
	}

	public static String getApplicationRevision() {
		return applicationProperties.getProperty(APPLICATION_PROPERTIES_REVISION_KEY);
	}

	public static File getUserAppDir() { 
		return userAppDir;
	}

	public static File getUserPluginDir() {
		return userPluginsDir;
	}	
	
	private static void initUserAppDirs() {
		if (EnvironmentUtil.isWindows()) {
			userAppDir = new File(System.getenv("APPDATA") + "\\Syncany");
		}
		else {
			userAppDir = new File(System.getProperty("user.home") + "/.config/syncany");
		}
		
		userPluginsDir = new File(userAppDir, "plugins");		
		userPluginsDir.mkdirs();
	}

	private static void initGlobalConfig() {
		File globalConfigFile = new File(userAppDir, "userconfig.xml");
		
		if (globalConfigFile.exists()) {
			try {
				GlobalConfigTO globalConfigTO = GlobalConfigTO.load(globalConfigFile);
				
				for (Map.Entry<String, String> systemProperty : globalConfigTO.getSystemProperties().entrySet()) {
					System.setProperty(systemProperty.getKey(), systemProperty.getValue());
				}
			}
			catch (ConfigException e) {
				e.printStackTrace();
				System.err.println(e.getMessage());
				System.err.println("Ignoring global config file.");
			}
		}		
	}

	private static void initApplicationProperties() {
		InputStream globalPropertiesInputStream = Client.class.getResourceAsStream(APPLICATION_PROPERTIES_RESOURCE);

		try {
			applicationProperties = new Properties();
			applicationProperties.load(globalPropertiesInputStream);
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot load application properties.", e);
		}
	}	
}
