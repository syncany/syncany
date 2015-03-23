/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.syncany.config.Config;
import org.syncany.config.UserConfig;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.OperationResult;

/**
 * The client class is a convenience class to call the application's {@link Operation}s
 * using a central entry point. The class offers wrapper methods around the operations.
 * 
 * <p>The methods typically take an {@link OperationOptions} instance as an argument, 
 * and return an instance of the {@link OperationResult} class.
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Client {
	private static final String APPLICATION_PROPERTIES_RESOURCE = "/application.properties"; // TODO [low] Move this!
	private static final String APPLICATION_PROPERTIES_TEST_RESOURCE = "/org/syncany/application.test.properties";
	private static final String APPLICATION_PROPERTIES_RELEASE_KEY = "applicationRelease";
	private static final String APPLICATION_PROPERTIES_VERSION_KEY = "applicationVersion";
	private static final String APPLICATION_PROPERTIES_VERSION_FULL_KEY = "applicationVersionFull";
	private static final String APPLICATION_PROPERTIES_REVISION_KEY = "applicationRevision";
	private static final String APPLICATION_PROPERTIES_DATE_KEY = "applicationDate";
	private static final String APPLICATION_PROPERTIES_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
	
	private static Properties applicationProperties;
	
	protected Config config;

	static {
		initUserConfig();
		initApplicationProperties();
	}
	
	public Client() {
		this.config = null;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public Config getConfig() {
		return config;
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

	public static String getApplicationVersionFull() {
		return applicationProperties.getProperty(APPLICATION_PROPERTIES_VERSION_FULL_KEY);
	}

	public static String getApplicationRevision() {
		return applicationProperties.getProperty(APPLICATION_PROPERTIES_REVISION_KEY);
	}
	
	public static Date getApplicationDate() {
		try {
			DateFormat dateFormat = new SimpleDateFormat(APPLICATION_PROPERTIES_DATE_FORMAT, Locale.ENGLISH);
		    Date applicationDate = dateFormat.parse(applicationProperties.getProperty(APPLICATION_PROPERTIES_DATE_KEY)); 
		    
			return applicationDate;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	private static void initUserConfig() {
		UserConfig.init();
	}

	private static void initApplicationProperties() {
		InputStream globalPropertiesInputStream = Client.class.getResourceAsStream(APPLICATION_PROPERTIES_RESOURCE);

		try {
			applicationProperties = new Properties();
			applicationProperties.load(globalPropertiesInputStream);
			
			initTestApplicationProperties();			
		}  
		catch (Exception e) {
			throw new RuntimeException("Cannot load application properties.", e);
		}
	}

	private static void initTestApplicationProperties() {
		InputStream testApplicationProperties = Client.class.getResourceAsStream(APPLICATION_PROPERTIES_TEST_RESOURCE);
		boolean isTestEnvironment = testApplicationProperties != null;
		
		if (isTestEnvironment) {
			try {
				applicationProperties.clear();
				applicationProperties.load(testApplicationProperties);
			}
			catch (Exception e) {
				throw new RuntimeException("Cannot load TEST-ONLY application properties.", e);
			}
		}
	}
}
