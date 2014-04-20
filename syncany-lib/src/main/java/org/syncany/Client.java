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
package org.syncany;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.syncany.config.Config;
import org.syncany.connection.plugins.StorageException;
import org.syncany.crypto.CipherException;
import org.syncany.operations.CleanupOperation;
import org.syncany.operations.CleanupOperation.CleanupOperationOptions;
import org.syncany.operations.CleanupOperation.CleanupOperationResult;
import org.syncany.operations.LogOperation;
import org.syncany.operations.LogOperation.LogOperationOptions;
import org.syncany.operations.LogOperation.LogOperationResult;
import org.syncany.operations.LsRemoteOperation;
import org.syncany.operations.LsRemoteOperation.LsRemoteOperationResult;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.OperationResult;
import org.syncany.operations.RestoreOperation;
import org.syncany.operations.RestoreOperation.RestoreOperationOptions;
import org.syncany.operations.RestoreOperation.RestoreOperationResult;
import org.syncany.operations.StatusOperation;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.down.DownOperationListener;
import org.syncany.operations.down.DownOperationOptions;
import org.syncany.operations.down.DownOperationResult;
import org.syncany.operations.init.ConnectOperation;
import org.syncany.operations.init.ConnectOperationListener;
import org.syncany.operations.init.ConnectOperationOptions;
import org.syncany.operations.init.ConnectOperationResult;
import org.syncany.operations.init.GenlinkOperation;
import org.syncany.operations.init.GenlinkOperationResult;
import org.syncany.operations.init.InitOperation;
import org.syncany.operations.init.InitOperationListener;
import org.syncany.operations.init.InitOperationOptions;
import org.syncany.operations.init.InitOperationResult;
import org.syncany.operations.plugin.PluginOperation;
import org.syncany.operations.plugin.PluginOperationOptions;
import org.syncany.operations.plugin.PluginOperationResult;
import org.syncany.operations.up.UpOperation;
import org.syncany.operations.up.UpOperationListener;
import org.syncany.operations.up.UpOperationOptions;
import org.syncany.operations.up.UpOperationResult;
import org.syncany.operations.watch.WatchOperation;
import org.syncany.operations.watch.WatchOperation.WatchOperationListener;
import org.syncany.operations.watch.WatchOperation.WatchOperationOptions;
import org.syncany.util.EnvironmentUtil;

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
	private static final String APPLICATION_PROPERTIES_RESOURCE = "/application.properties";
	private static final String APPLICATION_PROPERTIES_RELEASE_KEY = "applicationRelease";
	private static final String APPLICATION_PROPERTIES_VERSION_KEY = "applicationVersion";
	private static final String APPLICATION_PROPERTIES_REVISION_KEY = "applicationRevision";
	
	private static Properties applicationProperties;	
	private static File userAppDir;
	private static File userPluginsDir;

	protected Config config;

	static {
		initApplicationProperties();
		initUserAppDirs();	
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public Config getConfig() {
		return config;
	}

	public UpOperationResult up() throws Exception {
		return up(new UpOperationOptions());
	}

	public UpOperationResult up(UpOperationOptions options) throws Exception {
		return up(options, null);
	}

	public UpOperationResult up(UpOperationOptions options, UpOperationListener listener) throws Exception {
		return new UpOperation(config, options, listener).execute();
	}

	public DownOperationResult down() throws Exception {
		return down(new DownOperationOptions(), null);
	}

	public DownOperationResult down(DownOperationOptions options) throws Exception {
		return down(options, null);
	}

	public DownOperationResult down(DownOperationOptions options, DownOperationListener listener) throws Exception {
		return new DownOperation(config, options, listener).execute();
	}

	public StatusOperationResult status() throws Exception {
		return status(new StatusOperationOptions());
	}

	public StatusOperationResult status(StatusOperationOptions options) throws Exception {
		return new StatusOperation(config, options).execute();
	}

	public LsRemoteOperationResult lsRemote() throws Exception {
		return new LsRemoteOperation(config).execute();
	}

	public RestoreOperationResult restore(RestoreOperationOptions options) throws Exception {
		return new RestoreOperation(config, options).execute();
	}

	public LogOperationResult log(LogOperationOptions options) throws Exception {
		return new LogOperation(config, options).execute();
	}

	public void watch(WatchOperationOptions options) throws Exception {
		watch(options, null);	
	}	

	public void watch(WatchOperationOptions options, WatchOperationListener listener) throws Exception {
		new WatchOperation(config, options, listener).execute();		
	}	

	public GenlinkOperationResult genlink() throws Exception {
		return new GenlinkOperation(config).execute();
	}

	public InitOperationResult init(InitOperationOptions options) throws Exception {
		return init(options, null);
	}

	public InitOperationResult init(InitOperationOptions options, InitOperationListener listener) throws Exception {
		return new InitOperation(options, listener).execute();
	}

	public ConnectOperationResult connect(ConnectOperationOptions options) throws IOException, StorageException, CipherException {
		return connect(options, null);
	}

	public ConnectOperationResult connect(ConnectOperationOptions options, ConnectOperationListener listener) throws IOException, StorageException,
			CipherException {
		
		return new ConnectOperation(options, listener).execute();
	}

	public CleanupOperationResult cleanup() throws Exception {
		return new CleanupOperation(config, new CleanupOperationOptions()).execute();
	}

	public CleanupOperationResult cleanup(CleanupOperationOptions options) throws Exception {
		return new CleanupOperation(config, options).execute();
	}

	public PluginOperationResult plugin(PluginOperationOptions options) throws Exception {
		return new PluginOperation(config, options).execute();
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
			userAppDir = new File(System.getProperty("user.home") + "\\Syncany");
		}
		else {
			userAppDir = new File(System.getProperty("user.home") + "/.config/syncany");
		}
		
		userPluginsDir = new File(userAppDir, "plugins");
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
