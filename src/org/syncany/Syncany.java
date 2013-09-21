package org.syncany;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.Config;
import org.syncany.config.ConfigTO;
import org.syncany.config.Logging;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.operations.InitOperation;
import org.syncany.operations.Operation.OperationOptions;
import org.syncany.operations.RestoreOperation.RestoreOperationOptions;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.SyncUpOperation.SyncUpOperationOptions;
import org.syncany.util.FileUtil;

public class Syncany {
	private static final Logger logger = Logger.getLogger(Syncany.class.getSimpleName());	
	public enum OperationType { SYNC_UP, SYNC_DOWN, SYNC, STATUS, REMOTE_STATUS, INIT, RESTORE };
	
	private String[] args;
	private Client client;

	private OperationType operationType;
	private OperationOptions operationOptions;
	
	private File configFile;
		
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
		OptionSpec<File> optionLog = parser.acceptsAll(asList("l", "log")).withRequiredArg().ofType(File.class);
		OptionSpec<String> optionLogLevel = parser.acceptsAll(asList("v", "loglevel")).withOptionalArg();
		OptionSpec<Void> optionDebug = parser.acceptsAll(asList("D", "debug"));
		OptionSpec<Void> optionQuiet = parser.acceptsAll(asList("q", "quiet"));
		
		// Parse global options and operation name
		OptionSet options = parser.parse(args);
		
		// Evaluate options
		initHelpOption(options, optionHelp);
		initConfigOption(options, optionConfig);
		initLogFileOption(options, optionLog);
		initLogLevelOption(options, optionLogLevel, optionQuiet, optionDebug);

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
			this.operationType = OperationType.STATUS;
		}
		else if ("ls-remote".equals(aOperationType)) {
			this.operationType = OperationType.REMOTE_STATUS;
		}
		else if ("init".equals(aOperationType)) {
			this.operationType = OperationType.INIT;
		}
		else if ("restore".equals(aOperationType)) {
			initRestoreOperation(aOperationArguments);			
		}
		else {
			showErrorAndExit("Given command is unknown.");
		}		
	}

	private void initRestoreOperation(String[] operationArgs) throws Exception {
		RestoreOperationOptions aOperationOptions = new RestoreOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<String> optionDateStr = parser.acceptsAll(asList("d", "date")).withRequiredArg().required();
		OptionSpec<Void> optionForce = parser.acceptsAll(asList("f", "force"));
		
		OptionSet options = parser.parse(operationArgs);	
		
		// --date
		String dateStr = options.valueOf(optionDateStr);
		
		Pattern relativeDatePattern = Pattern.compile("^(\\d+)([smhDWMY])$");
		Pattern absoluteDatePattern = Pattern.compile("^(\\d{2})-(\\d{2})-(\\d{4})$");
		
		Matcher relativeDateMatcher = relativeDatePattern.matcher(dateStr);		
		
		if (relativeDateMatcher.matches()) {
			int time = Integer.parseInt(relativeDateMatcher.group(1));
			String unitStr = relativeDateMatcher.group(2);
			int unitMultiplier = 0;
			
			if ("s".equals(unitStr)) { unitMultiplier = 1; }
			else if ("m".equals(unitStr)) { unitMultiplier = 60; }
			else if ("h".equals(unitStr)) { unitMultiplier = 60*60; }
			else if ("D".equals(unitStr)) { unitMultiplier = 24*60*60; }
			else if ("W".equals(unitStr)) { unitMultiplier = 7*24*60*60; }
			else if ("M".equals(unitStr)) { unitMultiplier = 30*24*60*60; }
			else if ("Y".equals(unitStr)) { unitMultiplier = 365*24*60*60; }
			
			long restoreDateMillies = time*unitMultiplier;
			Date restoreDate = new Date(restoreDateMillies);
			
			aOperationOptions.setRestoreTime(restoreDate);
		}
		else {
			Matcher absoluteDateMatcher = absoluteDatePattern.matcher(dateStr);
			
			if (absoluteDateMatcher.matches()) {
				int date = Integer.parseInt(absoluteDateMatcher.group(1));
				int month = Integer.parseInt(absoluteDateMatcher.group(2));
				int year = Integer.parseInt(absoluteDateMatcher.group(3));
				
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.set(year, month-1, date);
				
				Date restoreDate = calendar.getTime();
				aOperationOptions.setRestoreTime(restoreDate);
			}
			else {
				throw new Exception("Invalid '--date' argument: "+dateStr);
			}
		}
		
		// --force
		if (options.has(optionForce)) {
			aOperationOptions.setForce(true);
		}
		else {
			aOperationOptions.setForce(false);
		}
		
		// Files
		List<?> nonOptionArgs = options.nonOptionArguments();
		List<String> restoreFilePaths = new ArrayList<String>();
		
		for (Object nonOptionArg : nonOptionArgs) {
			restoreFilePaths.add(nonOptionArg.toString());
		}

		aOperationOptions.setRestoreFilePaths(restoreFilePaths);
		
		// Arguments for call
		operationType = OperationType.RESTORE;
		operationOptions = aOperationOptions;
	}

	private void initSyncUpOperation(String[] operationArgs) {
		SyncUpOperationOptions aOperationOptions = new SyncUpOperationOptions();

		OptionParser parser = new OptionParser();		
		OptionSpec<Void> optionNoCleanup = parser.acceptsAll(asList("c", "no-cleanup"));
		
		OptionSet options = parser.parse(operationArgs);
		
		if (options.has(optionNoCleanup)) {
			aOperationOptions.setCleanupEnabled(false);
		}
		
		operationType = OperationType.SYNC_UP;
		operationOptions = aOperationOptions;
	}

	private void initLogFileOption(OptionSet options, OptionSpec<File> optionLog) throws SecurityException, IOException {
		if (options.has(optionLog)) {
			File logFile = options.valueOf(optionLog);
			FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath());
			
			// TODO [low] Logging: Is this the right way to set logging properties

			// Add handler to existing loggers
			for (Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames(); loggerNames.hasMoreElements(); ) {
		        String loggerName = loggerNames.nextElement();
		        Logger theLogger = LogManager.getLogManager().getLogger(loggerName);
		        
		        if (theLogger != null) {
		            theLogger.addHandler(fileHandler);
		        }
		    }		

			// And to future loggers
			Logger.getLogger("").addHandler(fileHandler);
			//Logger.getGlobal().addHandler(fileHandler);
		}
	}

	private void initLogLevelOption(OptionSet options, OptionSpec<String> optionLogLevel, OptionSpec<Void> optionQuiet, OptionSpec<Void> optionDebug) {
		Level currentLogLevel = Logger.getLogger("").getLevel();
		Level newLogLevel = null;
		
		if (options.has(optionDebug)) {
			newLogLevel = Level.ALL;
		}
		else if (options.has(optionQuiet)) {
			newLogLevel = Level.OFF;
		}
		else if (options.has(optionLogLevel)) {
			String newLogLevelStr = options.valueOf(optionLogLevel);

			try {
				newLogLevel = Level.parse(newLogLevelStr);
			}
			catch (IllegalArgumentException e) {
				showErrorAndExit("Invalid log level given "+newLogLevelStr+"'");
			}
		}		
		
		if (newLogLevel != currentLogLevel) {
			// TODO [low] Logging: Is this the right way to set logging properties

			// Add handler to existing loggers
			for (Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames(); loggerNames.hasMoreElements(); ) {
		        String loggerName = loggerNames.nextElement();
		        Logger theLogger = LogManager.getLogManager().getLogger(loggerName);
		        
		        if (theLogger != null) {
		            theLogger.setLevel(newLogLevel);
		        }
		    }		

			// And to future loggers
			Logger.getLogger("").setLevel(newLogLevel);
		}
	}

	private void initConfigOption(OptionSet options, OptionSpec<File> optionConfig) throws IOException {
		if (options.has(optionConfig)) {
			configFile = options.valueOf(optionConfig);
		}
		else {
			configFile = findConfigFileInPath();
		}		
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
		
		ConfigTO configTO = ConfigTO.load(configFile);
		
		client = new Client();
		client.setConfig(new Config(configTO));
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
				ChangeSet changeSet = client.status();
				
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
		System.out.println("      Alias to --loglevel=ALL");
		System.out.println();
		System.out.println("  -h, --help");
		System.out.println("      Print this help screen");
		System.out.println();
		System.out.println("Commands:");
		System.out.println("  up");
		System.out.println("      Detect local changes and upload to repo (commit)");
		System.out.println();
		System.out.println("      Arguments:");
		System.out.println("      --no-cleanup    Do not merge own databases in repo.");
		System.out.println();
		System.out.println("  down");
		System.out.println("      Detect remote changes and apply locally (update)");
		System.out.println();
		System.out.println("  sync");
		System.out.println("      Perform down- and up-command.");		
		System.out.println();
		System.out.println("  status");
		System.out.println("      Detect local changes and print to STDOUT.");
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
