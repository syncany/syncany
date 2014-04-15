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
package org.syncany.operations.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.operations.Operation;
import org.syncany.operations.plugin.PluginOperationOptions.PluginListMode;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.FileUtil;

public class PluginOperation extends Operation {
	private static final Logger logger = Logger.getLogger(PluginOperation.class.getSimpleName());

	private static final String PLUGIN_LIST_URL_ALL = "http://api.syncany.org/v1/plugins/list.php";
	private static final String PLUGIN_LIST_URL_SINGLE = "http://api.syncany.org/v1/plugins/list.php?plugin=%s";
	
	private PluginOperationOptions options;
	private PluginOperationResult result;

	public PluginOperation(Config config, PluginOperationOptions options) {
		super(config);

		this.options = options;
		this.result = new PluginOperationResult();
	}

	@Override
	public PluginOperationResult execute() throws Exception {
		switch (options.getAction()) {
		case LIST:
			return executeList();

		case INSTALL:
			return executeInstall();
			
		case UNINSTALL:
			return executeUninstall();

		default:
			throw new Exception("Unknown action: " + options.getAction());
		}
	}

	private PluginOperationResult executeUninstall() throws Exception {
		String pluginId = options.getPluginId();
		Plugin plugin = Plugins.get(pluginId);
		
		if (plugin == null) {
			throw new Exception("Plugin not installed.");
		}
		
		Class<? extends Plugin> pluginClass = plugin.getClass();
		URL pluginClassLocation = pluginClass.getResource('/' + pluginClass.getName().replace('.', '/') + ".class");
		String pluginClassLocationStr = pluginClassLocation.toString();
		
		File globalUserPluginDir = getGlobalUserPluginDir();
		String globalUserDirJarPrefix = "jar:file:" + globalUserPluginDir.getAbsolutePath();
		
		boolean canBeUninstalled = pluginClassLocationStr.startsWith(globalUserDirJarPrefix);
		
		if (canBeUninstalled) {
			logger.log(Level.INFO, "Plugin can be uninstalled; class location at " + pluginClassLocation);
			
			int indexStartAfterSchema = "jar:file:".length();
			int indexEndAtExclamationPoint = pluginClassLocationStr.indexOf("!");
			File pluginJarFile = new File(pluginClassLocationStr.substring(indexStartAfterSchema, indexEndAtExclamationPoint));
			
			logger.log(Level.INFO, "Uninstalling plugin from file " + pluginJarFile);			
			pluginJarFile.delete();			
		}
		else {
			logger.log(Level.INFO, "Plugin can NOT be uninstalled because class location at " + pluginClassLocation);
		}
		
		
		return null;
	}

	private PluginOperationResult executeInstall() throws Exception {
		String pluginId = options.getPluginId();
		File potentialLocalPluginJarFile = new File(pluginId);
		
		if (pluginId.matches("^https?://")) {
			return executeInstallFromUrl(pluginId);	
		}
		else if (potentialLocalPluginJarFile.exists()) {
			return executeInstallFromLocalFile(potentialLocalPluginJarFile);
		}
		else {
			return executeInstallFromApiHost(pluginId);
		}		
	}
	
	private PluginOperationResult executeInstallFromApiHost(String pluginId) throws Exception {
		try {
			PluginInfo pluginInfo = getRemotePluginInfo(pluginId);
			
			if (pluginInfo == null) {
				throw new RuntimeException("Plugin not found");			
			}			
			
			File tempPluginJarFile = downloadPluginJar(pluginInfo.getDownloadUrl());		
			// TODO [high] Validate checksum
			
			File globalUserPluginDir = getGlobalUserPluginDir();
			globalUserPluginDir.mkdirs();
			
			File targetPluginJarFile = new File(globalUserPluginDir, String.format("syncany-plugin-%s-%s.jar", pluginInfo.getPluginId(), pluginInfo.getPluginVersion()));			
			FileUtils.copyFile(tempPluginJarFile, targetPluginJarFile);
			
			return null;
		}
		catch (Exception e) {
			throw new Exception(e);
		}
	}

	private PluginOperationResult executeInstallFromLocalFile(File potentialLocalPluginJarFile) {
		throw new RuntimeException("Not yet implemented.");
		//FileUtils.copyFileToDirectory(srcFile, destDir);

	}

	private PluginOperationResult executeInstallFromUrl(String downloadUrl) {
		throw new RuntimeException("Not yet implemented.");
	}

	private File getGlobalUserAppDir() {
		if (EnvironmentUtil.isWindows()) {
			return new File(System.getProperty("user.home") + "/Syncany");
		}
		else {
			return new File(System.getProperty("user.home") + "/.config/syncany");
		}
	}
	
	private File getGlobalUserPluginDir() {
		return new File(getGlobalUserAppDir(), "plugins");
	}

	/**
	 * Downloads the plugin JAR from the given URL to a temporary
	 * local location.  
	 */
	private File downloadPluginJar(String pluginJarUrl) throws Exception {
		try {
			URL pluginJarFile = new URL(pluginJarUrl);
			logger.log(Level.INFO, "Querying " + pluginJarFile + " ...");

			URLConnection urlConnection = pluginJarFile.openConnection();
			urlConnection.setConnectTimeout(2000);
			urlConnection.setReadTimeout(2000);
			
			File tempPluginFile = File.createTempFile("syncany-plugin", "tmp");
			tempPluginFile.deleteOnExit();
			
			FileOutputStream tempPluginFileOutputStream = new FileOutputStream(tempPluginFile);
			InputStream remoteJarFileInputStream = urlConnection.getInputStream();
			
			FileUtil.appendToOutputStream(remoteJarFileInputStream, tempPluginFileOutputStream);
			
			remoteJarFileInputStream.close();
			tempPluginFileOutputStream.close();
			
			return tempPluginFile;			
		}
		catch (Exception e) {
			throw new Exception(e);
		}
	}

	private PluginOperationResult executeList() throws Exception {
		Map<String, ExtendedPluginInfo> pluginInfos = new TreeMap<String, ExtendedPluginInfo>();
		
		if (options.getListMode() == PluginListMode.ALL || options.getListMode() == PluginListMode.LOCAL) {
			for (PluginInfo localPluginInfo : getLocalList()) {
				ExtendedPluginInfo extendedPluginInfo = new ExtendedPluginInfo();

				extendedPluginInfo.setLocalPluginInfo(localPluginInfo);
				extendedPluginInfo.setInstalled(true);
				
				pluginInfos.put(localPluginInfo.getPluginId(), extendedPluginInfo);
			}
		}
		
		if (options.getListMode() == PluginListMode.ALL || options.getListMode() == PluginListMode.REMOTE) {
			for (PluginInfo remotePluginInfo : getRemotePluginInfoList()) {
				ExtendedPluginInfo extendedPluginInfo = pluginInfos.get(remotePluginInfo.getPluginId());
				
				if (extendedPluginInfo == null) { // Locally not installed 
					extendedPluginInfo = new ExtendedPluginInfo();
					
					extendedPluginInfo.setInstalled(false);
					extendedPluginInfo.setRemoteAvailable(true);
					extendedPluginInfo.setUpgradeAvailable(true);												
				}
				else { // Locally also installed					
					boolean remoteAndLocalVersionEqual = remotePluginInfo.getPluginVersion().equals(extendedPluginInfo.getLocalPluginInfo().getPluginVersion());

					extendedPluginInfo.setRemoteAvailable(true);	
					extendedPluginInfo.setUpgradeAvailable(!remoteAndLocalVersionEqual);
				}
				
				extendedPluginInfo.setRemotePluginInfo(remotePluginInfo);
				pluginInfos.put(remotePluginInfo.getPluginId(), extendedPluginInfo);
			}
		}
		
		result.setPluginList(new ArrayList<ExtendedPluginInfo>(pluginInfos.values()));

		return result;
	}

	private List<PluginInfo> getLocalList() {
		List<PluginInfo> localPluginInfos = new ArrayList<PluginInfo>();
		
		for (Plugin plugin : Plugins.list()) {
			PluginInfo pluginInfo = new PluginInfo();
						
			pluginInfo.setPluginId(plugin.getId());
			pluginInfo.setPluginName(plugin.getName());
			pluginInfo.setPluginVersion(plugin.getVersion());
			
			localPluginInfos.add(pluginInfo);
		}
		
		return localPluginInfos;
	}

	private List<PluginInfo> getRemotePluginInfoList() throws Exception {
		String remoteListStr = getRemoteListStr(null);
		PluginListResponse pluginListResponse = new Persister().read(PluginListResponse.class, remoteListStr);

		return pluginListResponse.getPlugins();
	}
	
	private PluginInfo getRemotePluginInfo(String pluginId) throws Exception {
		String remoteListStr = getRemoteListStr(pluginId);
		PluginListResponse pluginListResponse = new Persister().read(PluginListResponse.class, remoteListStr);

		if (pluginListResponse.getPlugins().size() > 0) {
			return pluginListResponse.getPlugins().get(0); 
		}
		else {
			return null;
		}
	}

	private String getRemoteListStr(String pluginId) throws Exception {
		try {
			URL pluginListUrl = (pluginId != null) ? new URL(String.format(PLUGIN_LIST_URL_SINGLE, pluginId)) : new URL(PLUGIN_LIST_URL_ALL);
			logger.log(Level.INFO, "Querying " + pluginListUrl + " ...");

			URLConnection urlConnection = pluginListUrl.openConnection();
			urlConnection.setConnectTimeout(2000);
			urlConnection.setReadTimeout(2000);
			BufferedReader breader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

			StringBuilder stringBuilder = new StringBuilder();

			String line;
			while ((line = breader.readLine()) != null) {
				stringBuilder.append(line);
			}

			String responseStr = stringBuilder.toString();
			logger.log(Level.INFO, "Response from api.syncany.org: " + responseStr);
			
			return responseStr;
		}
		catch (Exception e) {
			throw new Exception(e);
		}
	}
}
