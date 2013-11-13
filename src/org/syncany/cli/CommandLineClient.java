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

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.Client;
import org.syncany.config.Config;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.LogFormatter;
import org.syncany.config.Logging;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.util.StringUtil;
import org.syncany.util.StringUtil.StringJoinListener;

public class CommandLineClient extends Client {
	private static final Logger logger = Logger.getLogger(CommandLineClient.class.getSimpleName());	
	
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
			OptionSpec<String> optionLogLevel = parser.acceptsAll(asList("loglevel")).withOptionalArg();
			OptionSpec<Void> optionDebug = parser.acceptsAll(asList("D", "debug"));
			OptionSpec<Void> optionQuiet = parser.acceptsAll(asList("q", "quiet"));		
			
			// Parse global options and operation name
			OptionSet options = parser.parse(args);
			
			// Evaluate options
			// WARNING: Do not re-order unless you know what you are doing!
			initHelpOption(options, optionHelp, options.nonOptionArguments());
			initConfigOption(options, optionLocalDir);
			initLogOption(options, optionLog, optionLogLevel, optionQuiet, optionDebug);
	
			// Run!
			return runCommand(options, options.nonOptionArguments());
		}
		catch (OptionException e) {
			return showErrorAndExit(e.getMessage());
		}
	}	

	private void initHelpOption(OptionSet options, OptionSpec<Void> optionHelp, List<?> nonOptions) {
		if (options.has(optionHelp) || nonOptions.size() == 0) {
			showUsageAndExit();
		}
	}

	private void initLogOption(OptionSet options, OptionSpec<String> optionLog, OptionSpec<String> optionLogLevel, OptionSpec<Void> optionQuiet, OptionSpec<Void> optionDebug) throws SecurityException, IOException {
		initLogHandlers(options, optionLog, optionDebug);		
		initLogLevel(options, optionDebug, optionQuiet, optionLogLevel);		
	}

	private void initLogLevel(OptionSet options, OptionSpec<Void> optionDebug, OptionSpec<Void> optionQuiet, OptionSpec<String> optionLogLevel) {
		Level newLogLevel = null;

		// --debug
		if (options.has(optionDebug)) {
			out.println("debug");
			newLogLevel = Level.ALL;			
		}
		
		// --quiet
		else if (options.has(optionQuiet)) {
			newLogLevel = Level.OFF;
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

	private void initLogHandlers(OptionSet options, OptionSpec<String> optionLog, OptionSpec<Void> optionDebug) throws SecurityException, IOException {
		// --log=<file>
		String logFilePattern = null;
				
		if (options.has(optionLog)) {
			if (!"-".equals(options.valueOf(optionLog))) {
				logFilePattern = options.valueOf(optionLog);
			}			
		}
		else if (config != null && config.getLogDir().exists()) {
			logFilePattern = config.getLogDir()+File.separator+new SimpleDateFormat("yyMMdd").format(new Date())+".log";
		}
		
		if (logFilePattern != null) {	
			Handler fileLogHandler = new FileHandler(logFilePattern, true);			
			fileLogHandler.setFormatter(new LogFormatter());
	
			Logging.addGlobalHandler(fileLogHandler);
		}
				
		// --debug, add console handler
		if (options.has(optionDebug) || (options.has(optionLog) && "-".equals(options.valueOf(optionLog)))) {
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
			localDir = findLocalDirInPath();
		}			
		
		// Load config
		File appDir = new File(localDir+"/"+Config.DEFAULT_DIR_APPLICATION);
		
		if (appDir.exists()) {
			logger.log(Level.INFO, "Loading config from {0} ...", localDir);				

			ConfigTO configTO = loadConfigTO(localDir);
			RepoTO repoTO = loadRepoTO(localDir, configTO);
			
			config = new Config(localDir, configTO, repoTO);
		}		
		else {
			logger.log(Level.INFO, "Not loading config, app dir does not exist: {0}", appDir);
		}
	}		
	
	private ConfigTO loadConfigTO(File localDir) throws Exception {
		File configFile = new File(localDir+"/"+Config.DEFAULT_DIR_APPLICATION+"/"+Config.DEFAULT_FILE_CONFIG);
		
		if (!configFile.exists()) {
			throw new Exception("Cannot find config file at "+configFile+". Try connecting to a repository using 'connect', or 'init' to create a new one.");
		}
		
		return ConfigTO.load(configFile);
	}

	private RepoTO loadRepoTO(File localDir, ConfigTO configTO) throws Exception {
		File repoFile = new File(localDir+"/"+Config.DEFAULT_DIR_APPLICATION+"/"+Config.DEFAULT_FILE_REPO);
		
		if (!repoFile.exists()) {
			throw new Exception("Cannot find repository file at "+repoFile+". Try connecting to a repository using 'connect', or 'init' to create a new one.");
		}
		
		if (CipherUtil.isEncrypted(repoFile)) {
			logger.log(Level.INFO, "Loading encrypted repo file from {0} ...", repoFile);				

			SaltedSecretKey masterKey = configTO.getMasterKey();
			
			if (masterKey == null) {
				throw new Exception("Repo file is encrypted, but master key not set in config file.");
			}
			
			String repoFileStr = CipherUtil.decryptToString(new FileInputStream(repoFile), masterKey);
			
			Serializer serializer = new Persister();
			return serializer.read(RepoTO.class, repoFileStr);			
		}
		else {
			logger.log(Level.INFO, "Loading (unencrypted) repo file from {0} ...", repoFile);
			
			Serializer serializer = new Persister();
			return serializer.read(RepoTO.class, repoFile);
		}
	}
	
	private File findLocalDirInPath() throws IOException {
		File currentSearchFolder = new File(".").getCanonicalFile();
		
		while (currentSearchFolder != null) {
			File possibleAppDir = new File(currentSearchFolder+"/"+Config.DEFAULT_DIR_APPLICATION);
			
			if (possibleAppDir.exists()) {
				return possibleAppDir.getParentFile().getCanonicalFile();
			}
			
			currentSearchFolder = currentSearchFolder.getParentFile();
		}
		 
		return new File(".").getCanonicalFile(); 
	}
	
	private int runCommand(OptionSet options, List<?> nonOptions) throws Exception {
		if (nonOptions.size() == 0) {
			showUsageAndExit();
		}
		
		List<Object> nonOptionsCopy = new ArrayList<Object>(nonOptions);
		String operationName = (String) nonOptionsCopy.remove(0); 
		String[] operationArgs = nonOptionsCopy.toArray(new String[nonOptionsCopy.size()]);
		
		// Find command
		Command command = CommandFactory.getInstance(operationName);

		if (command == null) {
			showErrorAndExit("Given command is unknown: "+operationName);			
		}
		
		command.setClient(this);
		command.setOut(out);
		command.setLocalDir(localDir);
		
		// Pre-init operations
		if (command.initializedLocalDirRequired()) { 
			if (config == null) {
				showErrorAndExit("No repository found in path. Use 'init' command to create one.");			
			}			
		}
		else {
			if (config != null) {
				showErrorAndExit("Repository found in path. Command can only be used outside a repository.");			
			}
		}
		
		// Run!
		int exitCode = command.execute(operationArgs);		
		return exitCode;	
	}
	
	private void showUsageAndExit() {
		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		
		String pluginsStr = StringUtil.join(plugins, ", ", new StringJoinListener<Plugin>() {
			@Override
			public String getString(Plugin plugin) {
				return plugin.getId();
			}			
		});		
		
		out.println("Syncany, version 0.1, copyright (c) 2011-2013 Philipp C. Heckel");
		out.println("Usage: sy [-l|--localdir=<path>] [--log=<path>]");
		out.println("          [--loglevel=OFF|SEVERE|..] [-q|--quiet]");
		out.println("          [-d|--debug] [-h|--help] <command> [<args>]");
		out.println();
		out.println("Global options:");
		out.println("  -l, --localdir=<path>");
		out.println("      Use <path> instead of the current directory as local sync folder. ");
		out.println("      Syncany searches for a '.syncany' folder in the given and all parent");
		out.println("      directories.");
		out.println();
		out.println("  -q, --quiet");
		out.println("      Alias to --loglevel=OFF");
		out.println();
		out.println("  -d, --debug");
		out.println("      Sets the log level to ALL, and print the log to the console.");
		out.println();
		out.println("  -h, --help");
		out.println("      Print this help screen");
		out.println();
		out.println("  --log=<path>");
		out.println("      Log output to the file given by <path>. If - is given, the");
		out.println("      output will be logged to STDOUT (default).");
		out.println();
		out.println("  --loglevel=<level>");
		out.println("      Change log level to <level>. Level can be either of the");
		out.println("      following: OFF, SEVERE, WARNING, INFO, FINE, FINER, FINEST, ALL");
		out.println();
		out.println("Commands:");
		out.println("  init [<args>]");
		out.println("      Initialize the current folder as a Syncany folder (interactive).");		
		out.println("      Currently loaded plugins: "+pluginsStr);
		out.println();
		out.println("      Arguments:");
		out.println("      -p, --plugin=<plugin>            Specify a plugin to use for storage (see list above)");
		out.println("      -P, --plugin-option=<key=value>  Set plugin settings, can/must be used multiple times");
		out.println("      -e, --no-encryption              The new repo will not be encrypted (no password, DON'T USE THIS)");
		out.println("      -a, --no-gzip                    The new repo will not use gzip to compress files");
		out.println("      -a, --advanced                   Asks more questions in the interactive dialog (pick cipher, etc.)");
		out.println();
		out.println("  connect [<args>] [<syncany link>]");
		out.println("      Connect the current folder to an existing Syncany repository. To initialize the connection");
		out.println("      a Syncany link (syncany://..) can be used.");
		out.println();
		out.println("      Arguments:");
		out.println("      -p, --plugin=<plugin>            Specify a plugin to use for storage (see list above)");
		out.println("      -P, --plugin-option=<key=value>  Set plugin settings, can/must be used multiple times");
		out.println();
		out.println("  up [<args>]");
		out.println("      Detect local changes and upload to repo (commit)");
		out.println();
		out.println("      Arguments:");
		out.println("      -F, --force-upload               Force upload even if remote changes exist (will conflict!)");
		out.println("      -c, --no-cleanup                 Do not merge own databases in repo");
		out.println();
		out.println("      In addition to these arguments, all arguments of the 'status' command can be used.");
		out.println();
		out.println("  down");
		out.println("      Detect remote changes and apply locally (update)");
		out.println();
		out.println("  sync [<args>]");
		out.println("      Synchronizes the local folder with the remote repository by calling the down command");	
		out.println("      and the up command. All arguments of the up/down/status/ls-remote commands can be used.");
		out.println();
		out.println("  status [<args>]");
		out.println("      Detect local changes and print to STDOUT.");
		out.println();
		out.println("      Arguments:");
		out.println("      -f, --force-checksum             Force checksum comparison, if not enabled mod. date/size is used");
		out.println();
		out.println("  ls-remote");
		out.println("      Detect remote changes and print to STDOUT.");
		out.println();
		out.println("  watch [<args>]");
		out.println("      Performs the sync-command in a loop. In furture releases, this command will");
		out.println("      watch the file system.");
		out.println();
		out.println("      Arguments:");
		out.println("      -i, --interval=<sec>             Repeat sync every <sec> seconds (default is 30)");
		out.println();
		out.println("      In addition to these arguments, all arguments from the up/down/status/ls-remote commands");
		out.println("      can be used.");
		out.println();		
		out.println("  restore [<args>] <paths>");
		out.println("     Restore the given file paths from the remote repository.");
		out.println();
		out.println("      Arguments:");
		out.println("      -D, --date=<unit(smhDMWY)>       Restore versions prior to the given relative date");
		out.println("      -D, --date=<dd-mm-yy>            Restore versions prior to the given absolute date");
		out.println("      -v, --version=<[-]version>       Restore <version> or go back <version> versions");
		out.println();
		out.println("  log [<paths>]");
		out.println("      Print to STDOUT information stored in the local database about the given file paths or");
		out.println("      all paths known by the database if no path is given. The output format is unstable and");
		out.println("      might change in future releases.");
		out.println();
		
		out.close();		
		System.exit(0);
	}

	private int showErrorAndExit(String errorMessage) {
		out.println("Syncany: "+errorMessage);
		out.println("         Refer to help page using '--help'.");
		out.println();
		
		out.close();		
		System.exit(0);
		
		return 0;
	}
	
}
