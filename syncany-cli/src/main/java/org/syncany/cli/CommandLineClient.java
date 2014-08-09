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

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
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
import org.syncany.config.to.PortTO;
import org.syncany.operations.daemon.messages.CliRequest;
import org.syncany.operations.daemon.messages.CliResponse;
import org.syncany.operations.daemon.messages.MessageFactory;
import org.syncany.operations.daemon.messages.Response;
import org.syncany.util.EnvironmentUtil;

/**
 * The command line client implements a typical CLI. It represents the first entry
 * point for the Syncany command line application and can be used to run all of the
 * supported commands. 
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CommandLineClient extends Client {
	private static final Logger logger = Logger.getLogger(CommandLineClient.class.getSimpleName());
	
	private static final String SERVER_SCHEMA = "http://";
	private static final String SERVER_HOSTNAME = "localhost";
	private static final String SERVER_REST_API = "/api/rs";
	
	private static final String LOG_FILE_PATTERN = "syncany.log";
	private static final int LOG_FILE_COUNT = 4;
	private static final int LOG_FILE_LIMIT = 25000000; // 25 MB
	
	private static final Pattern HELP_TEXT_RESOURCE_PATTERN = Pattern.compile("\\%RESOURCE:([^%]+)\\%");
	private static final String HELP_TEXT_RESOURCE_ROOT = "/" + CommandLineClient.class.getPackage().getName().replace(".", "/") + "/";
	private static final String HELP_TEXT_HELP_SKEL_RESOURCE = "cmd/help.skel";
	private static final String HELP_TEXT_VERSION_SHORT_SKEL_RESOURCE = "incl/version_short.skel";
	private static final String HELP_TEXT_VERSION_FULL_SKEL_RESOURCE = "incl/version_full.skel";
	private static final String HELP_TEXT_USAGE_SKEL_RESOURCE = "incl/usage.skel";
	private static final String HELP_TEXT_CMD_SKEL_RESOURCE = "cmd/help.%s.skel";

	private static final String MAN_PAGE_MAIN = "sy";
	private static final String MAN_PAGE_COMMAND_FORMAT = "sy-%s";
	
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
			OptionSpec<Void> optionDebug = parser.acceptsAll(asList("D", "debug"));
			OptionSpec<Void> optionShortVersion = parser.acceptsAll(asList("v"));
			OptionSpec<Void> optionFullVersion = parser.acceptsAll(asList("vv"));
			
			// Parse global options and operation name
			OptionSet options = parser.parse(args);
			List<?> nonOptions = options.nonOptionArguments();

			// -v, -vv, --version
			initVersionOptions(options, optionShortVersion, optionFullVersion);
			initHelpOrUsage(options, nonOptions, optionHelp);
			
			// Run!
			List<Object> nonOptionsCopy = new ArrayList<Object>(nonOptions);
			String commandName = (String) nonOptionsCopy.remove(0); 
			String[] commandArgs = nonOptionsCopy.toArray(new String[0]);
			
			// Find command
			Command command = CommandFactory.getInstance(commandName);

			if (command == null) {			
				return showErrorAndExit("Given command is unknown: "+commandName);			
			}
			
			// Potentially show help
			if (options.has(optionHelp)) {
				return showCommandHelpAndExit(commandName);
			}
						
			// Pre-init operations
			initLocalDir(options, optionLocalDir);
			initConfigIfRequired(command.getRequiredCommandScope(), localDir);
			initLogOption(options, optionLog, optionLogLevel, optionLogPrint, optionDebug);
			
			// Init command
			return runCommand(command, commandName, commandArgs);
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while initializing or running command.", e);
			return showErrorAndExit(e.getMessage());
		}
	}	

	private void initVersionOptions(OptionSet options, OptionSpec<Void> optionShortVersion, OptionSpec<Void> optionFullVersion) throws IOException {
		if (options.has(optionShortVersion)) {
			showShortVersionAndExit();
		}
		else if (options.has(optionFullVersion)) {
			showFullVersionAndExit();
		}
	}

	private void initHelpOrUsage(OptionSet options, List<?> nonOptions, OptionSpec<Void> optionHelp) throws IOException {
		if (nonOptions.size() == 0) {
			if (options.has(optionHelp)) {
				showHelpAndExit();
			}
			else {
				showUsageAndExit();
			}
		}		
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

	private void initConfigIfRequired(CommandScope requiredCommandScope, File localDir) throws ConfigException {
		switch (requiredCommandScope) {
		case INITIALIZED_LOCALDIR:
			if (!ConfigHelper.configExists(localDir)) {
				showErrorAndExit("No repository found in path, or configured plugin not installed. Use 'sy init' to create one.");				
			}
			
			config = ConfigHelper.loadConfig(localDir);
			
			if (config == null) {
				showErrorAndExit("Invalid config in " + localDir);			
			}	

			break;
			
		case UNINITIALIZED_LOCALDIR:
			if (ConfigHelper.configExists(localDir)) {
				showErrorAndExit("Repository found in path. Command can only be used outside a repository.");				
			}
			
			break;				

		case ANY:
		default:		
			break;
		}
	}

	private void initLogOption(OptionSet options, OptionSpec<String> optionLog, OptionSpec<String> optionLogLevel, OptionSpec<Void> optionLogPrint, OptionSpec<Void> optionDebug) throws SecurityException, IOException {
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
				showErrorAndExit("Invalid log level given "+newLogLevelStr+"'");
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

	private void initLogHandlers(OptionSet options, OptionSpec<String> optionLog, OptionSpec<Void> optionLogPrint, OptionSpec<Void> optionDebug) throws SecurityException, IOException {
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
			portFile = new File(config.getAppDir(), Config.FILE_PORT);
		}
		
		boolean localDirHandledInDaemonScope = portFile != null && portFile.exists();
		boolean needsToRunInInitializedScope = command.getRequiredCommandScope() == CommandScope.INITIALIZED_LOCALDIR;
		boolean sendToRest = localDirHandledInDaemonScope && needsToRunInInitializedScope;
		
		if (sendToRest) {
			return sendToRest(command, commandName, commandArgs, portFile);
		}
		else {
			return runLocally(command, commandArgs);
		}
	}
	
	private int runLocally(Command command, String[] commandArgs) {
		command.setClient(this);
		command.setOut(out);
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
		// Read port config (for daemon) from port file
		PortTO portConfig = readPortConfig(portFile);

		// Create authentication details
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(
			new AuthScope(SERVER_HOSTNAME, portConfig.getPort()), 
			new UsernamePasswordCredentials(portConfig.getUser().getUsername(), portConfig.getUser().getPassword()));

		// Create client with authentication details
		CloseableHttpClient client = HttpClients
			.custom()
			.setDefaultCredentialsProvider(credentialsProvider)
			.build();

		String SERVER_URI = SERVER_SCHEMA + SERVER_HOSTNAME + ":" + portConfig.getPort() + SERVER_REST_API;
		HttpPost post = new HttpPost(SERVER_URI);

		try {
			logger.log(Level.INFO, "Sending HTTP Request to: " + SERVER_URI);
			
			// Create and send HTTP/REST request
			CliRequest cliRequest = new CliRequest();
			
			cliRequest.setId(Math.abs(new Random().nextInt()));
			cliRequest.setRoot(config.getLocalDir().getAbsolutePath());
			cliRequest.setCommand(commandName);
			cliRequest.setCommandArgs(Arrays.asList(commandArgs));
			
			post.setEntity(new StringEntity(MessageFactory.toRequest(cliRequest)));
			
			// Handle response
			HttpResponse httpResponse = client.execute(post);
			logger.log(Level.FINE, "Received HttpResponse: " + httpResponse);
			
			String responseStr = IOUtils.toString(httpResponse.getEntity().getContent());			
			logger.log(Level.FINE, "Responding to message with responseString: " + responseStr);
			
			Response response = MessageFactory.createResponse(responseStr);
			
			if (response instanceof CliResponse) {
				out.print(((CliResponse) response).getOutput());	
			}
			else {
				out.println(response.getMessage());
			}
			
			return 0;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Command " + command.toString() + " FAILED. ", e);
			return showErrorAndExit(e.getMessage());
		}		
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

	private void showShortVersionAndExit() throws IOException {
		printHelpTextAndExit(HELP_TEXT_VERSION_SHORT_SKEL_RESOURCE);
	}
	
	private void showFullVersionAndExit() throws IOException {
		printHelpTextAndExit(HELP_TEXT_VERSION_FULL_SKEL_RESOURCE);
	}

	private void showUsageAndExit() throws IOException {
		printHelpTextAndExit(HELP_TEXT_USAGE_SKEL_RESOURCE);
	}

	private void showHelpAndExit() throws IOException {
		// Try opening man page (if on Linux)
		if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			execManPageAndExit(MAN_PAGE_MAIN);
		}
		
		// Fallback (and on Windows): Display man page on STDOUT
		printHelpTextAndExit(HELP_TEXT_HELP_SKEL_RESOURCE);
	}
	
	private int showCommandHelpAndExit(String commandName) throws IOException {
		// Try opening man page (if on Linux)
		if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			String commandManPage = String.format(MAN_PAGE_COMMAND_FORMAT, commandName);
			execManPageAndExit(commandManPage);
		}
		
		// Fallback (and on Windows): Display man page on STDOUT
		String helpTextResource = String.format(HELP_TEXT_CMD_SKEL_RESOURCE, commandName);
		return printHelpTextAndExit(helpTextResource);		
	}

	private void execManPageAndExit(String manPage) {
		try {
			Runtime runtime = Runtime.getRuntime();
			Process manProcess = runtime.exec(new String[] { "sh", "-c", "man " + manPage + " > /dev/tty" });
			
			int manProcessExitCode = manProcess.waitFor();
			
			if (manProcessExitCode == 0) {
				System.exit(0); 
			}
		}
		catch (Exception e) {
			// Don't care!
		}
	}

	private int printHelpTextAndExit(String helpTextResource) throws IOException {
		String fullHelpTextResource = HELP_TEXT_RESOURCE_ROOT + helpTextResource;
		InputStream helpTextInputStream = CommandLineClient.class.getResourceAsStream(fullHelpTextResource);
		
		if (helpTextInputStream == null) {
			showErrorAndExit("No detailed help text available for this command.");
		}
		
		for (String line : IOUtils.readLines(helpTextInputStream)) {
			line = replaceVariables(line);			
			out.println(line.replaceAll("\\s$", ""));			
		}
		
		out.close();		
		System.exit(0);
		
		return -1; // Never reached
	}

	private String replaceVariables(String line) throws IOException {
		Properties applicationProperties = Client.getApplicationProperties();
		
		for (Entry<Object, Object> applicationProperty : applicationProperties.entrySet()) {
			String variableName = String.format("%%%s%%", applicationProperty.getKey());
			
			if (line.contains(variableName)) {
				line = line.replace(variableName, (String) applicationProperty.getValue());
			}
		}
		
		Matcher includeResourceMatcher = HELP_TEXT_RESOURCE_PATTERN.matcher(line);
		
		if (includeResourceMatcher.find()) {
			String includeResource = HELP_TEXT_RESOURCE_ROOT + includeResourceMatcher.group(1);
			InputStream includeResourceInputStream = CommandLineClient.class.getResourceAsStream(includeResource);
			String includeResourceStr = IOUtils.toString(includeResourceInputStream);

			line = includeResourceMatcher.replaceAll(includeResourceStr);
			line = replaceVariables(line);
		}
		
		return line;
	}

	private int showErrorAndExit(String errorMessage) {
		out.println("Error: "+errorMessage);
		out.println("       Refer to help page using '--help'.");
		out.println();
		
		out.close();	
		System.exit(0);
		
		return -1; // Never reached
	}
}
