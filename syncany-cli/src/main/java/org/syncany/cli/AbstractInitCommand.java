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

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.cli.util.InitConsole;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.init.GenlinkOperationResult;
import org.syncany.plugins.PluginOptionSpec;
import org.syncany.plugins.PluginOptionSpec.OptionValidationResult;
import org.syncany.plugins.PluginOptionSpecs;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.StorageTestResult;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

import com.google.common.base.Strings;

public abstract class AbstractInitCommand extends Command implements UserInteractionListener {
	public static final int PASSWORD_MIN_LENGTH = 10;
	public static final int PASSWORD_WARN_LENGTH = 12;
	
	protected InitConsole console;
	protected boolean isInteractive;	

	public AbstractInitCommand() {
		console = InitConsole.getInstance();
	}

	protected ConfigTO createConfigTO(ConnectionTO connectionTO) throws Exception {
		ConfigTO configTO = new ConfigTO();

		configTO.setDisplayName(getDefaultDisplayName());
		configTO.setMachineName(getRandomMachineName());
		configTO.setMasterKey(null); 
		configTO.setConnectionTO(connectionTO); // can be null

		return configTO;
	}

	protected ConnectionTO createConnectionTOFromOptions(OptionSet options, OptionSpec<String> optionPlugin, OptionSpec<String> optionPluginOpts,
			OptionSpec<Void> optionNonInteractive) throws Exception {
		
		TransferPlugin plugin = null;
		Map<String, String> pluginSettings = null;

		List<String> pluginOptionStrings = options.valuesOf(optionPluginOpts);
		Map<String, String> knownPluginSettings = parsePluginSettingsFromOptions(pluginOptionStrings);

		if (!options.has(optionNonInteractive)) {
			if (options.has(optionPlugin)) {
				plugin = initPlugin(options.valueOf(optionPlugin));
			}
			else {
				plugin = askPlugin();
			}

			pluginSettings = askPluginSettings(plugin, knownPluginSettings, false);
		}
		else {
			plugin = initPlugin(options.valueOf(optionPlugin));
			pluginSettings = initPluginSettings(plugin, knownPluginSettings);
		}

		// Create configTO
		ConnectionTO connectionTO = new ConnectionTO();
		
		connectionTO.setType(plugin.getId());
		connectionTO.setSettings(pluginSettings);

		return connectionTO;
	}
	
	protected Map<String, String> parsePluginSettingsFromOptions(List<String> pluginSettingsOptList) throws Exception {
		Map<String, String> pluginOptionValues = new HashMap<String, String>();

		// Fill settings map
		for (String pluginSettingKeyValue : pluginSettingsOptList) {
			String[] keyValue = pluginSettingKeyValue.split("=", 2);

			if (keyValue.length != 2) {
				throw new Exception("Invalid setting: " + pluginSettingKeyValue);
			}

			pluginOptionValues.put(keyValue[0], keyValue[1]);
		}

		return pluginOptionValues;
	}
	
	protected TransferPlugin initPlugin(String pluginStr) throws Exception {
		TransferPlugin plugin = Plugins.get(pluginStr, TransferPlugin.class);

		if (plugin == null) {
			throw new Exception("ERROR: Plugin '" + pluginStr + "' does not exist.");
		}

		return plugin;
	}

	protected Map<String, String> askPluginSettings(TransferPlugin plugin, Map<String, String> knownPluginOptionValues, boolean confirmKnownValues) throws StorageException {
		TransferSettings connection = plugin.createSettings();
		PluginOptionSpecs pluginOptionSpecs = connection.getOptionSpecs();
		
		Map<String, String> pluginOptionValues = new HashMap<String, String>();

		out.println();
		out.println("Connection details for " + plugin.getName() + " connection:");

		for (PluginOptionSpec optionSpec : pluginOptionSpecs.values()) {
			String knownOptionValue = knownPluginOptionValues.get(optionSpec.getId());
			String optionValue = null;
			
			if (knownOptionValue == null) {
				optionValue = askPluginOption(optionSpec, knownOptionValue);
			}
			else {
				if (confirmKnownValues) {
					optionValue = askPluginOption(optionSpec, knownOptionValue);
				}
				else {
					optionValue = knownOptionValue;
				}
			}
			
			pluginOptionValues.put(optionSpec.getId(), optionValue);
		}

		pluginOptionSpecs.validate(pluginOptionValues); // throws error if invalid
		
		return pluginOptionValues;
	}
	
	protected Map<String, String> initPluginSettings(TransferPlugin plugin, Map<String, String> knownPluginOptionValues) throws StorageException {
		if (knownPluginOptionValues == null) {
			knownPluginOptionValues = new HashMap<String, String>();
		}
		
		TransferSettings connection = plugin.createSettings();
		PluginOptionSpecs pluginOptionSpecs = connection.getOptionSpecs();		

		pluginOptionSpecs.validate(knownPluginOptionValues); // throws error if invalid
		
		return knownPluginOptionValues;
	}

	private String askPluginOption(PluginOptionSpec optionSpec, String knownOptionValue) {
		while (true) {
			String value = null;

			// Retrieve value
			if (optionSpec.isSensitive()) {
				// The option is sensitive. Could be either mandatory or optional
				value = askPluginOptionSensitive(optionSpec, knownOptionValue);				
			}
			else if (!optionSpec.isMandatory()) {
				// The option is optional
				value = askPluginOptionOptional(optionSpec, knownOptionValue);	
			}
			else {
				// The option is mandatory, but not sensitive
				value = askPluginOptionNormal(optionSpec, knownOptionValue);
			}

			// Validate result
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
				throw new RuntimeException("Invalid return type: " + validationResult);
			}
		}
	}

	private String askPluginOptionNormal(PluginOptionSpec optionSpec, String knownOptionValue) {
		String value = knownOptionValue;
		
		if (knownOptionValue == null || "".equals(knownOptionValue)) {
			out.printf("- %s: ", optionSpec.getDescription());
			value = console.readLine();
		}
		else {
			out.printf("- %s (%s): ", optionSpec.getDescription(), knownOptionValue);					
			value = console.readLine();
			
			if ("".equals(value)) {
				value = knownOptionValue;
			}
		}
		
		return value;
	}

	private String askPluginOptionOptional(PluginOptionSpec optionSpec, String knownOptionValue) {
		String value = knownOptionValue;
		
		if (knownOptionValue == null || "".equals(knownOptionValue)) {
			out.printf("- %s (optional, default is %s): ", optionSpec.getDescription(), optionSpec.getDefaultValue());
			value = console.readLine();
		}
		else {
			out.printf("- %s (%s): ", optionSpec.getDescription(), knownOptionValue);
			value = console.readLine();
			
			if ("".equals(value)) {
				value = knownOptionValue;
			}
		}		
		
		return value;
	}

	private String askPluginOptionSensitive(PluginOptionSpec optionSpec, String knownOptionValue) {
		String value = knownOptionValue;
		String optionalIndicator = optionSpec.isMandatory() ? "" : ", optional"; 

		if (knownOptionValue == null || "".equals(knownOptionValue)) {
			out.printf("- %s (not displayed%s): ", optionSpec.getDescription(), optionalIndicator);
			value = String.copyValueOf(console.readPassword());
		}
		else {
			out.printf("- %s (***, not displayed%s): ", optionSpec.getDescription(), optionalIndicator);
			value = String.copyValueOf(console.readPassword());
			
			if ("".equals(value)) {
				value = knownOptionValue;
			}
		}
		
		return value;
	}

	protected TransferPlugin askPlugin() {
		TransferPlugin plugin = null;

		List<TransferPlugin> plugins = Plugins.list(TransferPlugin.class);
		String pluginsList = StringUtil.join(plugins, ", ", new StringJoinListener<TransferPlugin>() {
			@Override
			public String getString(TransferPlugin plugin) {
				return plugin.getId();
			}
		});

		while (plugin == null) {
			out.println("Choose a storage plugin. Available plugins are: " + pluginsList);
			out.print("Plugin: ");
			String pluginStr = console.readLine();
			
			plugin = Plugins.get(pluginStr, TransferPlugin.class);

			if (plugin == null) {
				out.println("ERROR: Plugin does not exist.");
				out.println();
			}
		}

		return plugin;
	}

	protected String getRandomMachineName() {
		return CipherUtil.createRandomAlphabeticString(20);
	}

	protected String getDefaultDisplayName() throws UnknownHostException {
		return System.getProperty("user.name");
	}

	protected boolean askRetryConnection() {
		return onUserConfirm(null, "Connection failure", "Would you change the settings and retry the connection");		
	}

	protected void updateConnectionTO(ConnectionTO connectionTO) throws StorageException {
		Map<String, String> newPluginSettings = askPluginSettings(Plugins.get(connectionTO.getType(), TransferPlugin.class), connectionTO.getSettings(), true);
		connectionTO.setSettings(newPluginSettings);
	}
	
	protected void printLink(GenlinkOperationResult operationResult, boolean shortOutput) {
		if (shortOutput) {
			out.println(operationResult.getShareLink());
		}
		else {
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
	
	protected void printTestResult(StorageTestResult testResult) {
		out.println("Details:");
		out.println("- Target connect success: " + testResult.isTargetCanConnect());
		out.println("- Target exists:          " + testResult.isTargetExists());
		out.println("- Target creatable:       " + testResult.isTargetCanCreate());
		out.println("- Target writable:        " + testResult.isTargetCanWrite());
		out.println("- Repo file exists:       " + testResult.isRepoFileExists());
		out.println();		
		
		if (testResult.getException() != null) {
			out.println("Error message (see log file for details):");
			out.println("  " + testResult.getException().getMessage());
		}
	}

	@Override
	public boolean onUserConfirm(String header, String message, String question) {
		if (header != null) {
			out.println();
			out.println(header);
			out.println(Strings.repeat("-", header.length()));
		}
		
		out.println(message);
		out.println();
		
		String yesno = console.readLine(question + " (y/n)? ");
		
		if (!yesno.toLowerCase().startsWith("y") && !"".equals(yesno)) {
			return false;
		}
		else {
			return true;
		}
	}

	@Override
	public void onShowMessage(String message) {
		out.println(message);
	}

	@Override
	public String onUserPassword(String header, String message) {
		out.println();

		if (header != null) {
			out.println(header);
			out.println(Strings.repeat("-", header.length()));
		}
		
		if (!message.trim().endsWith(":")) {
			message += ": ";
		}
		
		char[] passwordChars = console.readPassword(message);
		return String.copyValueOf(passwordChars);
	}
	
	@Override
	public String onUserNewPassword() {
		out.println();
		out.println("The password is used to encrypt data on the remote storage.");
		out.println("Choose wisely!");
		out.println();
		
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password (min. "+PASSWORD_MIN_LENGTH+" chars): ");
			
			if (passwordChars.length < PASSWORD_MIN_LENGTH) {
				out.println("ERROR: This password is not allowed (too short, min. "+PASSWORD_MIN_LENGTH+" chars)");
				out.println();
				
				continue;
			}
			
			char[] confirmPasswordChars = console.readPassword("Confirm: ");
			
			if (!Arrays.equals(passwordChars, confirmPasswordChars)) {
				out.println("ERROR: Passwords do not match.");
				out.println();
				
				continue;
			} 
			
			if (passwordChars.length < PASSWORD_WARN_LENGTH) {
				out.println();
				out.println("WARNING: The password is a bit short. Less than "+PASSWORD_WARN_LENGTH+" chars are not future-proof!");
				String yesno = console.readLine("Are you sure you want to use it (y/n)? ");
				
				if (!yesno.toLowerCase().startsWith("y") && !"".equals(yesno)) {
					out.println();
					continue;
				}
			}
			
			password = new String(passwordChars);			
		}	
		
		return password;
	}
} 
