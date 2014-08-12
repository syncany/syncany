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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.core.Persister;
import org.syncany.Client;
import org.syncany.config.Config;
import org.syncany.config.UserConfig;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.Operation;
import org.syncany.operations.plugin.PluginOperationOptions.PluginAction;
import org.syncany.operations.plugin.PluginOperationOptions.PluginListMode;
import org.syncany.operations.plugin.PluginOperationResult.PluginResultCode;
import org.syncany.plugins.Plugin;
import org.syncany.plugins.Plugins;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

import com.github.zafarkhaja.semver.Version;

/**
 * The plugin operation installs, removes and lists storage {@link Plugin}s.
 * 
 * <p>The plugin implements these three functionalities as different 
 * {@link PluginAction}:
 * 
 * <ul>
 *   <li><tt>INSTALL</tt>: Installation means copying a file to the user plugin directory
 *       as specified by {@link Client#getUserPluginLibDir()}. A plugin can be installed 
 *       from a local JAR file, a URL (the operation downloads a JAR file), or the 
 *       API host (the operation find the plugin using the 'list' action and downloads
 *       the JAR file).</li>
 *   <li><tt>REMOVE</tt>: Removal means deleting a JAR file from the user plugin
 *       directoryThis action. This action simply finds the responsible plugin JAR
 *       file and deletes it. Only JAR files inside the user plugin direcory can be
 *       deleted.</li>
 *   <li><tt>LIST</tt>: Listing refers to a local and a remote list. The locally installed
 *       plugins can be queried by {@link Plugins#list()}. These plugins' JAR files must be
 *       in the application's class path. Remotely available plugins are queried through the
 *       API.</li>
 * </ul> 
 *   
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class PluginOperation extends Operation {
	private static final Logger logger = Logger.getLogger(PluginOperation.class.getSimpleName());

	private static final String PLUGIN_LIST_URL = "https://api.syncany.org/v1/plugins/list?appVersion=%s&snapshots=%s&pluginId=%s";	
	private static final String PURGEFILE_FILENAME = "purgefile";

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

		case REMOVE:
			return executeRemove();

		default:
			throw new Exception("Unknown action: " + options.getAction());
		}
	}

	private PluginOperationResult executeRemove() throws Exception {
		String pluginId = options.getPluginId();
		Plugin plugin = Plugins.get(pluginId);

		if (plugin == null) {
			throw new Exception("Plugin not installed.");
		}

		Class<? extends Plugin> pluginClass = plugin.getClass();
		URL pluginClassLocation = pluginClass.getResource('/' + pluginClass.getName().replace('.', '/') + ".class");
		String pluginClassLocationStr = pluginClassLocation.toString();
		logger.log(Level.INFO, "Plugin class is at " + pluginClassLocation);

		File globalUserPluginDir = UserConfig.getUserPluginLibDir();

		int indexStartAfterSchema = "jar:file:".length();
		int indexEndAtExclamationPoint = pluginClassLocationStr.indexOf("!");
		File pluginJarFile = new File(pluginClassLocationStr.substring(indexStartAfterSchema, indexEndAtExclamationPoint));
		logger.log(Level.INFO, "Plugin is in JAR at " + pluginJarFile);

		boolean canBeUninstalled = pluginJarFile.getAbsolutePath().startsWith(globalUserPluginDir.getAbsolutePath());

		if (canBeUninstalled) {
			PluginInfo pluginInfo = readPluginInfoFromJar(pluginJarFile);

			logger.log(Level.INFO, "Uninstalling plugin from file " + pluginJarFile);
			pluginJarFile.delete();

			// JAR files are locked on Windows, adding JAR filename to a list for delayed deletion (by batch file)
			if (EnvironmentUtil.isWindows()) {
				File purgefilePath = new File(UserConfig.getUserConfigDir(), PURGEFILE_FILENAME);
				
				try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(purgefilePath, true)))) {
					out.println(pluginJarFile.getAbsolutePath());
				}
				catch (IOException e) {
					logger.log(Level.SEVERE, "Unable to append to purgefile " + purgefilePath, e);
				}
			}

			result.setSourcePluginPath(pluginJarFile.getAbsolutePath());
			result.setAffectedPluginInfo(pluginInfo);
			result.setResultCode(PluginResultCode.OK);
		}
		else {
			logger.log(Level.INFO, "Plugin can NOT be uninstalled because class location not in " + globalUserPluginDir);
			result.setResultCode(PluginResultCode.NOK);
		}

		return result;
	}

	private PluginOperationResult executeInstall() throws Exception {
		String pluginId = options.getPluginId();
		File potentialLocalPluginJarFile = new File(pluginId);

		if (pluginId.matches("^https?://.+")) {
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
		checkPluginNotInstalled(pluginId);

		PluginInfo pluginInfo = getRemotePluginInfo(pluginId);

		if (pluginInfo == null) {
			throw new Exception("Plugin with ID '" + pluginId + "' not found");
		}

		checkPluginCompatibility(pluginInfo);
		
		File tempPluginJarFile = downloadPluginJar(pluginInfo.getDownloadUrl());
		String expectedChecksum = pluginInfo.getSha256sum();
		String actualChecksum = calculateChecksum(tempPluginJarFile);

		if (expectedChecksum == null || !expectedChecksum.equals(actualChecksum)) {
			throw new Exception("Checksum mismatch. Expected: " + expectedChecksum + ", but was: " + actualChecksum);
		}
		
		logger.log(Level.INFO, "Plugin JAR checksum verified: " + actualChecksum);

		File targetPluginJarFile = installPlugin(tempPluginJarFile, pluginInfo);

		result.setSourcePluginPath(pluginInfo.getDownloadUrl());
		result.setTargetPluginPath(targetPluginJarFile.getAbsolutePath());
		result.setAffectedPluginInfo(pluginInfo);
		result.setResultCode(PluginResultCode.OK);

		return result;
	}

	private void checkPluginCompatibility(PluginInfo pluginInfo) throws Exception {
		Version applicationVersion = Version.valueOf(Client.getApplicationVersion());
		Version pluginAppMinVersion = Version.valueOf(pluginInfo.getPluginAppMinVersion());
		
		logger.log(Level.INFO, "Checking plugin compatibility:");
		logger.log(Level.INFO, "- Application version:             " + Client.getApplicationVersion() + "(" + applicationVersion + ")");
		logger.log(Level.INFO, "- Plugin min. application version: " + pluginInfo.getPluginAppMinVersion() + "(" + pluginAppMinVersion + ")");
		
		if (!applicationVersion.greaterThanOrEqualTo(pluginAppMinVersion)) {
			throw new Exception("Plugin is incompatible to this application version. Plugin min. application version is "
					+ pluginInfo.getPluginAppMinVersion() + ", current application version is " + Client.getApplicationVersion());
		}
		
		// Verify if any conflicting plugins are installed
		logger.log(Level.INFO, "Checking for conflicting plugins.");
		
		List<String> conflictingIds = pluginInfo.getConflictingPluginIds();
		List<String> conflictingInstalledIds = new ArrayList<String>();
		
		if (conflictingIds != null) {
			for (String pluginId : conflictingIds) {
				Plugin plugin = Plugins.get(pluginId);
				
				if (plugin != null) {
					logger.log(Level.INFO, "- Conflicting plugin " + pluginId + " found.");
					conflictingInstalledIds.add(pluginId);
				}
				
				logger.log(Level.FINE, "- Conflicting plugin " + pluginId + " not installed");
			}
		}
		
		result.setConflictingPlugins(conflictingInstalledIds);
	}

	private String calculateChecksum(File tempPluginJarFile) throws Exception {
		CipherUtil.enableUnlimitedStrength();

		byte[] actualChecksum = FileUtil.createChecksum(tempPluginJarFile, "SHA256");
		return StringUtil.toHex(actualChecksum);
	}

	private PluginOperationResult executeInstallFromLocalFile(File pluginJarFile) throws Exception {
		PluginInfo pluginInfo = readPluginInfoFromJar(pluginJarFile);

		checkPluginNotInstalled(pluginInfo.getPluginId());
		checkPluginCompatibility(pluginInfo);

		File targetPluginJarFile = installPlugin(pluginJarFile, pluginInfo);

		result.setSourcePluginPath(pluginJarFile.getPath());
		result.setTargetPluginPath(targetPluginJarFile.getPath());
		result.setAffectedPluginInfo(pluginInfo);
		result.setResultCode(PluginResultCode.OK);

		return result;
	}

	private PluginOperationResult executeInstallFromUrl(String downloadJarUrl) throws Exception {
		File tempPluginJarFile = downloadPluginJar(downloadJarUrl);
		PluginInfo pluginInfo = readPluginInfoFromJar(tempPluginJarFile);

		checkPluginNotInstalled(pluginInfo.getPluginId());
		checkPluginCompatibility(pluginInfo);

		File targetPluginJarFile = installPlugin(tempPluginJarFile, pluginInfo);

		result.setSourcePluginPath(downloadJarUrl);
		result.setTargetPluginPath(targetPluginJarFile.getPath());
		result.setAffectedPluginInfo(pluginInfo);
		result.setResultCode(PluginResultCode.OK);

		return result;
	}

	private void checkPluginNotInstalled(String pluginId) throws Exception {
		Plugin locallyInstalledPlugin = Plugins.get(pluginId);

		if (locallyInstalledPlugin != null) {
			throw new Exception("Plugin '" + pluginId + "' already installed. Use 'sy plugin remove " + pluginId + "' to uninstall it first.");
		}

		logger.log(Level.INFO, "Plugin '" + pluginId + "' not installed. Okay!");
	}

	private PluginInfo readPluginInfoFromJar(File pluginJarFile) throws Exception {
		try (JarInputStream jarStream = new JarInputStream(new FileInputStream(pluginJarFile))) {
			Manifest jarManifest = jarStream.getManifest();

			if (jarManifest == null) {
				throw new Exception("Given file is not a valid Syncany plugin file (not a JAR file, or no manifest).");
			}
			
			String pluginId = jarManifest.getMainAttributes().getValue("Plugin-Id");
			
			if (pluginId == null) {
				throw new Exception("Given file is not a valid Syncany plugin file (no plugin ID in manifest).");
			}
			
			PluginInfo pluginInfo = new PluginInfo();
			
			pluginInfo.setPluginId(pluginId);
			pluginInfo.setPluginName(jarManifest.getMainAttributes().getValue("Plugin-Name"));
			pluginInfo.setPluginVersion(jarManifest.getMainAttributes().getValue("Plugin-Version"));
			pluginInfo.setPluginDate(jarManifest.getMainAttributes().getValue("Plugin-Date"));
			pluginInfo.setPluginAppMinVersion(jarManifest.getMainAttributes().getValue("Plugin-App-Min-Version"));
			pluginInfo.setPluginRelease(Boolean.parseBoolean(jarManifest.getMainAttributes().getValue("Plugin-Release")));
			
			if (jarManifest.getMainAttributes().getValue("Plugin-Conflicts-With") != null) {
				pluginInfo.setConflictingPluginIds(Arrays.asList(jarManifest.getMainAttributes().getValue("Plugin-Conflicts-With")));
			}
			
			return pluginInfo;
		}		
	}

	private File installPlugin(File pluginJarFile, PluginInfo pluginInfo) throws IOException {
		File globalUserPluginDir = UserConfig.getUserPluginLibDir();
		globalUserPluginDir.mkdirs();

		File targetPluginJarFile = new File(globalUserPluginDir, String.format("syncany-plugin-%s-%s.jar", pluginInfo.getPluginId(),
				pluginInfo.getPluginVersion()));

		logger.log(Level.INFO, "Installing plugin from " + pluginJarFile + " to " + targetPluginJarFile + " ...");
		FileUtils.copyFile(pluginJarFile, targetPluginJarFile);

		return targetPluginJarFile;
	}

	/**
	 * Downloads the plugin JAR from the given URL to a temporary
	 * local location.  
	 */
	private File downloadPluginJar(String pluginJarUrl) throws Exception {
		URL pluginJarFile = new URL(pluginJarUrl);
		logger.log(Level.INFO, "Querying " + pluginJarFile + " ...");

		URLConnection urlConnection = pluginJarFile.openConnection();
		urlConnection.setConnectTimeout(2000);
		urlConnection.setReadTimeout(2000);

		File tempPluginFile = File.createTempFile("syncany-plugin", "tmp");
		tempPluginFile.deleteOnExit();

		logger.log(Level.INFO, "Downloading to " + tempPluginFile + " ...");
		FileOutputStream tempPluginFileOutputStream = new FileOutputStream(tempPluginFile);
		InputStream remoteJarFileInputStream = urlConnection.getInputStream();

		IOUtils.copy(remoteJarFileInputStream, tempPluginFileOutputStream);

		remoteJarFileInputStream.close();
		tempPluginFileOutputStream.close();

		if (!tempPluginFile.exists() || tempPluginFile.length() == 0) {
			throw new Exception("Downloading plugin file failed, URL was " + pluginJarUrl);
		}

		return tempPluginFile;
	}

	private PluginOperationResult executeList() throws Exception {
		Map<String, ExtendedPluginInfo> pluginInfos = new TreeMap<String, ExtendedPluginInfo>();

		if (options.getListMode() == PluginListMode.ALL || options.getListMode() == PluginListMode.LOCAL) {
			for (PluginInfo localPluginInfo : getLocalList()) {
				if (options.getPluginId() != null && !localPluginInfo.getPluginId().equals(options.getPluginId())) {
					continue;
				}

				ExtendedPluginInfo extendedPluginInfo = new ExtendedPluginInfo();

				extendedPluginInfo.setLocalPluginInfo(localPluginInfo);
				extendedPluginInfo.setInstalled(true);

				pluginInfos.put(localPluginInfo.getPluginId(), extendedPluginInfo);
			}
		}

		if (options.getListMode() == PluginListMode.ALL || options.getListMode() == PluginListMode.REMOTE) {
			for (PluginInfo remotePluginInfo : getRemotePluginInfoList()) {
				if (options.getPluginId() != null && !remotePluginInfo.getPluginId().equals(options.getPluginId())) {
					continue;
				}

				ExtendedPluginInfo extendedPluginInfo = pluginInfos.get(remotePluginInfo.getPluginId());

				if (extendedPluginInfo == null) { // Locally not installed
					extendedPluginInfo = new ExtendedPluginInfo();

					extendedPluginInfo.setInstalled(false);
					extendedPluginInfo.setRemoteAvailable(true);
					extendedPluginInfo.setUpgradeAvailable(true);
				}
				else { // Locally also installed
					boolean remoteAndLocalVersionEqual = remotePluginInfo.getPluginVersion().equals(
							extendedPluginInfo.getLocalPluginInfo().getPluginVersion());

					extendedPluginInfo.setRemoteAvailable(true);
					extendedPluginInfo.setUpgradeAvailable(!remoteAndLocalVersionEqual);
				}

				extendedPluginInfo.setRemotePluginInfo(remotePluginInfo);
				pluginInfos.put(remotePluginInfo.getPluginId(), extendedPluginInfo);
			}
		}

		result.setPluginList(new ArrayList<ExtendedPluginInfo>(pluginInfos.values()));
		result.setResultCode(PluginResultCode.OK);

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
		String appVersion = Client.getApplicationVersion();
		String snapshotsEnabled = (options.isSnapshots()) ? "true" : "false";
		String pluginIdQueryStr = (pluginId != null) ? pluginId : "";

		URL pluginListUrl = new URL(String.format(PLUGIN_LIST_URL, appVersion, snapshotsEnabled, pluginIdQueryStr));
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
}
