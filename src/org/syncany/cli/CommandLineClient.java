package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.Console;
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

public class CommandLineClient extends Client {
	private static final Logger logger = Logger.getLogger(CommandLineClient.class.getSimpleName());	
	
	private String[] args;	
	private File configFile;
	
	private PrintStream out;
	private Console console;
		
	static {
		Logging.init();
		Logging.disableLogging();		
	}
	
	public CommandLineClient(String[] args) {
		this.args = args;		
		this.out = System.out;
		this.console = System.console();
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
			OptionSpec<File> optionConfig = parser.acceptsAll(asList("c", "config")).withRequiredArg().ofType(File.class);
			OptionSpec<String> optionLog = parser.acceptsAll(asList("l", "log")).withRequiredArg();
			OptionSpec<String> optionLogLevel = parser.acceptsAll(asList("v", "loglevel")).withOptionalArg();
			OptionSpec<Void> optionDebug = parser.acceptsAll(asList("D", "debug"));
			OptionSpec<Void> optionQuiet = parser.acceptsAll(asList("q", "quiet"));		
			
			// Parse global options and operation name
			OptionSet options = parser.parse(args);
			
			// Evaluate options
			// WARNING: Do not re-order unless you know what you are doing!
			initHelpOption(options, optionHelp);
			initConfigOption(options, optionConfig);
			initLogOption(options, optionLog, optionLogLevel, optionQuiet, optionDebug);
	
			// Run!
			return runOperation(options, options.nonOptionArguments());
		}
		catch (OptionException e) {
			return showErrorAndExit(e.getMessage());
		}
	}	

	private void initHelpOption(OptionSet options, OptionSpec<Void> optionHelp) {
		if (options.has(optionHelp)) {
			showUsageAndExit();
		}
	}

	private int runOperation(OptionSet options, List<?> nonOptions) throws Exception {
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
		
		// Pre-init operations
		if (command.needConfigFile()) { 
			// Check config (required for these operations)
			if (configFile == null) {
				showErrorAndExit("No repository found in path. Use 'init' command to create one.");			
			}			
		}
		else {
			// Check config (NOT allowed for these operations)
			if (configFile != null) {
				showErrorAndExit("Repository found in path. Command can only be used outside a repository.");			
			}
		}
		
		// Run!
		int exitCode = command.execute(operationArgs);		
		return exitCode;	
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

	private void initConfigOption(OptionSet options, OptionSpec<File> optionConfig) throws ConfigException, Exception {
		// Find config or use --config option
		if (options.has(optionConfig)) {
			configFile = options.valueOf(optionConfig);
		}
		else {
			configFile = findConfigFileInPath();
		}		
		
		// Load config
		if (configFile != null) {
			logger.log(Level.INFO, "Loading config from {0} ...", configFile);				

			ConfigTO configTO = loadConfigTO(configFile);
			RepoTO repoTO = loadRepoTO(configTO);
			
			config = new Config(configTO, repoTO);
			
			// Create folders
			logger.log(Level.INFO, "Creating directories ...");				
			createDirectories();
		}				
	}		
	
	private ConfigTO loadConfigTO(File configFile) throws ConfigException {
		return ConfigTO.load(configFile);
	}

	private RepoTO loadRepoTO(ConfigTO configTO) throws Exception {
		File repoFile = findRepoFile(configTO);
		
		if (repoFile == null) {
			throw new Exception("Cannot find repository file. Try connecting to a repository using 'connect', or 'init' to create a new one.");
		}
		
		if (CipherUtil.isEncrypted(repoFile)) {
			String password = configTO.getPassword();
			
			if (password == null) {
				password = askPassword();
				configTO.setPassword(password);
			}
			
			String repoFileStr = CipherUtil.decryptToString(new FileInputStream(repoFile), password);
			
			Serializer serializer = new Persister();
			return serializer.read(RepoTO.class, repoFileStr);			
		}
		else {
			Serializer serializer = new Persister();
			return serializer.read(RepoTO.class, repoFile);
		}
	}
	
	private File findRepoFile(ConfigTO configTO) {
		if (configTO.getAppDir() != null) {
			File repoFile = new File(configTO.getAppDir()+"/"+Config.DEFAULT_FILE_REPO);
			
			if (repoFile.exists()) {
				return repoFile;
			}
		}
		
		File configFileFolder = new File(configTO.getConfigFile()).getParentFile();
		File repoFile = new File(configFileFolder+"/"+Config.DEFAULT_FILE_REPO);
		
		if (repoFile.exists()) {
			return repoFile;
		}
		
		return null;
	}

	private File findConfigFileInPath() throws IOException {
		File currentSearchFolder = new File(".").getCanonicalFile();
		
		while (currentSearchFolder != null) {
			File possibleConfigFile = new File(currentSearchFolder+"/"+Config.DEFAULT_DIR_APPLICATION+"/"+Config.DEFAULT_FILE_CONFIG);
			
			if (possibleConfigFile.exists()) {
				return possibleConfigFile.getCanonicalFile();
			}
			
			currentSearchFolder = currentSearchFolder.getParentFile();
		}
		 
		return null; 
	}
	
	private String askPassword() {
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password: ");			
			password = new String(passwordChars);			
		}	
		
		return password;
	}
	
	private void showUsageAndExit() {
		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		String pluginsStr = "";
		
		for (int i=0; i<plugins.size(); i++) {
			pluginsStr += plugins.get(i).getId();
			if (i < plugins.size()-1) { pluginsStr += ", "; }			
		}
		
		out.println("Syncany, version 0.1, copyright (c) 2011-2013 Philipp C. Heckel");
		out.println("Usage: sy [-c|--config=<path>] [-l|--log=<path>]");
		out.println("          [-v|--loglevel=OFF|SEVERE|..] [-q|--quiet]");
		out.println("          [-d|--debug] [-h|--help] <command> [<args>]");
		out.println();
		out.println("Global options:");
		out.println("  -c, --config=<path>");
		out.println("      Load config file from <path>. If <path> is in a '.syncany'-folder");
		out.println("      'localDir' is assumed to be the parent directory. If the -c option");
		out.println("      is not given, Syncany searches for a '.syncany'-folder in all parent");
		out.println("      directories.");
		out.println();
		out.println("  -l, --log=<path>");
		out.println("      Log output to the file given by <path>. If - is given, the");
		out.println("      output will be logged to STDOUT (default).");
		out.println();
		out.println("  -v, --loglevel=<level>");
		out.println("      Change log level to <level>. Level can be either of the");
		out.println("      following: OFF, SEVERE, WARNING, INFO, FINE, FINER, FINEST, ALL");
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
		out.println("Commands:");
		out.println("  init -i");
		out.println("  init <plugin> [<folder>]");
		out.println("      Initialize <folder> as a Syncany folder (default is current folder). This");
		out.println("      command creates and initializes a skeleton config file for the plugin <plugin>.");
		out.println();
		out.println("      The <plugin> attribute can be any of the loaded plugins.");		
		out.println("      Currently loaded are: "+pluginsStr);
		out.println();
		out.println("      Arguments:");
		out.println("      -i, --interactive       Run interactive mode to init Syncany folder.");
		out.println();
		out.println("  up [<args>]");
		out.println("      Detect local changes and upload to repo (commit)");
		out.println();
		out.println("      Arguments:");
		out.println("      -F, --force-upload      Force upload even if remote changes exist (will conflict!).");
		out.println("      -c, --no-cleanup        Do not merge own databases in repo.");
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
		out.println("      -f, --force-checksum    Force checksum comparison, if not enabled mod. date/size is used.");
		out.println();
		out.println("  ls-remote");
		out.println("      Detect remote changes and print to STDOUT.");
		out.println();
		out.println("  watch [<args>]");
		out.println("      Performs the sync-command in a loop. In furture releases, this command will");
		out.println("      watch the file system.");
		out.println();
		out.println("      Arguments:");
		out.println("      -i, --interval=<sec>    Repeat sync every <sec> seconds (default is 30).");
		out.println();
		out.println("      In addition to these arguments, all arguments from the up/down/status/ls-remote commands");
		out.println("      can be used.");
		out.println();		
		
		System.exit(1);
	}

	private int showErrorAndExit(String errorMessage) {
		out.println("Syncany: "+errorMessage);
		out.println("         Refer to help page using '--help'.");
		out.println();

		System.exit(1);		
		return 1;
	}
	
}
