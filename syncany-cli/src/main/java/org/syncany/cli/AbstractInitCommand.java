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

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.cli.util.InitConsole;
import org.syncany.config.to.ConfigTO;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.init.GenlinkOperationResult;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.plugins.transfer.NestedTransferPluginOption;
import org.syncany.plugins.transfer.TransferPluginOptionCallback;
import org.syncany.plugins.transfer.TransferPluginOptionConverter;
import org.syncany.plugins.transfer.TransferPluginOptions;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.StorageTestResult;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferPluginOption;
import org.syncany.plugins.transfer.TransferPluginUtil;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.plugins.transfer.TransferPluginOption.ValidationResult;
import org.syncany.util.ReflectionUtil;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The abstract init command provides multiple shared methods for the 'init'
 * and 'connect' command. Both commands must provide the ability to
 * query a user for transfer settings or parse settings from the command line
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class AbstractInitCommand extends Command implements UserInteractionListener {
	private static final Logger logger = Logger.getLogger(AbstractInitCommand.class.getName());

	private static final char NESTED_OPTIONS_SEPARATOR = '.';
	private static final String GENERIC_PLUGIN_TYPE_IDENTIFIER = ":type";
	private static final int PASSWORD_MIN_LENGTH = 10;
	private static final int PASSWORD_WARN_LENGTH = 12;

	protected InitConsole console;
	protected boolean isInteractive;

	public AbstractInitCommand() {
		console = InitConsole.getInstance();
	}

	protected ConfigTO createConfigTO(TransferSettings transferSettings) throws Exception {
		ConfigTO configTO = new ConfigTO();

		configTO.setDisplayName(getDefaultDisplayName());
		configTO.setMachineName(getRandomMachineName());
		configTO.setMasterKey(null);
		configTO.setTransferSettings(transferSettings); // can be null

		return configTO;
	}

	protected TransferSettings createTransferSettingsFromOptions(OptionSet options, OptionSpec<String> optionPlugin,
			OptionSpec<String> optionPluginOpts) throws Exception {

		TransferPlugin plugin;
		TransferSettings transferSettings;

		List<String> pluginOptionStrings = options.valuesOf(optionPluginOpts);
		Map<String, String> knownPluginSettings = parsePluginSettingsFromOptions(pluginOptionStrings);

		if (!options.has(optionPlugin) && knownPluginSettings.size() > 0) {
			throw new IllegalArgumentException("Provided plugin settings without a plugin name.");
		}

		isInteractive = !options.has(optionPlugin) && knownPluginSettings.size() == 0;

		plugin = options.has(optionPlugin) ? initPlugin(options.valueOf(optionPlugin)) : askPlugin();
		transferSettings = askPluginSettings(plugin.createEmptySettings(), knownPluginSettings);

		return transferSettings;
	}

	private Map<String, String> parsePluginSettingsFromOptions(List<String> pluginSettingsOptList) throws Exception {
		Map<String, String> pluginOptionValues = new HashMap<>();

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

	private TransferPlugin initPlugin(String pluginStr) throws Exception {
		TransferPlugin plugin = Plugins.get(pluginStr, TransferPlugin.class);

		if (plugin == null) {
			throw new Exception("ERROR: Plugin '" + pluginStr + "' does not exist.");
		}

		return plugin;
	}

	private TransferSettings askPluginSettings(TransferSettings settings, Map<String, String> knownPluginSettings) throws StorageException {
		if (isInteractive) {
			out.println();
			out.println("Connection details for " + settings.getType() + " connection:");
		}
		else {
			logger.log(Level.INFO, "Non interactive mode");
		}

		try {
			List<TransferPluginOption> pluginOptions = TransferPluginOptions.getOrderedOptions(settings.getClass());

			for (TransferPluginOption option : pluginOptions) {
				askPluginSettings(settings, option, knownPluginSettings, "");
			}
		}
		catch (InstantiationException | IllegalAccessException e) {
			logger.log(Level.SEVERE, "Unable to execute option generator", e);
			throw new RuntimeException("Unable to execute option generator: " + e.getMessage());
		}

		if (!settings.isValid()) {
			if (askRetryInvalidSettings(settings.getReasonForLastValidationFail())) {
				return askPluginSettings(settings, knownPluginSettings);
			}
			
			throw new StorageException("Validation failed: " + settings.getReasonForLastValidationFail());
		}

		logger.log(Level.INFO, "Settings are " + settings.toString());

		return settings;
	}

	private void askPluginSettings(TransferSettings settings, TransferPluginOption option, Map<String, String> knownPluginSettings, String nestPrefix)
			throws IllegalAccessException, InstantiationException, StorageException {

		if (option instanceof NestedTransferPluginOption) {
			Class<?> childPluginTransferSettingsClass = ReflectionUtil.getClassFromType(option.getType());
			boolean isGenericChildPlugin = TransferSettings.class.equals(childPluginTransferSettingsClass);
			
			if (isGenericChildPlugin) {
				askGenericChildPluginSettings(settings, option, knownPluginSettings, nestPrefix);
			}
			else {
				askConreteChildPluginSettings(settings, (NestedTransferPluginOption) option, knownPluginSettings, nestPrefix);
			}
		}
		else {
			askNormalPluginSettings(settings, option, knownPluginSettings, nestPrefix);
		}
	}

	private void askNormalPluginSettings(TransferSettings settings, TransferPluginOption option, Map<String, String> knownPluginSettings, String nestPrefix)
			throws StorageException, InstantiationException, IllegalAccessException {

		Class<? extends TransferPluginOptionCallback> optionCallbackClass = option.getCallback();
		TransferPluginOptionCallback optionCallback = optionCallbackClass != null ? optionCallbackClass.newInstance() : null;
		Class<? extends TransferPluginOptionConverter> optionConverterClass = option.getConverter();

		if (!isInteractive && !knownPluginSettings.containsKey(nestPrefix + option.getName())) {
			throw new IllegalArgumentException("Missing plugin option (" + nestPrefix + option.getName() + ") in non-interactive mode.");
		}
		else if (knownPluginSettings.containsKey(nestPrefix + option.getName())) {
			settings.setField(option.getField().getName(), knownPluginSettings.get(nestPrefix + option.getName()));
		}
		else {
			callAndPrintPreQueryCallback(optionCallback);

			String optionValue = askPluginOption(settings, option);

			if (optionConverterClass != null) {
				optionValue = optionConverterClass.newInstance().convert(optionValue);
			}

			settings.setField(option.getField().getName(), optionValue);

			callAndPrintPostQueryCallback(optionCallback, optionValue);
		}
	}

	/**
	 * Queries the user for a plugin (which plugin to use?) and then
	 * asks for all of the plugin's settings.
	 * 
	 * <p>This case is triggered by a field looking like this:
	 * <tt>private TransferSettings childPluginSettings;</tt> 
	 */
	private void askGenericChildPluginSettings(TransferSettings settings, TransferPluginOption option, Map<String, String> knownPluginSettings, String nestPrefix)
			throws StorageException, IllegalAccessException, InstantiationException {

		Class<? extends TransferPluginOptionCallback> optionCallbackClass = option.getCallback();
		TransferPluginOptionCallback optionCallback = optionCallbackClass != null ? optionCallbackClass.newInstance() : null;

		if (isInteractive) {
			callAndPrintPreQueryCallback(optionCallback);		

			out.println();
			out.println(option.getDescription() + ":");			
		}

		TransferPlugin childPlugin = null;
		Class<? extends TransferPlugin> pluginClass = TransferPluginUtil.getTransferPluginClass(settings.getClass());

		// Non-interactive: Plugin settings might be given via command line
		try {
			childPlugin = initPlugin(knownPluginSettings.get(nestPrefix + option.getName() + GENERIC_PLUGIN_TYPE_IDENTIFIER));
		}
		catch (Exception e) {
			if (!isInteractive) {
				throw new IllegalArgumentException("Missing nested plugin type (" + nestPrefix + option.getName() + GENERIC_PLUGIN_TYPE_IDENTIFIER
						+ ") in non-interactive mode.");
			}
		}

		// Interactive mode: Ask for sub-plugin
		while (childPlugin == null) {
			childPlugin = askPlugin(pluginClass);
		}

		if (isInteractive) {
			out.println();
		}

		// Create nested/child settings
		TransferSettings childSettings = childPlugin.createEmptySettings();

		settings.setField(option.getField().getName(), childSettings);
		nestPrefix = nestPrefix + option.getName() + NESTED_OPTIONS_SEPARATOR;

		for (TransferPluginOption nestedOption : TransferPluginOptions.getOrderedOptions(childSettings.getClass())) {
			askPluginSettings(childSettings, nestedOption, knownPluginSettings, nestPrefix);
		}
		
		if (isInteractive) {
			callAndPrintPostQueryCallback(optionCallback, null);		
		}
	}

	/**
	 * Asks the user for all of the child plugin's settings.
	 * 
	 * <p>This case is triggered by a field looking like this:
	 * <tt>private LocalTransferSettings localChildPluginSettings;</tt> 
	 */
	private void askConreteChildPluginSettings(TransferSettings settings, NestedTransferPluginOption option, Map<String, String> knownPluginSettings,
			String nestPrefix) throws StorageException, IllegalAccessException, InstantiationException {
		
		Class<? extends TransferPluginOptionCallback> optionCallbackClass = option.getCallback();
		TransferPluginOptionCallback optionCallback = optionCallbackClass != null ? optionCallbackClass.newInstance() : null;

		if (isInteractive) {
			callAndPrintPreQueryCallback(optionCallback);		

			out.println();
			out.println(option.getDescription() + ":");			
		}

		for (TransferPluginOption nestedPluginOption : option.getOptions()) {
			Class<?> nestedTransferSettingsClass = ReflectionUtil.getClassFromType(option.getType());

			if (nestedTransferSettingsClass == null) {
				throw new RuntimeException("No class found for type: " + option.getType());
			}

			TransferSettings nestedSettings = (TransferSettings) nestedTransferSettingsClass.newInstance();

			settings.setField(option.getField().getName(), nestedSettings);
			nestPrefix = nestPrefix + option.getName() + NESTED_OPTIONS_SEPARATOR;

			askPluginSettings(nestedSettings, nestedPluginOption, knownPluginSettings, nestPrefix);
		}
		
		if (isInteractive) {
			callAndPrintPostQueryCallback(optionCallback, null);		
		}
	}
	
	private void callAndPrintPreQueryCallback(TransferPluginOptionCallback optionCallback) {
		if (optionCallback != null) {
			String preQueryMessage = optionCallback.preQueryCallback();
			
			if (preQueryMessage != null) {
				out.println(preQueryMessage);
			}
		}
	}
	
	private void callAndPrintPostQueryCallback(TransferPluginOptionCallback optionCallback, String optionValue) {
		if (optionCallback != null) {
			String postQueryMessage = optionCallback.postQueryCallback(optionValue);
			
			if (postQueryMessage != null) {
				out.println(postQueryMessage);
			}
		}
	}

	private String askPluginOption(TransferSettings settings, TransferPluginOption option) throws StorageException {
		while (true) {
			String value;

			// Retrieve value
			if (option.isSensitive()) {
				// The option is sensitive. Could be either mandatory or optional
				value = askPluginOptionSensitive(settings, option);
			}
			else if (!option.isRequired()) {
				// The option is optional
				value = askPluginOptionOptional(settings, option);
			}
			else {
				// The option is mandatory, but not sensitive
				value = askPluginOptionNormal(settings, option);
			}

			if ("".equals(value)) {
				value = null;
			}

			// Validate result
			ValidationResult validationResult = option.isValid(value);

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
				return value;

			default:
				throw new RuntimeException("Invalid return type: " + validationResult);
			}
		}
	}

	private String askPluginOptionNormal(TransferSettings settings, TransferPluginOption option) throws StorageException {
		String knownOptionValue = settings.getField(option.getField().getName());
		String value = knownOptionValue;

		if (knownOptionValue == null || "".equals(knownOptionValue)) {
			out.printf("- %s: ", option.getDescription());
			value = console.readLine();
		}
		else {
			out.printf("- %s (%s): ", option.getDescription(), knownOptionValue);
			value = console.readLine();

			if ("".equals(value)) {
				value = knownOptionValue;
			}
		}

		return value;
	}

	private String askPluginOptionOptional(TransferSettings settings, TransferPluginOption option) throws StorageException {
		String knownOptionValue = settings.getField(option.getField().getName());
		String value = knownOptionValue;

		if (knownOptionValue == null || "".equals(knownOptionValue)) {
			out.printf("- %s (optional, default is %s): ", option.getDescription(), settings.getField(option.getField().getName()));
			value = console.readLine();
		}
		else {
			out.printf("- %s (%s): ", option.getDescription(), knownOptionValue);
			value = console.readLine();

			if ("".equals(value)) {
				value = knownOptionValue;
			}
		}

		return value;
	}

	private String askPluginOptionSensitive(TransferSettings settings, TransferPluginOption option) throws StorageException {
		String knownOptionValue = settings.getField(option.getField().getName());
		String value = knownOptionValue;
		String optionalIndicator = option.isRequired() ? "" : ", optional";

		if (knownOptionValue == null || "".equals(knownOptionValue)) {
			out.printf("- %s (not displayed%s): ", option.getDescription(), optionalIndicator);
			value = String.copyValueOf(console.readPassword());
		}
		else {
			out.printf("- %s (***, not displayed%s): ", option.getDescription(), optionalIndicator);
			value = String.copyValueOf(console.readPassword());

			if ("".equals(value)) {
				value = knownOptionValue;
			}
		}

		return value;
	}

	protected TransferPlugin askPlugin() {
		return askPlugin(null);
	}

	protected TransferPlugin askPlugin(final Class<? extends TransferPlugin> ignoreTransferPluginClass) {
		TransferPlugin plugin = null;
		final List<TransferPlugin> plugins = Plugins.list(TransferPlugin.class);

		Iterables.removeIf(plugins, new Predicate<TransferPlugin>() {
			@Override
			public boolean apply(TransferPlugin transferPlugin) {
				return ignoreTransferPluginClass == transferPlugin.getClass();
			}
		});

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

			if (plugin == null || ignoreTransferPluginClass == plugin.getClass()) {
				out.println("ERROR: Plugin does not exist or cannot be used.");
				out.println();

				plugin = null;
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

	protected boolean askRetryInvalidSettings(String failReason) {
		return onUserConfirm("Validation failure", failReason, "Would you change the settings");
	}

	protected boolean askRetryConnection() {
		return onUserConfirm(null, "Connection failure", "Would you change the settings and retry the connection");
	}

	protected TransferSettings updateTransferSettings(TransferSettings transferSettings) throws StorageException {
		try {
			return askPluginSettings(transferSettings, new HashMap<String, String>());
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Unable to reload old plugin settings", e);
			throw new StorageException("Unable to reload old plugin settings: " + e.getMessage());
		}
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
			char[] passwordChars = console.readPassword("Password (min. " + PASSWORD_MIN_LENGTH + " chars): ");

			if (passwordChars.length < PASSWORD_MIN_LENGTH) {
				out.println("ERROR: This password is not allowed (too short, min. " + PASSWORD_MIN_LENGTH + " chars)");
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
				out.println("WARNING: The password is a bit short. Less than " + PASSWORD_WARN_LENGTH + " chars are not future-proof!");
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
