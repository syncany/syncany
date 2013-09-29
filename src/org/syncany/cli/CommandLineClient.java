package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.Client;
import org.syncany.config.Config;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.ConfigTO;
import org.syncany.config.ConfigTO.ConnectionSettings;
import org.syncany.config.ConfigTO.EncryptionSettings;
import org.syncany.config.Encryption;
import org.syncany.config.LogFormatter;
import org.syncany.config.Logging;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.operations.DaemonOperation.DaemonOperationOptions;
import org.syncany.operations.InitOperation.InitOperationOptions;
import org.syncany.operations.RestoreOperation.RestoreOperationOptions;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.SyncDownOperation.SyncDownOperationOptions;
import org.syncany.operations.SyncDownOperation.SyncDownOperationResult;
import org.syncany.operations.SyncOperation.SyncOperationOptions;
import org.syncany.operations.SyncOperation.SyncOperationResult;
import org.syncany.operations.SyncUpOperation.SyncUpOperationOptions;
import org.syncany.operations.SyncUpOperation.SyncUpOperationResult;

public class CommandLineClient extends Client {
	private static final Logger logger = Logger.getLogger(CommandLineClient.class.getSimpleName());	
	
	private String[] args;	
	private File configFile;
	
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
	
	public void start() throws Exception {
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
			runOperation(options, options.nonOptionArguments());
		}
		catch (OptionException e) {
			showErrorAndExit(e.getMessage());
		}
	}	

	private void initHelpOption(OptionSet options, OptionSpec<Void> optionHelp) {
		if (options.has(optionHelp)) {
			showUsageAndExit();
		}
	}

	private void runOperation(OptionSet options, List<?> nonOptions) throws Exception {
		if (nonOptions.size() == 0) {
			showUsageAndExit();
		}
		
		List<Object> nonOptionsCopy = new ArrayList<Object>(nonOptions);
		String operationName = (String) nonOptionsCopy.remove(0); 
		String[] operationArgs = nonOptionsCopy.toArray(new String[nonOptionsCopy.size()]);
		
		// Pre-init operations
		if ("init".equals(operationName)) {
			// Check config (NOT allowed for these operations)
			if (configFile != null) {
				showErrorAndExit("Repository found in path. Command can only be used outside a repository.");			
			}
			
			runInitOperation(operationArgs);
		}
		else {
			// Check config (required for these operations)
			if (configFile == null) {
				showErrorAndExit("No repository found in path. Use 'init' command to create one.");			
			}
						
			// Run commands
			if ("up".equals(operationName)) {
				runSyncUpOperation(operationArgs);
			}
			else if ("down".equals(operationName)) {
				runSyncDownOperation(operationArgs);
			}
			else if ("sync".equals(operationName)) {
				runSyncOperation(operationArgs);
			}
			else if ("status".equals(operationName)) {			
				runStatusOperation(operationArgs);
			}
			else if ("ls-remote".equals(operationName)) {
				runRemoteStatusOperation(operationArgs);
			}			
			else if ("restore".equals(operationName)) {
				runRestoreOperation(operationArgs);
			}
			else if ("daemon".equals(operationName)) {
				runDaemonOperation(operationArgs);			
			}
			else {
				showErrorAndExit("Given command is unknown: "+operationName);
			}
		}
	}

	private void runRestoreOperation(String[] operationArguments) throws Exception {
		RestoreOperationOptions operationOptions = new RestoreOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<String> optionDateStr = parser.acceptsAll(asList("d", "date")).withRequiredArg().required();
		OptionSpec<Void> optionForce = parser.acceptsAll(asList("f", "force"));
		
		OptionSet options = parser.parse(operationArguments);	
		
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
			
			logger.log(Level.FINE, "Restore date: "+restoreDate);
			operationOptions.setRestoreTime(restoreDate);
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
				
				logger.log(Level.FINE, "Restore date: "+restoreDate);
				operationOptions.setRestoreTime(restoreDate);
			}
			else {
				throw new Exception("Invalid '--date' argument: "+dateStr);
			}
		}
		
		// --force
		if (options.has(optionForce)) {
			operationOptions.setForce(true);
		}
		else {
			operationOptions.setForce(false);
		}
		
		// Files
		List<?> nonOptionArgs = options.nonOptionArguments();
		List<String> restoreFilePaths = new ArrayList<String>();
		
		for (Object nonOptionArg : nonOptionArgs) {
			restoreFilePaths.add(nonOptionArg.toString());
		}

		operationOptions.setRestoreFilePaths(restoreFilePaths);	
		
		// Run!
		restore(operationOptions);
	}

	private void runInitOperation(String[] operationArguments) throws OptionException, Exception {
		InitOperationOptions operationOptions = new InitOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<Void> optionInteractive = parser.acceptsAll(asList("i", "interactive"));
		
		OptionSet options = parser.parse(operationArguments);	
		List<?> nonOptionArgs = options.nonOptionArguments();
		
		// --interactive
		if (options.has(optionInteractive)) {
			runInitInteractive(operationOptions);
		}
		
		// not --interactive
		else {			
			if (nonOptionArgs.size() == 0) {
				showErrorAndExit("Argument <plugin> is required.");
			}
			
			// <plugin>
			String pluginStr = (String) nonOptionArgs.get(0);		
			operationOptions.setConnection(new ConnectionSettings(pluginStr, null));
			
			// <location>
			File localDir = null;
			
			if (nonOptionArgs.size() > 1) {
				String locationStr = (String) nonOptionArgs.get(1);
				localDir = new File(locationStr).getCanonicalFile(); 
			}
			else {
				localDir = new File(".").getCanonicalFile(); 			
			}
			
			operationOptions.setLocalDir(localDir.getAbsolutePath());
		}
						
		// Run!
		File skelConfigFile = init(operationOptions);
		
		out.println("Repository initialized, skeleton config at "+skelConfigFile);
		out.println("Please edit the config now, then use '--help' option.");
	}

	private void runInitInteractive(InitOperationOptions operationOptions) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Console console = System.console();
		
		// <location>
		File localDir = null;		
		File defaultLocalDir = new File(".").getCanonicalFile();
		
		out.println("Interactive repository initialization started.");
		out.println("Default options are in brackets.");
		out.println();
		
		while (localDir == null) {
			out.println("Choose the sync folder. Files in this folder will be synchronized.");
			out.print("Syncany folder ["+defaultLocalDir+"]: ");
			String localDirStr = in.readLine();
			
			if ("".equals(localDirStr)) {
				localDir = defaultLocalDir;
			}
			else {
				localDir = new File(localDirStr);
				
				if (!localDir.exists()) {
					out.println("ERROR: "+localDir+" does not exist.");
					out.println();
					
					localDir = null;
				}
			}
		}
		
		operationOptions.setLocalDir(localDir.getAbsolutePath());
		
		// <plugin>
		String pluginStr = null;
		
		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		String pluginsList = "";
		
		for (int i=0; i<plugins.size(); i++) {
			pluginsList += plugins.get(i).getId();
			if (i < plugins.size()-1) { pluginsList += ", "; }			
		}
		
		out.println();
		
		while (pluginStr == null) {
			out.println("Choose a storage plugin. Available plugins are: "+pluginsList);
			out.print("Plugin: ");
			pluginStr = in.readLine();
			
			if (Plugins.get(pluginStr) == null) {
				out.println("ERROR: Plugin '"+pluginStr+"' does not exist.");
				out.println();
				
				pluginStr = null;
			}
		}
					
		// Mandatory plugin fields
		Connection connection = Plugins.get(pluginStr).createConnection();

		Map<String, String>  pluginSettings = new HashMap<String, String>();
		String[] mandatorySettings = connection.getMandatorySettings();
		String[] optionalSettings = connection.getOptionalSettings();
		
		out.println();
		out.println("This plugin has the following settings.");
		
		for (String settingKey : mandatorySettings) {
			String settingValue = null;
			
			while (settingValue == null) {
				out.print("- "+settingKey+": ");
				settingValue = in.readLine();
				
				if ("".equals(settingValue)) {
					out.println("ERROR: This setting is mandatory.");
					out.println();
					
					settingValue = null;
				}
			}
			
			pluginSettings.put(settingKey, settingValue);			
		}

		for (String settingKey : optionalSettings) {
			out.print("- "+settingKey+" (optional): ");
			String settingValue = in.readLine();
			
			if (!"".equals(settingValue)) {
				pluginSettings.put(settingKey, settingValue);
			}
		}

		operationOptions.setConnection(new ConnectionSettings(pluginStr, pluginSettings));
		
		// Encryption
		out.println();
		out.println("The password is used to encrypt data on the remote storage.");
		out.println("Please choose it wisely.");
		out.println();
		
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password: ");
			char[] confirmPasswordChars = console.readPassword("Confirm: ");
			
			if (!Arrays.equals(passwordChars, confirmPasswordChars)) {
				out.println("ERROR: Passwords do not match.");
				out.println();
				
				continue;
			}
			
			password = new String(passwordChars);
			
			if (passwordChars.length < 12) {
				out.println("WARNING: The password is a bit short. Less than 12 chars are not future-proof!");
				String yesno = console.readLine("Are you sure you want to use it (y/n)? ");
				
				if (!yesno.toLowerCase().startsWith("y")) {
					out.println();
					password = null;
				}
			}
		}		
		
		out.println();
		out.println("The cipher defines the encryption parameters and key size. There are a ");
		out.println("few predefined crypto settings. To choose your own, please edit the config file.");
		out.println();
		out.println("Options:");
		out.println("  'aes128'      AES/GCM/NoPadding, 128 bit (weak, but no country restrictions)");
		out.println("  'aes256'      AES/GCM/NoPadding, 256 bit (strong, but restricted in some countries)");
		out.println("  'twofish256'  Twofish/GCM/NoPadding, 256 bit (strong, but restricted in some countries)");
		out.println();
		
		String cipherStr = null;
		int keySize = 0;
		boolean unlimitedCryptoNeeded = false;
		
		while (cipherStr == null) {
			cipherStr = console.readLine("Cipher: ");
			
			if ("aes128".equals(cipherStr)) {
				cipherStr = "AES/GCM/NoPadding";
				keySize = 128;
				unlimitedCryptoNeeded = false;
			}
			else if ("aes256".equals(cipherStr) || "twofish256".equals(cipherStr)) {
				out.println();
				out.println("This cipher/keysize might not be allowed in your country.");
				String yesno = console.readLine("Are you sure you want to use it (y/n)? ");
				
				if (yesno.toLowerCase().startsWith("y")) {
					if ("aes256".equals(cipherStr)) {
						cipherStr = "AES/GCM/NoPadding";
						keySize = 256;
					}
					else {
						cipherStr = "Twofish/GCM/NoPadding";
						keySize = 256;
					}

					// Try enabling unlimited crypto
					unlimitedCryptoNeeded = true;
				
					try {
						Encryption.enableUnlimitedCrypto();
					}
					catch (Exception e) {
						showErrorAndExit("Unable to enable unlimited crypto. Check out: http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html");
					}
				}
				else {
					out.println();
					cipherStr = null;
				}
			}
			else {
				out.println("ERROR: Please choose a valid option.");
				out.println();
				
				cipherStr = null;
			}
		}
		
		operationOptions.setEncryption(new EncryptionSettings(true, password, cipherStr, keySize, true, unlimitedCryptoNeeded));
		
	}

	private void runRemoteStatusOperation(String[] operationArguments) throws Exception {
		List<RemoteFile> remoteStatus = remoteStatus();
		
		if (remoteStatus.size() > 0) {
			for (RemoteFile unknownRemoteFile : remoteStatus) {
				out.println("? "+unknownRemoteFile.getName());
			}
		}
		else {
			out.println("No remote changes.");
		}
	}

	private void runSyncOperation(String[] operationArguments) throws Exception {
		SyncOperationOptions operationOptions = parseSyncOptions(operationArguments);		
		SyncOperationResult operationResult = sync(operationOptions);
		
		printSyncResult(operationResult);
	}

	private void printSyncResult(SyncOperationResult operationResult) {
		printSyncDownResult(operationResult.getSyncDownResult());		
		printSyncUpResult(operationResult.getSyncUpResult());
	}

	private SyncOperationOptions parseSyncOptions(String[] operationArguments) throws Exception {
		SyncDownOperationOptions syncDownOptions = parseSyncDownOptions(operationArguments);
		SyncUpOperationOptions syncUpOptions = parseSyncUpOptions(operationArguments);

		SyncOperationOptions syncOptions = new SyncOperationOptions();
		syncOptions.setSyncDownOptions(syncDownOptions);
		syncOptions.setSyncUpOptions(syncUpOptions);
		
		return syncOptions;
	}

	private SyncDownOperationOptions parseSyncDownOptions(String[] operationArguments) {
		return new SyncDownOperationOptions();
	}

	private void runSyncDownOperation(String[] operationArguments) throws Exception {
		SyncDownOperationOptions operationOptions = parseSyncDownOptions(operationArguments);		
		SyncDownOperationResult operationResult = down(operationOptions);		
		
		printSyncDownResult(operationResult);
	}

	private void printSyncDownResult(SyncDownOperationResult operationResult) {
		ChangeSet changeSet = operationResult.getChangeSet();
		
		if (changeSet.hasChanges()) {
			for (String newFile : changeSet.getNewFiles()) {
				out.println("A "+newFile);
			}
	
			for (String changedFile : changeSet.getChangedFiles()) {
				out.println("M "+changedFile);
			}
			
			for (String deletedFile : changeSet.getDeletedFiles()) {
				out.println("D "+deletedFile);
			}	
			
			out.println("Sync down finished.");
		}
		else {
			out.println("Sync down skipped, no remote changes.");
		}
	}

	private StatusOperationOptions parseStatusOptions(String[] operationArgs) {
		StatusOperationOptions operationOptions = new StatusOperationOptions();

		OptionParser parser = new OptionParser();	
		parser.allowsUnrecognizedOptions();
		
		OptionSpec<Void> optionForceChecksum = parser.acceptsAll(asList("f", "force-checksum"));
		
		OptionSet options = parser.parse(operationArgs);	
		
		// --force-checksum
		operationOptions.setForceChecksum(options.has(optionForceChecksum));
		
		return operationOptions;
	}
	
	private void runStatusOperation(String[] operationArgs) throws Exception {
		StatusOperationOptions operationOptions = parseStatusOptions(operationArgs);
		ChangeSet changeSet = status(operationOptions);
		
		printStatusResult(changeSet);
	}

	private void printStatusResult(ChangeSet changeSet) {
		if (changeSet.hasChanges()) {
			for (String newFile : changeSet.getNewFiles()) {
				out.println("? "+newFile);
			}

			for (String changedFile : changeSet.getChangedFiles()) {
				out.println("M "+changedFile);
			}
			
			for (String deletedFile : changeSet.getDeletedFiles()) {
				out.println("D "+deletedFile);
			}						
		}
		else {
			out.println("No local changes.");
		}
	}

	private void runDaemonOperation(String[] operationArgs) throws Exception {
		DaemonOperationOptions operationOptions = new DaemonOperationOptions();

		OptionParser parser = new OptionParser();	
		OptionSpec<Integer> optionInterval = parser.acceptsAll(asList("i", "interval")).withRequiredArg().ofType(Integer.class);
		
		OptionSet options = parser.parse(operationArgs);	
		
		// --interval
		if (options.has(optionInterval)) {
			operationOptions.setInterval(options.valueOf(optionInterval)*1000);
		}
		
		// Run!
		daemon(operationOptions);
	}

	private SyncUpOperationOptions parseSyncUpOptions(String[] operationArgs) throws Exception {
		// Sync up options
		SyncUpOperationOptions operationOptions = new SyncUpOperationOptions();

		OptionParser parser = new OptionParser();	
		parser.allowsUnrecognizedOptions();
		
		OptionSpec<Void> optionNoCleanup = parser.acceptsAll(asList("c", "no-cleanup"));
		OptionSpec<Void> optionForceUpload = parser.acceptsAll(asList("F", "force-upload"));
		
		OptionSet options = parser.parse(operationArgs);
		
		// status [<args>]
		operationOptions.setStatusOptions(parseStatusOptions(operationArgs));
		
		// --no-cleanup
		operationOptions.setCleanupEnabled(!options.has(optionNoCleanup));
		
		// --force
		operationOptions.setForceUploadEnabled(options.has(optionForceUpload));
		
		return operationOptions;
	}
	
	private void runSyncUpOperation(String[] operationArgs) throws Exception {
		// Run!
		SyncUpOperationOptions operationOptions = parseSyncUpOptions(operationArgs);
		SyncUpOperationResult operationResult = up(operationOptions);	
		
		printSyncUpResult(operationResult);
	}

	private void printSyncUpResult(SyncUpOperationResult operationResult) {
		ChangeSet changeSet = operationResult.getUploadChangeSet();
		
		if (changeSet.hasChanges()) {			
			for (String newFile : changeSet.getNewFiles()) {
				out.println("A "+newFile);
			}
	
			for (String changedFile : changeSet.getChangedFiles()) {
				out.println("M "+changedFile);
			}
			
			for (String deletedFile : changeSet.getDeletedFiles()) {
				out.println("D "+deletedFile);
			}	
			
			out.println("Sync up finished.");
		}
		else {
			out.println("Sync up skipped, no local changes.");
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

			ConfigTO configTO = ConfigTO.load(configFile);
			config = new Config(configTO);
			
			// Enable unlimited crypto if necessary
			boolean isUnlimitedCryptoNeeded = configTO.getEncryption() != null 
					&& configTO.getEncryption().isUnlimitedCryptoNeeded() != null && configTO.getEncryption().isUnlimitedCryptoNeeded();
			
			if (isUnlimitedCryptoNeeded) {
				try {
					Encryption.enableUnlimitedCrypto();
				}
				catch (Exception e) {
					showErrorAndExit("Unable to enable unlimited crypto. Check out: http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html");
				}
			}
			
			// Create folders
			logger.log(Level.INFO, "Creating directories ...");				
			createDirectories();
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
		out.println("      In addition to these arguments, all arguments of the status command can be used.");
		out.println();
		out.println("  down");
		out.println("      Detect remote changes and apply locally (update)");
		out.println();
		out.println("  sync [<args>]");
		out.println("      Synchronizes the local folder with the remote repository by calling the down command");	
		out.println("      and the up command. All arguments from the up/down/status/ls-remote commands can be used.");
		out.println();
		out.println("  daemon [<args>]");
		out.println("      Performs the sync-command in a loop. In furture releases, this command will");
		out.println("      watch the file system.");
		out.println();
		out.println("      Arguments:");
		out.println("      -i, --interval=<sec>    Repeat sync every <sec> seconds (default is 30).");
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
		
		System.exit(1);
	}

	private void showErrorAndExit(String errorMessage) {
		out.println("Syncany: "+errorMessage);
		out.println("         Refer to help page using '--help'.");
		out.println();

		System.exit(1);		
		
	}
	
}
