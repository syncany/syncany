package org.syncany;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.Config;
import org.syncany.config.ConfigTO;
import org.syncany.config.Logging;
import org.syncany.operations.Operation.OperationOptions;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.SyncUpOperation.SyncUpOperationOptions;
import org.syncany.util.FileUtil;

public class Syncany {
	private static final Logger logger = Logger.getLogger(Syncany.class.getSimpleName());	
	public enum CommandArgument { SYNC_UP, SYNC_DOWN, STATUS };
	
	private String[] args;
	private Client client;

	private CommandArgument operationArgument;
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
		initClient(configFile);
		
		runOperation();
	}	

	private void readCommandLineArguments(String[] args) throws Exception {
		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();
		
		OptionSpec<File> optionConfig = parser.acceptsAll(asList("c", "config")).withRequiredArg().ofType(File.class);
		OptionSpec<File> optionLog = parser.acceptsAll(asList("l", "log")).withRequiredArg().ofType(File.class);
		OptionSpec<String> optionLogLevel = parser.acceptsAll(asList("v", "loglevel")).withOptionalArg();
		OptionSpec<Void> optionDebug = parser.acceptsAll(asList("D", "debug"));		
		
		OptionSet options = parser.parse(args);
		
		initConfigOption(options, optionConfig);
		initLogFileOption(options, optionLog);
		initLogLevelOption(options, optionLogLevel, optionDebug);

		initOperation(options, options.nonOptionArguments());
	}	

	private void initOperation(OptionSet options, List<?> nonOptions) {
		if (nonOptions.size() == 0) {
			showUsageAndExit();
		}
		
		List<Object> nonOptionsCopy = new ArrayList<Object>(nonOptions);
		String operationArgument = (String) nonOptionsCopy.remove(0); 
		String[] operationArgs = nonOptions.toArray(new String[nonOptions.size()]);
		
		if ("up".equals(operationArgument)) {
			initSyncUpOperation(operationArgs);
		}
		else if ("down".equals(operationArgument)) {
			this.operationArgument = CommandArgument.SYNC_DOWN;
		}
		else if ("status".equals(operationArgument)) {
			this.operationArgument = CommandArgument.STATUS;
		}
		else {
			showUsageAndExit("Given command is unknown.");
		}		
	}

	private void initSyncUpOperation(String[] operationArgs) {
		SyncUpOperationOptions aOperationOptions = new SyncUpOperationOptions();

		OptionParser parser = new OptionParser();		
		OptionSpec<Void> optionNoCleanup = parser.acceptsAll(asList("c", "no-cleanup"));
		
		OptionSet options = parser.parse(operationArgs);
		
		if (options.has(optionNoCleanup)) {
			aOperationOptions.setCleanupEnabled(false);
		}
		
		operationArgument = CommandArgument.SYNC_UP;
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

	private void initLogLevelOption(OptionSet options, OptionSpec<String> optionLogLevel, OptionSpec<Void> optionDebug) {
		Level currentLogLevel = Logger.getLogger("").getLevel();
		Level newLogLevel = null;
		
		if (options.has(optionDebug)) {
			newLogLevel = Level.ALL;
		}
		else if (options.has(optionLogLevel)) {
			String newLogLevelStr = options.valueOf(optionLogLevel);

			try {
				newLogLevel = Level.parse(newLogLevelStr);
			}
			catch (IllegalArgumentException e) {
				showUsageAndExit("Invalid log level given "+newLogLevelStr+"'");
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

	private void initConfigOption(OptionSet options, OptionSpec<File> optionConfig) {
		if (options.has(optionConfig)) {
			configFile = options.valueOf(optionConfig);
		}
		else {
			configFile = new File("config.json");
		}
		
		if (!configFile.exists()) {
			showUsageAndExit("Cannot find config file '"+configFile+"'");
		}
	}		
	
	private void showUsageAndExit() {
		showUsageAndExit(null);
	}

	private void showUsageAndExit(String errorMessage) {
		if (errorMessage != null) {
			System.out.println("ERROR: "+errorMessage);
			System.out.println();
		}
		
		System.out.println("Usage: syncany [-c config.json] up     -  Sync up");
		System.out.println("       syncany [-c config.json] down   -  Sync down");
		System.out.println("       syncany [-c config.json] status -  List local changes");
		
		System.exit(1);
	}

	private void initClient(File configFile) throws Exception {
		logger.log(Level.INFO, "Loading config from {0} ...", configFile);
		
		ConfigTO configTO = ConfigTO.load(configFile);
		
		client = new Client();
		client.setConfig(new Config(configTO));
		client.createDirectories();
	}
	
	private void runOperation() throws Exception {
		if (operationArgument == CommandArgument.SYNC_UP) {
			client.up((SyncUpOperationOptions) operationOptions); // TODO [medium] ugly
		}
		else if (operationArgument == CommandArgument.SYNC_DOWN) {
			client.down();
		}
		else if (operationArgument == CommandArgument.STATUS) {
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
		else {
			showUsageAndExit("Unknown operation.");
		}
	}			
}
