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
package org.syncany.cli;

import java.io.Console;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.PluginOptionSpec;
import org.syncany.connection.plugins.PluginOptionSpec.OptionValidationResult;
import org.syncany.connection.plugins.PluginOptionSpecs;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.operations.GenlinkOperation.GenlinkOperationResult;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

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

	protected ConnectionTO initPluginWithOptions(OptionSet options, OptionSpec<String> optionPlugin, OptionSpec<String> optionPluginOpts)
			throws Exception {
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
		Map<String, String> pluginOptionValues = new HashMap<String, String>();		
		Plugin plugin = Plugins.get(pluginStr); // Assumes this exists

		Connection connection = plugin.createConnection();
		PluginOptionSpecs pluginOptionSpecs = connection.getOptionSpecs();
		
		// Fill settings map
		for (String pluginSettingKeyValue : pluginSettingsOptList) {
			String[] keyValue = pluginSettingKeyValue.split("=", 2);

			if (keyValue.length != 2) {
				throw new Exception("Invalid setting: " + pluginSettingKeyValue);
			}

			pluginOptionValues.put(keyValue[0], keyValue[1]);
		}
		
		pluginOptionSpecs.validate(pluginOptionValues); // throws error if invalid
		connection = null; // Connection only needed to to test for exceptions

		return pluginOptionValues;
	}

	protected String initPlugin(String pluginStr) throws Exception {
		Plugin plugin = Plugins.get(pluginStr);

		if (plugin == null) {
			throw new Exception("ERROR: Plugin '" + pluginStr + "' does not exist.");
		}

		return pluginStr;
	}

	protected Map<String, String> askPluginSettings(String pluginStr) throws StorageException {
		Plugin plugin = Plugins.get(pluginStr); // Assumes this exists
		
		Connection connection = plugin.createConnection();
		PluginOptionSpecs pluginOptionSpecs = connection.getOptionSpecs();
		
		Map<String, String> pluginOptionValues = new HashMap<String, String>();

		out.println();
		out.println("Connection details for " + plugin.getName() + " connection:");

		for (PluginOptionSpec optionSpec : pluginOptionSpecs.values()) {
			String optionValue = askPluginOption(optionSpec);
			pluginOptionValues.put(optionSpec.getId(), optionValue);
		}

		pluginOptionSpecs.validate(pluginOptionValues); // throws error if invalid
		connection = null; // Connection only needed to to test for exceptions
		
		return pluginOptionValues;
	}

	private String askPluginOption(PluginOptionSpec optionSpec) {
		while (true) {
			String value = null;

			if (optionSpec.isSensitive()) {
				out.printf("- %s (not displayed): ", optionSpec.getDescription());
				value = String.copyValueOf(console.readPassword());
			}
			else if (!optionSpec.isMandatory()) {
				out.printf("- %s (optional, default is %s): ", optionSpec.getDescription(), optionSpec.getDefaultValue());
				value = console.readLine();
			}
			else {
				out.printf("- %s: ", optionSpec.getDescription());
				value = console.readLine();
			}

			OptionValidationResult validationResult = optionSpec.validateInput(value);

			switch (validationResult) {
			case INVALID_NOT_SET:
				out.println("ERROR: This option is mandatory.");
				out.println();
				break;

			case INVALID_TYPE:
				out.println("ERROR: Not a valid input.");
				out.println();
				break;

			case VALID:
				return optionSpec.getValue(value);
				
			default:
				throw new RuntimeException("Invalid return type: "+validationResult);
			}
		}
	}

	protected String askPlugin() {
		String pluginStr = null;

		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		String pluginsList = StringUtil.join(plugins, ", ", new StringJoinListener<Plugin>() {
			@Override
			public String getString(Plugin plugin) {
				return plugin.getId();
			}
		});

		while (pluginStr == null) {
			out.println("Choose a storage plugin. Available plugins are: " + pluginsList);
			out.print("Plugin: ");
			pluginStr = console.readLine();

			Plugin plugin = Plugins.get(pluginStr);

			if (plugin == null) {
				out.println("ERROR: Plugin does not exist.");
				out.println();

				pluginStr = null;
			}
		}

		return pluginStr;
	}

	protected String getDefaultMachineName() throws UnknownHostException {
		return new String(InetAddress.getLocalHost().getHostName() + System.getProperty("user.name") + Math.abs(new Random().nextInt())).replaceAll(
				"[^a-zA-Z0-9]", "");
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
			out.println("   " + operationResult.getShareLink());
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
