/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.simpleframework.xml.core.Persister;
import org.syncany.Client;
import org.syncany.config.Config;
import org.syncany.config.ConfigException;
import org.syncany.config.ConfigHelper;
import org.syncany.config.LogFormatter;
import org.syncany.config.Logging;
import org.syncany.config.UserConfig;
import org.syncany.config.to.PortTO;
import org.syncany.operations.OperationOptions;
import org.syncany.operations.daemon.DaemonOperation;
import org.syncany.operations.daemon.WebServer;
import org.syncany.operations.daemon.messages.AlreadySyncingResponse;
import org.syncany.operations.daemon.messages.BadRequestResponse;
import org.syncany.operations.daemon.messages.api.FolderRequest;
import org.syncany.operations.daemon.messages.api.FolderResponse;
import org.syncany.operations.daemon.messages.api.Request;
import org.syncany.operations.daemon.messages.api.Response;
import org.syncany.operations.daemon.messages.api.XmlMessageFactory;
import org.syncany.util.EnvironmentUtil;
import org.syncany.util.PidFileUtil;
import org.syncany.util.StringUtil;

/**
 * The command line client implements a typical CLI. It represents the first entry
 * point for the Syncany command line application and can be used to run all of the
 * supported commands.
 *
 * <p>The responsibilities of the command line client include the parsing and interpretation
 * of global options (like log file, debugging), displaying of help pages, and executing
 * commands. It furthermore detects if a local folder is handled by the daemon and, if so,
 * passes the command to the daemon via REST.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CommandLineClient extends Client {
	private static final Logger logger = Logger.getLogger(CommandLineClient.class.getSimpleName());

	private static final String SERVER_SCHEMA = "https://";
	private static final String SERVER_HOSTNAME = "127.0.0.1";
	private static final String SERVER_REST_API = WebServer.API_ENDPOINT_REST_XML;

	private static final String LOG_FILE_PATTERN = "syncany.log";
	private static final int LOG_FILE_COUNT = 4;
	private static final int LOG_FILE_LIMIT = 25000000; // 25 MB

	private static final String HELP_TEXT_RESOURCE_ROOT = "/" + CommandLineClient.class.getPackage().getName().replace(".", "/") + "/";
	private static final String HELP_TEXT_HELP_SKEL_RESOURCE = "cmd/help.skel";
	private static final String HELP_TEXT_VERSION_SHORT_SKEL_RESOURCE = "incl/version_short.skel";
	private static final String HELP_TEXT_VERSION_FULL_SKEL_RESOURCE = "incl/version_full.skel";
	private static final String HELP_TEXT_USAGE_SKEL_RESOURCE = "incl/usage.skel";
	private static final String HELP_TEXT_CMD_SKEL_RESOURCE = "cmd/help.%s.skel";

	private static final String MAN_PAGE_MAIN = "sy";
	private static final String MAN_PAGE_COMMAND_FORMAT = "sy-%s";

	private Config config;
	
	private String[] args;
	private File localDir;

	private PrintStream out;

	static {
		Logging.init();
		Logging.disableLogging();
	}

	public CommandLineClient(String[] args) {
		this.args = args;
		this.out = System.out;
	}

	public void setOut(OutputStream out) {
		this.out = new PrintStream(out);
	}

	public int start() throws Exception {
		// WARNING: Do not re-order methods unless you know what you are doing!

		try {
			// Define global options
			OptionParser parser = new OptionParser();
			parser.allowsUnrecognizedOptions();

			OptionSpec<Void> optionHelp = parser.acceptsAll(asList("h", "help"));
			OptionSpec<File> optionLocalDir = parser.acceptsAll(asList("l", "localdir")).withRequiredArg().ofType(File.class);
			OptionSpec<String> optionLog = parser.acceptsAll(asList("log")).withRequiredArg();
			OptionSpec<Void> optionLogPrint = parser.acceptsAll(asList("print"));
			OptionSpec<String> optionLogLevel = parser.acceptsAll(asList("loglevel")).withOptionalArg();
			OptionSpec<Void> optionDebug = parser.acceptsAll(asList("d", "debug"));
			OptionSpec<Void> optionShortVersion = parser.acceptsAll(asList("v"));
			OptionSpec<Void> optionFullVersion = parser.acceptsAll(asList("vv"));

			// Parse global options and operation name
			OptionSet options = parser.parse(args);
			List<?> nonOptions = options.nonOptionArguments();

			// -v, -vv, --version
			int versionOptionsCode = initVersionOptions(options, optionShortVersion, optionFullVersion);

			if (versionOptionsCode != -1) {
				// Version information was displayed, exit.
				return versionOptionsCode;
			}

			int helpOrUsageCode = initHelpOrUsage(options, nonOptions, optionHelp);

			if (helpOrUsageCode != -1) {
				// Help or usage was displayed, exit.
				return helpOrUsageCode;
			}

			// Run!
			List<Object> nonOptionsCopy = new ArrayList<Object>(nonOptions);
			String commandName = (String) nonOptionsCopy.remove(0);
			String[] commandArgs = nonOptionsCopy.toArray(new String[0]);

			// Find command
			Command command = CommandFactory.getInstance(commandName);

			if (command == null) {
				return showErrorAndExit("Given command is unknown: " + commandName);
			}

			// Potentially show help
			if (options.has(optionHelp)) {
				return showCommandHelpAndExit(commandName);
			}

			// Pre-init operations
			initLocalDir(options, optionLocalDir);

			int configInitCode = initConfigIfRequired(command.getRequiredCommandScope(), localDir);

			if (configInitCode != 0) {
				return configInitCode;
			}

			initLogOption(options, optionLog, optionLogLevel, optionLogPrint, optionDebug);

			// Init command
			return runCommand(command, commandName, commandArgs);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while initializing or running command.", e);
			return showErrorAndExit(e.getMessage());
		}
	}

	private int initVersionOptions(OptionSet options, OptionSpec<Void> optionShortVersion, OptionSpec<Void> optionFullVersion) throws IOException {
		if (options.has(optionShortVersion)) {
			return showShortVersionAndExit();
		}
		else if (options.has(optionFullVersion)) {
			return showFullVersionAndExit();
		}

		return -1;
	}

	private int initHelpOrUsage(OptionSet options, List<?> nonOptions, OptionSpec<Void> optionHelp) throws IOException {
		if (nonOptions.size() == 0) {
			if (options.has(optionHelp)) {
				return showHelpAndExit();
			}
			else {
				return showUsageAndExit();
			}
		}

		return -1;
	}

	private void initLocalDir(OptionSet options, OptionSpec<File> optionLocalDir) throws ConfigException, Exception {
		// Find config or use --localdir option
		if (options.has(optionLocalDir)) {
			localDir = options.valueOf(optionLocalDir);
		}
		else {
			File currentDir = new File(".").getAbsoluteFile();
			localDir = ConfigHelper.findLocalDirInPath(currentDir);

			// If no local directory was found, choose current directory
			if (localDir == null) {
				localDir = currentDir;
			}
		}
	}

	/**
	 * Initializes configuration if required.
	 * Returns non-zero if something goes wrong.
	 */
	private int initConfigIfRequired(CommandScope requiredCommandScope, File localDir) throws ConfigException {
		switch (requiredCommandScope) {
		case INITIALIZED_LOCALDIR:
			if (!ConfigHelper.configExists(localDir)) {
				return showErrorAndExit("No repository found in path, or configured plugin not installed. Use 'sy init' to create one.");
			}

			config = ConfigHelper.loadConfig(localDir);

			if (config == null) {
				return showErrorAndExit("Invalid config in " + localDir);
			}

			break;

		case UNINITIALIZED_LOCALDIR:
			if (ConfigHelper.configExists(localDir)) {
				return showErrorAndExit("Repository found in path. Command can only be used outside a repository.");
			}

			break;

		case ANY:
		default:
			break;
		}

		return 0;
	}

	private void initLogOption(OptionSet options, OptionSpec<String> optionLog, OptionSpec<String> optionLogLevel, OptionSpec<Void> optionLogPrint,
			OptionSpec<Void> optionDebug) throws SecurityException, IOException {

		initLogHandlers(options, optionLog, optionLogPrint, optionDebug);
		initLogLevel(options, optionDebug, optionLogLevel);
	}

	private void initLogLevel(OptionSet options, OptionSpec<Void> optionDebug, OptionSpec<String> optionLogLevel) {
		Level newLogLevel = null;

		// --debug
		if (options.has(optionDebug)) {
			newLogLevel = Level.ALL;
		}

		// --loglevel=<level>
		else if (options.has(optionLogLevel)) {
			String newLogLevelStr = options.valueOf(optionLogLevel);

			try {
				newLogLevel = Level.parse(newLogLevelStr);
			}
			catch (IllegalArgumentException e) {
				showErrorAndExit("Invalid log level given " + newLogLevelStr + "'");
			}
		}
		else {
			newLogLevel = Level.INFO;
		}

		// Add handler to existing loggers, and future ones
		Logging.setGlobalLogLevel(newLogLevel);

		// Debug output
		if (options.has(optionDebug)) {
			out.println("debug");
			out.println(String.format("Application version: %s", Client.getApplicationVersionFull()));

			logger.log(Level.INFO, "Application version: {0}", Client.getApplicationVersionFull());
		}
	}

	private void initLogHandlers(OptionSet options, OptionSpec<String> optionLog, OptionSpec<Void> optionLogPrint, OptionSpec<Void> optionDebug)
			throws SecurityException, IOException {

		// --log=<file>
		String logFilePattern = null;

		if (options.has(optionLog)) {
			if (!"-".equals(options.valueOf(optionLog))) {
				logFilePattern = options.valueOf(optionLog);
			}
		}
		else if (config != null && config.getLogDir().exists()) {
			logFilePattern = config.getLogDir() + File.separator + LOG_FILE_PATTERN;
		}
		else {
			logFilePattern = UserConfig.getUserLogDir() + File.separator + LOG_FILE_PATTERN;
		}

		if (logFilePattern != null) {
			Handler fileLogHandler = new FileHandler(logFilePattern, LOG_FILE_LIMIT, LOG_FILE_COUNT, true);
			fileLogHandler.setFormatter(new LogFormatter());

			Logging.addGlobalHandler(fileLogHandler);
		}

		// --debug, add console handler
		if (options.has(optionDebug) || options.has(optionLogPrint) || (options.has(optionLog) && "-".equals(options.valueOf(optionLog)))) {
			Handler consoleLogHandler = new ConsoleHandler();
			consoleLogHandler.setFormatter(new LogFormatter());

			Logging.addGlobalHandler(consoleLogHandler);
		}
	}

	private int runCommand(Command command, String commandName, String[] commandArgs) {
		File portFile = null;

		if (config != null) {
			portFile = config.getPortFile();
		}

		File daemonPidFile = new File(UserConfig.getUserConfigDir(), DaemonOperation.PID_FILE);

		boolean localDirHandledInDaemonScope = portFile != null && portFile.exists();
		boolean daemonRunning = PidFileUtil.isProcessRunning(daemonPidFile);
		boolean needsToRunInInitializedScope = command.getRequiredCommandScope() == CommandScope.INITIALIZED_LOCALDIR;
		boolean sendToRest = daemonRunning & localDirHandledInDaemonScope && needsToRunInInitializedScope;

		command.setOut(out);

		if (sendToRest) {
			if (command.canExecuteInDaemonScope()) {
				return sendToRest(command, commandName, commandArgs, portFile);
			}
			else {
				logger.log(Level.SEVERE, "Command not allowed when folder is daemon-managed: " + command.toString());
				return showErrorAndExit("Command not allowed when folder is daemon-managed");
			}
		}
		else {
			return runLocally(command, commandArgs);
		}
	}

	private int runLocally(Command command, String[] commandArgs) {
		command.setConfig(config);
		command.setLocalDir(localDir);

		// Run!
		try {
			return command.execute(commandArgs);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Command " + command.toString() + " FAILED. ", e);
			return showErrorAndExit(e.getMessage());
		}
	}

	private int sendToRest(Command command, String commandName, String[] commandArgs, File portFile) {
		try {
			// Read port config (for daemon) from port file
			PortTO portConfig = readPortConfig(portFile);

			// Create authentication details
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(
					new AuthScope(SERVER_HOSTNAME, portConfig.getPort()),
					new UsernamePasswordCredentials(portConfig.getUser().getUsername(), portConfig.getUser().getPassword()));

			// Allow all hostnames in CN; this is okay as long as hostname is localhost/127.0.0.1!
			// See: https://github.com/syncany/syncany/pull/196#issuecomment-52197017
			X509HostnameVerifier hostnameVerifier = new AllowAllHostnameVerifier();

			// Fetch the SSL context (using the user key/trust store)
			SSLContext sslContext = UserConfig.createUserSSLContext();

			// Create client with authentication details
			CloseableHttpClient client = HttpClients
					.custom()
					.setSslcontext(sslContext)
					.setHostnameVerifier(hostnameVerifier)
					.setDefaultCredentialsProvider(credentialsProvider)
					.build();

			// Build and send request, print response
			Request request = buildFolderRequestFromCommand(command, commandName, commandArgs, config.getLocalDir().getAbsolutePath());
			String serverUri = SERVER_SCHEMA + SERVER_HOSTNAME + ":" + portConfig.getPort() + SERVER_REST_API;

			String xmlMessageString = XmlMessageFactory.toXml(request);
			StringEntity xmlMessageEntity = new StringEntity(xmlMessageString);

			HttpPost httpPost = new HttpPost(serverUri);
			httpPost.setEntity(xmlMessageEntity);

			logger.log(Level.INFO, "Sending HTTP Request to: " + serverUri);
			logger.log(Level.FINE, httpPost.toString());
			logger.log(Level.FINE, xmlMessageString);

			HttpResponse httpResponse = client.execute(httpPost);
			int exitCode = handleRestResponse(command, httpResponse);

			return exitCode;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Command " + command.toString() + " FAILED. ", e);
			return showErrorAndExit(e.getMessage());
		}
	}

	private int handleRestResponse(Command command, HttpResponse httpResponse) throws Exception {
		logger.log(Level.FINE, "Received HttpResponse: " + httpResponse);

		String responseStr = IOUtils.toString(httpResponse.getEntity().getContent());
		logger.log(Level.FINE, "Responding to message with responseString: " + responseStr);

		Response response = XmlMessageFactory.toResponse(responseStr);

		if (response instanceof FolderResponse) {
			FolderResponse folderResponse = (FolderResponse) response;
			command.printResults(folderResponse.getResult());

			return 0;
		}
		else if (response instanceof AlreadySyncingResponse) {
			out.println("Daemon is already syncing, please retry later.");
			return 1;
		}
		else if (response instanceof BadRequestResponse) {
			out.println(response.getMessage());
			return 1;
		}

		return 1;
	}

	private Request buildFolderRequestFromCommand(Command command, String commandName, String[] commandArgs, String root) throws Exception {
		String thisPackage = BadRequestResponse.class.getPackage().getName(); // TODO [low] Medium-dirty hack.
		String camelCaseMessageType = StringUtil.toCamelCase(commandName) + FolderRequest.class.getSimpleName();
		String fqMessageClassName = thisPackage + "." + camelCaseMessageType;

		FolderRequest folderRequest;

		try {
			Class<? extends FolderRequest> folderRequestClass = Class.forName(fqMessageClassName).asSubclass(FolderRequest.class);
			folderRequest = folderRequestClass.newInstance();
		}
		catch (Exception e) {
			logger.log(Level.INFO, "Could not find FQCN " + fqMessageClassName, e);
			throw new Exception("Cannot read request class from request type: " + commandName, e);
		}

		OperationOptions operationOptions = command.parseOptions(commandArgs);
		int requestId = Math.abs(new Random().nextInt());

		folderRequest.setRoot(root);
		folderRequest.setId(requestId);
		folderRequest.setOptions(operationOptions);

		return folderRequest;
	}

	private PortTO readPortConfig(File portFile) {
		try {
			return new Persister().read(PortTO.class, portFile);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "ERROR: Could not read portFile to connect to daemon.", e);

			showErrorAndExit("Cannot connect to daemon.");
			return null; // Never reached!
		}
	}

	private int showShortVersionAndExit() throws IOException {
		return printHelpTextAndExit(HELP_TEXT_VERSION_SHORT_SKEL_RESOURCE);
	}

	private int showFullVersionAndExit() throws IOException {
		return printHelpTextAndExit(HELP_TEXT_VERSION_FULL_SKEL_RESOURCE);
	}

	private int showUsageAndExit() throws IOException {
		return printHelpTextAndExit(HELP_TEXT_USAGE_SKEL_RESOURCE);
	}

	private int showHelpAndExit() throws IOException {
		// Try opening man page (if on Linux)
		if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			int manPageReturnCode = execManPageAndExit(MAN_PAGE_MAIN);

			if (manPageReturnCode == 0) { // Success
				return manPageReturnCode;
			}
		}

		// Fallback (and on Windows): Display man page on STDOUT
		return printHelpTextAndExit(HELP_TEXT_HELP_SKEL_RESOURCE);
	}

	private int showCommandHelpAndExit(String commandName) throws IOException {
		// Try opening man page (if on Linux)
		if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			String commandManPage = String.format(MAN_PAGE_COMMAND_FORMAT, commandName);
			int manPageReturnCode = execManPageAndExit(commandManPage);

			if (manPageReturnCode == 0) { // Success
				return manPageReturnCode;
			}
		}

		// Fallback (and on Windows): Display man page on STDOUT
		String helpTextResource = String.format(HELP_TEXT_CMD_SKEL_RESOURCE, commandName);
		return printHelpTextAndExit(helpTextResource);
	}

	private int execManPageAndExit(String manPage) {
		try {
			Runtime runtime = Runtime.getRuntime();
			Process manProcess = runtime.exec(new String[] { "sh", "-c", "man " + manPage + " > /dev/tty" });

			int manProcessExitCode = manProcess.waitFor();

			if (manProcessExitCode == 0) {
				return 0;
			}
		}
		catch (Exception e) {
			// Don't care!
		}
		return 1;
	}

	private int printHelpTextAndExit(String helpTextResource) throws IOException {
		String fullHelpTextResource = HELP_TEXT_RESOURCE_ROOT + helpTextResource;
		InputStream helpTextInputStream = CommandLineClient.class.getResourceAsStream(fullHelpTextResource);

		if (helpTextInputStream == null) {
			return showErrorAndExit("No detailed help text available for this command.");
		}

		for (String line : IOUtils.readLines(helpTextInputStream)) {
			line = replaceVariables(line);
			out.println(line.replaceAll("\\s$", ""));
		}

		out.close();

		return 0;
	}

	private String replaceVariables(String line) throws IOException {
		Properties applicationProperties = Client.getApplicationProperties();

		for (Entry<Object, Object> applicationProperty : applicationProperties.entrySet()) {
			String variableName = String.format("%%%s%%", applicationProperty.getKey());

			if (line.contains(variableName)) {
				line = line.replace(variableName, (String) applicationProperty.getValue());
			}
		}

		return line;
	}

	private int showErrorAndExit(String errorMessage) {
		out.println("Error: " + errorMessage);
		out.println("       Refer to help page using '--help'.");
		out.println();

		out.close();

		return 1;
	}
}
