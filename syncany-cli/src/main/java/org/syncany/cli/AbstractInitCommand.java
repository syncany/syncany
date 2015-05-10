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
package org.syncany.cli;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.cli.util.InitConsole;
import org.syncany.config.to.ConfigTO;
import org.syncany.crypto.CipherUtil;
import org.syncany.operations.daemon.messages.ShowMessageExternalEvent;
import org.syncany.operations.init.GenlinkOperationResult;
import org.syncany.plugins.Plugins;
import org.syncany.plugins.UserInteractionListener;
import org.syncany.plugins.transfer.NestedTransferPluginOption;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.StorageTestResult;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferPluginOption;
import org.syncany.plugins.transfer.TransferPluginOption.ValidationResult;
import org.syncany.plugins.transfer.TransferPluginOptionCallback;
import org.syncany.plugins.transfer.TransferPluginOptionConverter;
import org.syncany.plugins.transfer.TransferPluginOptions;
import org.syncany.plugins.transfer.TransferPluginUtil;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.plugins.transfer.oauth.OAuth;
import org.syncany.plugins.transfer.oauth.OAuthGenerator;
import org.syncany.plugins.transfer.oauth.OAuthTokenFinish;
import org.syncany.plugins.transfer.oauth.OAuthTokenWebListener;
import org.syncany.util.ReflectionUtil;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.Subscribe;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

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

	protected static final char NESTED_OPTIONS_SEPARATOR = '.';
	protected static final String GENERIC_PLUGIN_TYPE_IDENTIFIER = ":type";
	protected static final int PASSWORD_MIN_LENGTH = 10;
	protected static final int PASSWORD_WARN_LENGTH = 12;
	protected static final int OAUTH_TOKEN_WAIT_TIMEOUT = 60;

	protected InitConsole console;
	protected boolean isInteractive;
	protected boolean isHeadless;

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

		// Parse --plugin and --plugin-option values
		List<String> pluginOptionStrings = options.valuesOf(optionPluginOpts);
		Map<String, String> knownPluginSettings = parsePluginSettingsFromOptions(pluginOptionStrings);

		// Validation of some constraints
		if (!options.has(optionPlugin) && knownPluginSettings.size() > 0) {
			throw new IllegalArgumentException("Provided plugin settings without a plugin name.");
		}

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
			// Show OAuth output
			printOAuthInformation(settings);

			// Ask for plugin settings
			List<TransferPluginOption> pluginOptions = TransferPluginOptions.getOrderedOptions(settings.getClass());

			for (TransferPluginOption option : pluginOptions) {
				askPluginSettings(settings, option, knownPluginSettings, "");
			}
		}
		catch (NoSuchFieldException e) {
			logger.log(Level.SEVERE, "No token could be found, maybe user denied access", e);
			throw new StorageException("No token found. Did you accept the authorization?", e);
		}
		catch (TimeoutException e) {
			logger.log(Level.SEVERE, "No token was received in the given time interval", e);
			throw new StorageException("No token was received in the given time interval", e);
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | IOException | InterruptedException | ExecutionException e) {
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

	private void printOAuthInformation(TransferSettings settings) throws StorageException, NoSuchMethodException, SecurityException,
					InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, ExecutionException, InterruptedException, TimeoutException, NoSuchFieldException {
		OAuth oAuthSettings =	settings.getClass().getAnnotation(OAuth.class);

		if (oAuthSettings != null) {
			Constructor<? extends OAuthGenerator> optionCallbackClassConstructor = oAuthSettings.value().getDeclaredConstructor(settings.getClass());
			OAuthGenerator oAuthGenerator = optionCallbackClassConstructor.newInstance(settings);

			if (isHeadless) {
				logger.log(Level.FINE, "User is in headless mode and the plugin is OAuth based");

				if (oAuthGenerator instanceof OAuthGenerator.WithNoRedirectMode) {
					doOAuthInCopyTokenMode(oAuthGenerator);
				}
				else {
					throw new RuntimeException("OAuth based plugin does not support headless mode");
				}
			}
			else {
				doOAuthInRedirectMode(oAuthGenerator, oAuthSettings);
			}

		}
	}

	private void doOAuthInCopyTokenMode(OAuthGenerator generator) throws StorageException {
		URI oAuthURL = ((OAuthGenerator.WithNoRedirectMode) generator).generateAuthUrl();

		out.println();
		out.println("This plugin needs you to authenticate your account so that Syncany can access it.");
		out.printf("Please navigate to the URL below and enter the token:\n\n  %s\n\n", oAuthURL.toString());
		out.print("- Token (paste from URL): ");

		String token = console.readLine();
		generator.checkToken(token, null);
	}

	private void doOAuthInRedirectMode(OAuthGenerator generator, OAuth settings) throws IOException, InterruptedException, ExecutionException, TimeoutException, StorageException {
		OAuthTokenWebListener.Builder tokenListerBuilder = OAuthTokenWebListener.forMode(settings.mode());

		if (settings.callbackPort() != OAuth.RANDOM_PORT) {
			tokenListerBuilder.setPort(settings.callbackPort());
		}

		if (!settings.callbackId().equals(OAuth.PLUGIN_ID)) {
			tokenListerBuilder.setId(settings.callbackId());
		}

		// non standard plugin?
		if (generator instanceof OAuthGenerator.WithInterceptor) {
			tokenListerBuilder.setTokenInterceptor(((OAuthGenerator.WithInterceptor) generator).getInterceptor());
		}

		if (generator instanceof OAuthGenerator.WithExtractor) {
			tokenListerBuilder.setTokenExtractor(((OAuthGenerator.WithExtractor) generator).getExtractor());
		}

		OAuthTokenWebListener tokenListener = tokenListerBuilder.build();

		URI oAuthURL = generator.generateAuthUrl(tokenListener.start());
		Future<OAuthTokenFinish> futureTokenResponse = tokenListener.getToken();

		out.println();
		out.println("This plugin needs you to authenticate your account so that Syncany can access it.");
		out.printf("Please navigate to the URL below and accept the given permissions:\n\n  %s\n\n", oAuthURL.toString());
		out.print("Waiting for authorization...");

		OAuthTokenFinish tokenResponse = futureTokenResponse.get(OAUTH_TOKEN_WAIT_TIMEOUT, TimeUnit.SECONDS);

		if (tokenResponse != null) {
			out.printf(" received token '%s'\n\n", tokenResponse.getToken());
			generator.checkToken(tokenResponse.getToken(), tokenResponse.getCsrfState());
		}
		else {
			out.println(" canceled");
			throw new StorageException("Error while acquiring token, perhaps user denied authorization");
		}
	}

	private void askPluginSettings(TransferSettings settings, TransferPluginOption option, Map<String, String> knownPluginSettings, String nestPrefix)
			throws IllegalAccessException, InstantiationException, StorageException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {

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

	private void askNormalPluginSettings(TransferSettings settings, TransferPluginOption option, Map<String, String> knownPluginSettings,
			String nestPrefix)
			throws StorageException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {

		TransferPluginOptionCallback optionCallback = createOptionCallback(settings, option.getCallback());
		TransferPluginOptionConverter optionConverter = createOptionConverter(settings, option.getConverter());

		if (!isInteractive && !knownPluginSettings.containsKey(nestPrefix + option.getName())) {
			throw new IllegalArgumentException("Missing plugin option (" + nestPrefix + option.getName() + ") in non-interactive mode.");
		}
		else if (knownPluginSettings.containsKey(nestPrefix + option.getName())) {
			settings.setField(option.getField().getName(), knownPluginSettings.get(nestPrefix + option.getName()));
		}
		else if (!option.isVisible()) {
			// Do nothing. Invisible option!
		}
		else {
			callAndPrintPreQueryCallback(optionCallback);

			String optionValue = askPluginOption(settings, option);

			if (optionConverter != null) {
				optionValue = optionConverter.convert(optionValue);
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
	private void askGenericChildPluginSettings(TransferSettings settings, TransferPluginOption option, Map<String, String> knownPluginSettings,
			String nestPrefix)
			throws StorageException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {

		TransferPluginOptionCallback optionCallback = createOptionCallback(settings, option.getCallback());

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
			String nestPrefix) throws StorageException, IllegalAccessException, InstantiationException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {

		TransferPluginOptionCallback optionCallback = createOptionCallback(settings, option.getCallback());

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

		if (option.isSingular() || knownOptionValue == null || "".equals(knownOptionValue)) {
			out.printf("- %s: ", getDescription(settings, option));
			value = console.readLine();
		}
		else {
			out.printf("- %s (%s): ", getDescription(settings, option), knownOptionValue);
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
			String defaultValueDescription = settings.getField(option.getField().getName());

			if (defaultValueDescription == null) {
				defaultValueDescription = "none";
			}

			out.printf("- %s (optional, default is %s): ", getDescription(settings, option), defaultValueDescription);
			value = console.readLine();
		}
		else {
			out.printf("- %s (%s): ", getDescription(settings, option), knownOptionValue);
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

		if (option.isSingular() || knownOptionValue == null || "".equals(knownOptionValue)) {
			out.printf("- %s (not displayed%s): ", getDescription(settings, option), optionalIndicator);
			value = String.copyValueOf(console.readPassword());
		}
		else {
			out.printf("- %s (***, not displayed%s): ", getDescription(settings, option), optionalIndicator);
			value = String.copyValueOf(console.readPassword());

			if ("".equals(value)) {
				value = knownOptionValue;
			}
		}

		return value;
	}

	private String getDescription(TransferSettings settings, TransferPluginOption option) {
		Class<?> clazzForType = ReflectionUtil.getClassFromType(option.getType());

		if (Enum.class.isAssignableFrom(clazzForType)) {
			Object[] enumValues = clazzForType.getEnumConstants();

			if (enumValues == null) {
				throw new RuntimeException("Invalid TransferSettings class found: Enum at " + settings + " has no values");
			}

			logger.log(Level.FINE, "Found enum option, values are: " + StringUtil.join(enumValues, ", "));

			return String.format("%s, choose from %s", option.getDescription(), StringUtil.join(enumValues, ", "));
		}
		else {
			return option.getDescription();
		}
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

	private TransferPluginOptionConverter createOptionConverter(TransferSettings settings,
			Class<? extends TransferPluginOptionConverter> optionConverterClass) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		TransferPluginOptionConverter optionConverter = null;

		if (optionConverterClass != null) {
			Constructor<? extends TransferPluginOptionConverter> optionConverterClassConstructor = optionConverterClass.getDeclaredConstructor(settings.getClass());
			optionConverter = optionConverterClassConstructor.newInstance(settings);
		}

		return optionConverter;
	}

	private TransferPluginOptionCallback createOptionCallback(TransferSettings settings,
			Class<? extends TransferPluginOptionCallback> optionCallbackClass) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		TransferPluginOptionCallback optionCallback = null;

		if (optionCallbackClass != null) {
			Constructor<? extends TransferPluginOptionCallback> optionCallbackClassConstructor = optionCallbackClass.getDeclaredConstructor(settings.getClass());
			optionCallback = optionCallbackClassConstructor.newInstance(settings);
		}

		return optionCallback;
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
				out.println("Note: The link contains the details of your repo connection which typically");
				out.println("      consist of usernames/password of the connection (e.g. FTP user/pass).");
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

		if (testResult.getErrorMessage() != null) {
			out.println("Error message (see log file for details):");
			out.println("  " + testResult.getErrorMessage());
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

	@Subscribe
	public void onShowMessage(ShowMessageExternalEvent messageEvent) {
		out.println();
		out.println(messageEvent.getMessage());
	}

	@Override
	public String onUserPassword(String header, String message) {
		if (!isInteractive) {
			throw new RuntimeException("Repository is encrypted, but no password was given in non-interactive mode.");
		}

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
