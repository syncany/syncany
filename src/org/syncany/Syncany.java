package org.syncany;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.Config;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.ConfigTO;
import org.syncany.config.LogFormatter;
import org.syncany.config.Logging;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.operations.DaemonOperation.DaemonOperationOptions;
import org.syncany.operations.InitOperation;
import org.syncany.operations.Operation.OperationOptions;
import org.syncany.operations.RestoreOperation.RestoreOperationOptions;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.SyncUpOperation.SyncUpOperationOptions;
import org.syncany.util.FileUtil;

public class Syncany {
	private static final Logger logger = Logger.getLogger(Syncany.class.getSimpleName());	
	public enum OperationType { SYNC_UP, SYNC_DOWN, SYNC, STATUS, REMOTE_STATUS, INIT, RESTORE, DAEMON };
	
	private String[] args;
	private Client client;

	private OperationType operationType;
	private OperationOptions operationOptions;
	
	private File configFile;
	private Config config;
		
	static {
		Logging.init();
	}
	
	public static void main(String[] args) throws Exception {
		new Syncany(args).start();
	}
	
	public Syncany(String[] args) {
		this.args = args;
		this.client = null;
	}
	
	public void start() throws Exception {
		readCommandLineArguments(args);		
		runOperation();
	}	

	private void readCommandLineArguments(String[] args) throws Exception {
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

		initOperation(options, options.nonOptionArguments());
	}	

	private void initHelpOption(OptionSet options, OptionSpec<Void> optionHelp) {
		if (options.has(optionHelp)) {
			showUsageAndExit();
		}
	}

	private void initOperation(OptionSet options, List<?> nonOptions) throws Exception {
		if (nonOptions.size() == 0) {
			showUsageAndExit();
		}
		
		List<Object> nonOptionsCopy = new ArrayList<Object>(nonOptions);
		String aOperationType = (String) nonOptionsCopy.remove(0); 
		String[] aOperationArguments = nonOptions.toArray(new String[nonOptions.size()]);
		
		if ("up".equals(aOperationType)) {
			initSyncUpOperation(aOperationArguments);
		}
		else if ("down".equals(aOperationType)) {
			this.operationType = OperationType.SYNC_DOWN;
		}
		else if ("sync".equals(aOperationType)) {
			this.operationType = OperationType.SYNC;
		}
		else if ("status".equals(aOperationType)) {			
			initStatusOperation(aOperationArguments);
		}
		else if ("ls-remote".equals(aOperationType)) {
			this.operationType = OperationType.REMOTE_STATUS;
		}
		else if ("init".equals(aOperationType)) {
			this.operationType = OperationType.INIT;
		}
		else if ("restore".equals(aOperationType)) {
			this.operationType = OperationType.RESTORE;
			/*
			Operation operation = OperationFactory.getInstance(aOperationType);
			
			operation.setConfig(config);
			operation.setOptions(aOperationArguments);	
			
			operation.execute();*/
		}
		else if ("daemon".equals(aOperationType)) {
			initDaemonOperation(aOperationArguments);			
		}
		else {
			showErrorAndExit("Given command is unknown.");
		}		
	}

	private void initStatusOperation(String[] operationArgs) {
		StatusOperationOptions aOperationOptions = new StatusOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<Void> optionForceChecksum = parser.acceptsAll(asList("f", "force-checksum"));
		
		OptionSet options = parser.parse(operationArgs);	
		
		// --force-checksum
		aOperationOptions.setForceChecksum(options.has(optionForceChecksum));
		
		operationType = OperationType.STATUS;
		operationOptions = aOperationOptions;		
	}

	private void initDaemonOperation(String[] operationArgs) {
		DaemonOperationOptions aOperationOptions = new DaemonOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<Integer> optionInterval = parser.acceptsAll(asList("i", "interval")).withRequiredArg().ofType(Integer.class);
		
		OptionSet options = parser.parse(operationArgs);	
		
		// --interval
		if (options.has(optionInterval)) {
			aOperationOptions.setInterval(options.valueOf(optionInterval)*1000);
		}
		
		operationType = OperationType.DAEMON;
		operationOptions = aOperationOptions;		
	}


	private void initSyncUpOperation(String[] operationArgs) {
		SyncUpOperationOptions aOperationOptions = new SyncUpOperationOptions();

		OptionParser parser = new OptionParser();		
		OptionSpec<Void> optionNoCleanup = parser.acceptsAll(asList("c", "no-cleanup"));
		OptionSpec<Void> optionForce = parser.acceptsAll(asList("f", "force"));
		
		OptionSet options = parser.parse(operationArgs);
		
		aOperationOptions.setCleanupEnabled(!options.has(optionNoCleanup));
		aOperationOptions.setForceEnabled(options.has(optionForce));
		
		operationType = OperationType.SYNC_UP;
		operationOptions = aOperationOptions;
	}

	private void initLogOption(OptionSet options, OptionSpec<String> optionLog, OptionSpec<String> optionLogLevel, OptionSpec<Void> optionQuiet, OptionSpec<Void> optionDebug) throws SecurityException, IOException {
		// Reset logging
		Logging.disableLogging();
		
		initLogHandlers(options, optionLog, optionDebug);		
		initLogLevel(options, optionDebug, optionQuiet, optionLogLevel);		
	}

	private void initLogLevel(OptionSet options, OptionSpec<Void> optionDebug, OptionSpec<Void> optionQuiet, OptionSpec<String> optionLogLevel) {
		Level newLogLevel = null;

		// --debug
		if (options.has(optionDebug)) {
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
		else if (config.getLogDir().exists()) {
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
		if (options.has(optionConfig)) {
			configFile = options.valueOf(optionConfig);
		}
		else {
			configFile = findConfigFileInPath();
		}		
		
		config = new Config(ConfigTO.load(configFile));
	}		
	
	private File findConfigFileInPath() throws IOException {
		File currentSearchFolder = new File(".").getCanonicalFile();
		
		while (currentSearchFolder != null) {
			File possibleConfigFile = new File(currentSearchFolder+"/.syncany/config.json");
			
			if (possibleConfigFile.exists()) {
				return possibleConfigFile.getCanonicalFile();
			}
			
			currentSearchFolder = currentSearchFolder.getParentFile();
		}
		 
		return null; 
	}
		
	private void initClient(File configFile) throws Exception {
		if (configFile == null) {
			showErrorAndExit("No repository found in path. Use 'init' command to create one.");			
		}
		
		logger.log(Level.INFO, "Loading config from {0} ...", configFile);
				
		client = new Client();
		client.setConfig(config);
		client.createDirectories();
	}
	
	private void runOperation() throws Exception {
		if (operationType == OperationType.INIT) {
			new InitOperation().execute();
		}
		else {
			// Create client object (necessary for these operations)
			initClient(configFile);
			
			// Run operation
			if (operationType == OperationType.SYNC_UP) {
				client.up((SyncUpOperationOptions) operationOptions); // TODO [medium] ugly
			}
			else if (operationType == OperationType.SYNC_DOWN) {
				client.down();
			}
			else if (operationType == OperationType.SYNC) {
				client.sync();
			}
			else if (operationType == OperationType.STATUS) {
				ChangeSet changeSet = client.status((StatusOperationOptions) operationOptions);
				
				for (File newFile : changeSet.getNewFiles()) {
					System.out.println("A "+FileUtil.getRelativePath(client.getConfig().getLocalDir(), newFile));
				}
	
				for (File changedFile : changeSet.getChangedFiles()) {
					System.out.println("M "+FileUtil.getRelativePath(client.getConfig().getLocalDir(), changedFile));
				}
				
				for (File deletedFile : changeSet.getDeletedFiles()) {
					System.out.println("D "+FileUtil.getRelativePath(client.getConfig().getLocalDir(), deletedFile));
				}	
			}
			else if (operationType == OperationType.REMOTE_STATUS) {
				List<RemoteFile> remoteStatus = client.remoteStatus();
				
				for (RemoteFile unknownRemoteFile : remoteStatus) {
					System.out.println("+ "+unknownRemoteFile.getName());
				}
			}
			else if (operationType == OperationType.RESTORE) {
				client.restore((RestoreOperationOptions) operationOptions);
			}
			else if (operationType == OperationType.DAEMON) {
				client.daemon((DaemonOperationOptions) operationOptions);
			}
			else {
				showErrorAndExit("Unknown operation '"+operationType+"'.");
			}
		}
	}	
	
	private void showUsageAndExit() {
		System.out.println("Syncany, version 0.1, copyright (c) 2011-2013 Philipp C. Heckel");
		System.out.println("Usage: sy [-c|--config=<path>] [-l|--log=<path>]");
		System.out.println("          [-v|--loglevel=OFF|SEVERE|..] [-q|--quiet]");
		System.out.println("          [-d|--debug] [-h|--help] <command> [<args>]");
		System.out.println();
		System.out.println("Global options:");
		System.out.println("  -c, --config=<path>");
		System.out.println("      Load config file from <path>. If <path> is in a '.syncany'-folder");
		System.out.println("      'localDir' is assumed to be the parent directory. If the -c option");
		System.out.println("      is not given, Syncany searches for a '.syncany'-folder in all parent");
		System.out.println("      directories.");
		System.out.println();
		System.out.println("  -l, --log=<path>");
		System.out.println("      Log output to the file given by <path>. If - is given, the");
		System.out.println("      output will be logged to STDOUT (default).");
		System.out.println();
		System.out.println("  -v, --loglevel=<level>");
		System.out.println("      Change log level to <level>. Level can be either of the");
		System.out.println("      following: OFF, SEVERE, WARNING, INFO, FINE, FINER, FINEST, ALL");
		System.out.println();
		System.out.println("  -q, --quiet");
		System.out.println("      Alias to --loglevel=OFF");
		System.out.println();
		System.out.println("  -d, --debug");
		System.out.println("      Sets the log level to ALL, and print the log to the console.");
		System.out.println();
		System.out.println("  -h, --help");
		System.out.println("      Print this help screen");
		System.out.println();
		System.out.println("Commands:");
		System.out.println("  up [<args>]");
		System.out.println("      Detect local changes and upload to repo (commit)");
		System.out.println();
		System.out.println("      Arguments:");
		System.out.println("      -f, --force             Force upload even if remote changes exist (will conflict!).");
		System.out.println("      -c, --no-cleanup        Do not merge own databases in repo.");
		System.out.println();
		System.out.println("  down");
		System.out.println("      Detect remote changes and apply locally (update)");
		System.out.println();
		System.out.println("  sync [<args>]");
		System.out.println("      Perform down- and up-command.");		
		System.out.println();
		System.out.println("  daemon [<args>]");
		System.out.println("      Performs the sync-command in a loop. In furture releases, this command will");
		System.out.println("      watch the file system.");
		System.out.println();
		System.out.println("      Arguments:");
		System.out.println("      -i, --interval=<sec>    Repeat sync every <sec> seconds (default is 30).");
		System.out.println();
		System.out.println("  status [<args>]");
		System.out.println("      Detect local changes and print to STDOUT.");
		System.out.println();
		System.out.println("      Arguments:");
		System.out.println("      -f, --force-checksum    Force checksum comparison, if not enabled mod. date/size is used.");
		System.out.println();
		System.out.println("  ls-remote");
		System.out.println("      Detect remote changes and print to STDOUT.");
		System.out.println();
		
		System.exit(1);
	}

	private void showErrorAndExit(String errorMessage) {
		System.out.println("Syncany: "+errorMessage);
		System.out.println("         Refer to help page using '--help'.");
		System.out.println();

		System.exit(1);		
		
	}
	
}
