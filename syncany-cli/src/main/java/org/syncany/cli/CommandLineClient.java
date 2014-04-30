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
import static org.syncany.cli.CommandScope.INITIALIZED_LOCALDIR;
import static org.syncany.cli.CommandScope.UNINITIALIZED_LOCALDIR;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
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
import org.syncany.Client;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.ConfigHelper;
import org.syncany.config.LogFormatter;
import org.syncany.config.Logging;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

/**
 * The command line client implements a typical CLI. It represents the first entry
 * point for the Syncany command line application and can be used to run all of the
 * supported commands. 
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CommandLineClient extends Client {
	private static final Logger logger = Logger.getLogger(CommandLineClient.class.getSimpleName());
	
	private static final String LOG_FILE_PATTERN = "syncany.log";
	private static final int LOG_FILE_COUNT = 4;
	private static final int LOG_FILE_LIMIT = 25000000; // 25 MB
	
	private static final Pattern HELP_TEXT_RESOURCE_PATTERN = Pattern.compile("\\%RESOURCE:([^%]+)\\%");
	private static final String HELP_TEXT_HELP_SKEL_RESOURCE = "/help/help.skel";
	private static final String HELP_TEXT_USAGE_SKEL_RESOURCE = "/help/usage.skel";
	private static final String HELP_TEXT_CMD_SKEL_RESOURCE = "/help/cmd/help.%CMD%.skel";
	private static final String HELP_VAR_CMD= "%CMD%";
	private static final String HELP_VAR_VERSION= "%VERSION%";
	private static final String HELP_VAR_PLUGINS = "%PLUGINS%";
	private static final String HELP_VAR_LOGFORMATS = "%LOGFORMATS%";
	
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
			
			// Parse global options and operation name
			OptionSet options = parser.parse(args);
			
			// Evaluate options
			// WARNING: Do not re-order unless you know what you are doing!
			initConfigOption(options, optionLocalDir);
			initLogOption(options, optionLog, optionLogLevel, optionLogPrint, optionDebug);
	
			// Run!
			return runCommand(options, optionHelp, options.nonOptionArguments());
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while initializing or running command.", e);
			return showErrorAndExit(e.getMessage());
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
			out.println("debug");
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

	private void initConfigOption(OptionSet options, OptionSpec<File> optionLocalDir) throws ConfigException, Exception {
		// Find config or use --config option
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
		
		// Load config
		config = ConfigHelper.loadConfig(localDir);
	}					
	
	private int runCommand(OptionSet options, OptionSpec<Void> optionHelp, List<?> nonOptions) throws Exception {
		if (nonOptions.size() == 0) {
			if (options.has(optionHelp)) {
				showHelpAndExit();
			}
			else {
				showUsageAndExit();
			}
		}
		
		List<Object> nonOptionsCopy = new ArrayList<Object>(nonOptions);
		String commandName = (String) nonOptionsCopy.remove(0); 
		String[] commandArgs = nonOptionsCopy.toArray(new String[nonOptionsCopy.size()]);
		
		// Find command
		Command command = CommandFactory.getInstance(commandName);

		if (command == null) {			
			return showErrorAndExit("Given command is unknown: "+commandName);			
		}
		
		// Potentially show help
		if (options.has(optionHelp)) {
			return showCommandHelpAndExit(commandName);
		}
		
		// Init command
		command.setClient(this);
		command.setOut(out);
		command.setLocalDir(localDir);
		
		// Pre-init operations
		if (command.getRequiredCommandScope() == INITIALIZED_LOCALDIR) { 
			if (config == null) {
				return showErrorAndExit("No repository found in path, or configured plugin not installed. Use 'sy init' to create one.");			
			}			
		}
		else if (command.getRequiredCommandScope() == UNINITIALIZED_LOCALDIR) {
			if (config != null) {
				return showErrorAndExit("Repository found in path. Command can only be used outside a repository.");			
			}
		}
		
		// Run!
		try {
			int exitCode = command.execute(commandArgs);
			return exitCode;
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Command "+ commandName+" FAILED. ", e);
			return showErrorAndExit(e.getMessage());
		}	
	}
	
	private void showUsageAndExit() throws IOException {
		printHelpTextAndExit(HELP_TEXT_USAGE_SKEL_RESOURCE);
	}

	private void showHelpAndExit() throws IOException {
		printHelpTextAndExit(HELP_TEXT_HELP_SKEL_RESOURCE);
	}
	
	private int showCommandHelpAndExit(String commandName) throws IOException {
		String helpTextResource = HELP_TEXT_CMD_SKEL_RESOURCE.replace(HELP_VAR_CMD, commandName);
		return printHelpTextAndExit(helpTextResource);		
	}

	private int printHelpTextAndExit(String helpTextResource) throws IOException {
		InputStream helpTextInputStream = CommandLineClient.class.getResourceAsStream(helpTextResource);
		
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
		if (line.contains(HELP_VAR_VERSION)) {
			line = line.replace(HELP_VAR_VERSION, getVersionStr());
		}
		
		if (line.contains(HELP_VAR_PLUGINS)) {
			line = line.replace(HELP_VAR_PLUGINS, getPluginsStr());	
		}
		
		if (line.contains(HELP_VAR_LOGFORMATS)) {
			line = line.replace(HELP_VAR_LOGFORMATS, getLogFormatsStr());	
		}
		
		Matcher includeResourceMatcher = HELP_TEXT_RESOURCE_PATTERN.matcher(line);
		
		if (includeResourceMatcher.find()) {
			String includeResource = includeResourceMatcher.group(1);
			InputStream includeResourceInputStream = CommandLineClient.class.getResourceAsStream(includeResource);
			String includeResourceStr = IOUtils.toString(includeResourceInputStream);

			line = includeResourceMatcher.replaceAll(includeResourceStr);
			line = replaceVariables(line);
		}
		
		return line;
	}

	private String getLogFormatsStr() {
		return StringUtil.join(LogCommand.getSupportedFormats(), ", ");
	}

	private String getPluginsStr() {
		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		
		String pluginsStr = StringUtil.join(plugins, ", ", new StringJoinListener<Plugin>() {
			@Override
			public String getString(Plugin plugin) {
				return plugin.getId();
			}			
		});	
		
		return pluginsStr;
	}

	private String getVersionStr() {
		String versionStr = Client.getApplicationVersion();
		
		if (!Client.isApplicationRelease()) {
			if (Client.getApplicationRevision() != null && !"".equals(Client.getApplicationRevision())) {
				versionStr += ", rev. "+Client.getApplicationRevision();
			}
			else {
				versionStr += ", no rev.";
			}
		}
		
		return versionStr;
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
