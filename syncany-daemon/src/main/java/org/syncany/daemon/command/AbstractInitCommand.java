/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.daemon.command;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.PluginOptionSpecs;
import org.syncany.connection.plugins.Plugins;
import org.syncany.crypto.SaltedSecretKey;

public abstract class AbstractInitCommand extends Command {
	
	protected ConfigTO createConfigTO(File localDir, SaltedSecretKey masterKey, ConnectionTO connectionTO) throws Exception {
		ConfigTO configTO = new ConfigTO();
		
		configTO.setDisplayName(getDefaultDisplayName());
		configTO.setMachineName(getDefaultMachineName());
		configTO.setMasterKey(masterKey); // can be null

		configTO.setConnection(connectionTO);
		
		return configTO;
	}		

	protected ConnectionTO initPluginWithOptions(String pluginName, List<String> pluginArgs) throws Exception {
		ConnectionTO connectionTO = new ConnectionTO();
		
		String pluginStr = null;
		Map<String, String> pluginSettings = new HashMap<String, String>();

		pluginStr = initPlugin(pluginName);
		pluginSettings = initPluginSettings(pluginStr, pluginArgs);			
		
		connectionTO.setType(pluginStr);
		connectionTO.setSettings(pluginSettings);
		
		return connectionTO;
	}
	
	protected Map<String, String> initPluginSettings(String pluginStr, List<String> pluginSettingsOptList) throws Exception {		
		Map<String, String> pluginSettings = new HashMap<String, String>();
		Plugin plugin = Plugins.get(pluginStr); // Assumes this exists
		
		Connection connection = plugin.createConnection();
		PluginOptionSpecs pluginOptionSpecs = connection.getOptionSpecs();
		
		// Fill settings map
		for (String pluginSettingKeyValue : pluginSettingsOptList) {
			String[] keyValue = pluginSettingKeyValue.split("=", 2);
			
			if (keyValue.length != 2) {
				throw new Exception("Invalid setting: "+pluginSettingKeyValue);
			}
			
			pluginSettings.put(keyValue[0], keyValue[1]);
		}
		
		pluginOptionSpecs.validate(pluginSettings); // throws error if invalid
		connection = null; // Connection only needed to to test for exceptions
		
		return pluginSettings;
		
		
	}

	protected String initPlugin(String pluginStr) throws Exception {
		Plugin plugin = Plugins.get(pluginStr);
		
		if (plugin == null) {
			throw new Exception("ERROR: Plugin '"+pluginStr+"' does not exist.");
		}
		
		return pluginStr;
	}

	protected String getDefaultMachineName() throws UnknownHostException {
		return new String(
			  InetAddress.getLocalHost().getHostName() 
			+ System.getProperty("user.name")
			+ Math.abs(new Random().nextInt())
		).replaceAll("[^a-zA-Z0-9]", "");		
	}
	

	protected String getDefaultDisplayName() throws UnknownHostException {
		return System.getProperty("user.name");		
	}
}
