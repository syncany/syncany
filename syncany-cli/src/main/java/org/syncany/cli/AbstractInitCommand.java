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
package org.syncany.cli;

import java.io.Console;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.PluginSetting;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.operations.GenlinkOperation.GenlinkOperationResult;
import org.syncany.util.StringUtil;

public abstract class AbstractInitCommand extends Command {
	protected Console console;
	
	public AbstractInitCommand() {
		console = System.console();
	}	
	
	protected ConfigTO createConfigTO(File localDir, SaltedSecretKey masterKey, ConnectionTO connectionTO) throws Exception {
		ConfigTO configTO = new ConfigTO();
		
		configTO.setDisplayName(getDefaultDisplayName());
		configTO.setMachineName(getDefaultMachineName());
		configTO.setMasterKey(masterKey); // can be null

		configTO.setConnection(connectionTO);
		
		return configTO;
	}		

	protected ConnectionTO initPluginWithOptions(OptionSet options, OptionSpec<String> optionPlugin, OptionSpec<String> optionPluginOpts) throws Exception {
		ConnectionTO connectionTO = new ConnectionTO();
		
		String pluginStr = null;
		Map<String, String> pluginSettings = new HashMap<String, String>();

		if (options.has(optionPlugin)) {
			pluginStr = initPlugin(options.valueOf(optionPlugin));
		}
		else {
			pluginStr = askPlugin();
		}
		
		if (options.has(optionPluginOpts)) {
			pluginSettings = initPluginSettings(pluginStr, options.valuesOf(optionPluginOpts));			
		}
		else {
			pluginSettings = askPluginSettings(pluginStr);
		}		
		
		connectionTO.setType(pluginStr);
		connectionTO.setSettings(pluginSettings);
		
		return connectionTO;
	}
	
	protected Map<String, String> initPluginSettings(String pluginStr, List<String> pluginSettingsOptList) throws Exception {		
		Map<String, String> pluginSettings = new HashMap<String, String>();
		
		// Fill settings map
		for (String pluginSettingKeyValue : pluginSettingsOptList) {
			String[] keyValue = pluginSettingKeyValue.split("=", 2);
			
			if (keyValue.length != 2) {
				throw new Exception("Invalid setting: "+pluginSettingKeyValue);
			}
			
			pluginSettings.put(keyValue[0], keyValue[1]);
		}
		
		Plugin plugin = Plugins.get(pluginStr); // Assumes this exists
		Connection connection = plugin.createConnection();
		
		// Check if all mandatory are set
		for (PluginSetting setting : connection.getSettings()) {
			if (setting.isMandatory() && !pluginSettings.containsKey(setting.name)) {
				throw new Exception("Not all mandatory settings are set ("+StringUtil.join(connection.getSettings(), ", ")+"). Use -Psettingname=.. to set it.");
			}
		}	
				
		connection.init(pluginSettings); // Only to test for exceptions!
		
		return pluginSettings;
	}

	protected String initPlugin(String pluginStr) throws Exception {
		Plugin plugin = Plugins.get(pluginStr);
		
		if (plugin == null) {
			throw new Exception("ERROR: Plugin '"+pluginStr+"' does not exist.");
		}
		
		return pluginStr;
	}

	protected Map<String, String> askPluginSettings(String pluginStr) throws StorageException {
		Plugin plugin = Plugins.get(pluginStr); // Assumes this exists
		Connection connection = plugin.createConnection();
		
		List<PluginSetting> pluginSettings = connection.getSettings();
		Map<String, String> pluginSettingsMap = new TreeMap<String, String>();
		
		out.println();
		out.println("Connection details for "+plugin.getName()+" connection:");
		
		for (PluginSetting setting : pluginSettings) {
			
			while (true) {
				out.print("- "+setting.name+": ");
				String value = null;
				if (setting.isSensitive()) {
					value = String.copyValueOf(console.readPassword());
				}
				else {
					value = console.readLine();
				}
				try {
					setting.setValue(value);
				}
				catch (InvalidParameterException e) {
					out.println(value + " is not valid input for the setting " + setting.name);
					out.println();
					continue;
				}
				if (setting.isMandatory()) {
					if ("".equals(setting.getValue())) {
						out.println("ERROR: This setting is mandatory.");
						out.println();
					}
					else {
						break;
					}
				}
				else {
					break;
				}
			}	
			if (setting.validate()) {
				pluginSettingsMap.put(setting.name, setting.getValue());
			}
		}

		connection.init(pluginSettingsMap); // To check for exceptions
		
		return pluginSettingsMap;
	}

	protected String askPlugin() {
		String pluginStr = null;
		
		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		String pluginsList = "";
		
		for (int i=0; i<plugins.size(); i++) {
			pluginsList += plugins.get(i).getId();
			if (i < plugins.size()-1) { pluginsList += ", "; }			
		}
		
		while (pluginStr == null) {
			out.println("Choose a storage plugin. Available plugins are: "+pluginsList);
			out.print("Plugin: ");			
			pluginStr = console.readLine();
			
			Plugin plugin = Plugins.get(pluginStr);
			
			if (plugin == null) {
				out.println("ERROR: Plugin '"+pluginStr+"' does not exist.");
				out.println();
				
				pluginStr = null;
			}
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
	
	protected void printLink(GenlinkOperationResult operationResult, boolean shortOutput) {
		if (shortOutput) {
			out.println(operationResult.getShareLink());
		}
		else {
			out.println();
			out.println("Repository created, and local folder initialized. To share the same repository");
			out.println("with others, you can share this link:");
			out.println();		
			out.println("   "+operationResult.getShareLink());
			out.println();
			
			if (operationResult.isShareLinkEncrypted()) {
				out.println("This link is encrypted with the given password, so you can safely share it.");
				out.println("using unsecure communication (chat, e-mail, etc.)");
				out.println();
				out.println("WARNING: The link contains the details of your repo connection which typically");
				out.println("         consist of usernames/password of the connection (e.g. FTP user/pass).");
			}
			else {
				out.println("WARNING: This link is NOT ENCRYPTED and might contain connection credentials");
				out.println("         Do NOT share this link unless you know what you are doing!");
				out.println();
				out.println("         The link contains the details of your repo connection which typically");
				out.println("         consist of usernames/password of the connection (e.g. FTP user/pass).");
			}

			out.println();
		}			
	}
}
