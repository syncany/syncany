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
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.operations.Operation;
import org.syncany.operations.plugin.PluginOperationOptions.PluginListMode;
import org.syncany.util.StringUtil;

public class PluginOperation extends Operation {
	private static final Logger logger = Logger.getLogger(PluginOperation.class.getSimpleName());

	private static final String PLUGIN_LIST_URL = "http://api.syncany.org/v1/plugins/list.php";
	
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
		case ACTIVATE:
		case DEACTIVATE:
			throw new Exception("Action not yet implemented: " + options.getAction());

		default:
			throw new Exception("Unknown action: " + options.getAction());
		}
	}

	private PluginOperationResult executeList() throws Exception {
		List<PluginInfo> pluginInfos = new ArrayList<PluginInfo>();
		
		if (options.getListMode() == PluginListMode.ALL || options.getListMode() == PluginListMode.LOCAL) {
			pluginInfos.addAll(getLocalList());
		}
		
		if (options.getListMode() == PluginListMode.ALL || options.getListMode() == PluginListMode.REMOTE) {
			pluginInfos.addAll(getRemoteList());
		}
		
		result.setPluginList(pluginInfos); // TODO This should include plugins in .syncany/plugins

		return result;
	}

	private List<PluginInfo> getLocalList() {
		List<PluginInfo> localPluginInfos = new ArrayList<PluginInfo>();
		
		for (Plugin plugin : Plugins.list()) {
			PluginInfo pluginInfo = new PluginInfo();
						
			pluginInfo.setPluginId(plugin.getId());
			pluginInfo.setPluginName(plugin.getName());
			pluginInfo.setPluginVersion(StringUtil.join(plugin.getVersion(), "."));
			
			localPluginInfos.add(pluginInfo);
		}
		
		return localPluginInfos;
	}

	private List<PluginInfo> getRemoteList() throws Exception {
		String remoteListStr = getRemoteListStr();
		PluginListResponse pluginListResponse = new Persister().read(PluginListResponse.class, remoteListStr);

		return pluginListResponse.getPlugins();
	}

	private String getRemoteListStr() throws Exception {
		try {
			URL pluginListUrl = new URL(PLUGIN_LIST_URL);
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
