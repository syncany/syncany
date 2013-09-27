package org.syncany;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.syncany.operations.SyncUpOperation.SyncUpOperationOptions;
import org.syncany.util.FileUtil;

public class Syncany extends Client {
	private static final Logger logger = Logger.getLogger(Syncany.class.getSimpleName());	
	
	private String[] args;	
	private File configFile;
		
	static {
		Logging.init();
	}
	
	public static void main(String[] args) throws Exception {
		new Syncany(args).start();
	}	
	
	public Syncany(String[] args) {
		this.args = args;
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
			
			logger.log(Level.INFO, "Creating directories from {0} ...", configFile);				
			createDirectories();
			
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
		
		System.out.println("Repository initialized, skeleton config at "+skelConfigFile);
		System.out.println("Please edit the config now, then use '--help' option.");
	}

	private void runInitInteractive(InitOperationOptions operationOptions) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Console console = System.console();
		
		// <location>
		File localDir = null;		
		File defaultLocalDir = new File(".").getCanonicalFile();
		
		System.out.println("Interactive repository initialization started.");
		System.out.println("Default options are in brackets.");
		System.out.println();
		
		while (localDir == null) {
			System.out.println("Choose the sync folder. Files in this folder will be synchronized.");
			System.out.print("Syncany folder ["+defaultLocalDir+"]: ");
			String localDirStr = in.readLine();
			
			if ("".equals(localDirStr)) {
				localDir = defaultLocalDir;
			}
			else {
				localDir = new File(localDirStr);
				
				if (!localDir.exists()) {
					System.out.println("ERROR: "+localDir+" does not exist.");
					System.out.println();
					
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
		
		System.out.println();
		
		while (pluginStr == null) {
			System.out.println("Choose a storage plugin. Available plugins are: "+pluginsList);
			System.out.print("Plugin: ");
			pluginStr = in.readLine();
			
			if (Plugins.get(pluginStr) == null) {
				System.out.println("ERROR: Plugin '"+pluginStr+"' does not exist.");
				System.out.println();
				
				pluginStr = null;
			}
		}
					
		// Mandatory plugin fields
		Connection connection = Plugins.get(pluginStr).createConnection();

		Map<String, String>  pluginSettings = new HashMap<String, String>();
		String[] mandatorySettings = connection.getMandatorySettings();
		String[] optionalSettings = connection.getOptionalSettings();
		
		System.out.println();
		System.out.println("This plugin has the following settings.");
		
		for (String settingKey : mandatorySettings) {
			String settingValue = null;
			
			while (settingValue == null) {
				System.out.print("- "+settingKey+": ");
				settingValue = in.readLine();
				
				if ("".equals(settingValue)) {
					System.out.println("ERROR: This setting is mandatory.");
					System.out.println();
					
					settingValue = null;
				}
			}
			
			pluginSettings.put(settingKey, settingValue);			
		}

		for (String settingKey : optionalSettings) {
			System.out.print("- "+settingKey+" (optional): ");
			String settingValue = in.readLine();
			
			if (!"".equals(settingValue)) {
				pluginSettings.put(settingKey, settingValue);
			}
		}

		operationOptions.setConnection(new ConnectionSettings(pluginStr, pluginSettings));
		
		// Encryption
		System.out.println();
		System.out.println("The password is used to encrypt data on the remote storage.");
		System.out.println("Please choose it wisely.");
		System.out.println();
		
		String password = null;
		
		while (password == null) {
			char[] passwordChars = console.readPassword("Password: ");
			char[] confirmPasswordChars = console.readPassword("Confirm: ");
			
			if (!Arrays.equals(passwordChars, confirmPasswordChars)) {
				System.out.println("ERROR: Passwords do not match.");
				System.out.println();
				
				continue;
			}
			
			password = new String(passwordChars);
			
			if (passwordChars.length < 12) {
				System.out.println("WARNING: The password is a bit short. Less than 12 chars are not future-proof!");
				String yesno = console.readLine("Are you sure you want to use it (y/n)? ");
				
				if (!yesno.toLowerCase().startsWith("y")) {
					System.out.println();
					password = null;
				}
			}
		}		
		
		System.out.println();
		System.out.println("The cipher defines the encryption parameters and key size. There are a ");
		System.out.println("few predefined crypto settings. To choose your own, please edit the config file.");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  'aes128'      AES/GCM/NoPadding, 128 bit (weak, but no country restrictions)");
		System.out.println("  'aes256'      AES/GCM/NoPadding, 256 bit (strong, but restricted in some countries)");
		System.out.println("  'twofish256'  Twofish/GCM/NoPadding, 256 bit (strong, but restricted in some countries)");
		System.out.println();
		
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
				System.out.println();
				System.out.println("This cipher/keysize might not be allowed in your country.");
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
					System.out.println();
					cipherStr = null;
				}
			}
			else {
				System.out.println("ERROR: Please choose a valid option.");
				System.out.println();
				
				cipherStr = null;
			}
		}
		
		operationOptions.setEncryption(new EncryptionSettings(true, password, cipherStr, keySize, true, unlimitedCryptoNeeded));
		
	}

	private void runRemoteStatusOperation(String[] operationArguments) throws Exception {
		List<RemoteFile> remoteStatus = remoteStatus();
		
		for (RemoteFile unknownRemoteFile : remoteStatus) {
			System.out.println("? "+unknownRemoteFile.getName());
		}
	}

	private void runSyncOperation(String[] operationArguments) throws Exception {
		// TODO [high] Process up-options and status options

		sync();
		
		// TODO [high] Print outcome of uploaded/downloaded files
	}

	private void runSyncDownOperation(String[] operationArguments) throws Exception {
		down();		
		
		// TODO [high] Print outcome of downloaded files		
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
		// Run!
		StatusOperationOptions operationOptions = parseStatusOptions(operationArgs);
		ChangeSet changeSet = status(operationOptions);
				
		// Output
		for (File newFile : changeSet.getNewFiles()) {
			System.out.println("? "+FileUtil.getRelativePath(config.getLocalDir(), newFile));
		}

		for (File changedFile : changeSet.getChangedFiles()) {
			System.out.println("M "+FileUtil.getRelativePath(config.getLocalDir(), changedFile));
		}
		
		for (File deletedFile : changeSet.getDeletedFiles()) {
			System.out.println("D "+FileUtil.getRelativePath(config.getLocalDir(), deletedFile));
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
		up(operationOptions);	
		
		// TODO [high] Print outcome of uploaded files, or print error message if sync up fails (e.g. if --force-checksum is not enabled)
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
			System.out.println("debug");
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
		System.out.println("  init [<args>] <plugin> [<folder>]");
		System.out.println("      Initialize <folder> as a Syncany folder (default is current folder). This");
		System.out.println("      command creates and initializes a skeleton config file for the plugin <plugin>.");
		System.out.println();
		System.out.println("      The <plugin> attribute can be any of the loaded plugins.");		
		System.out.print  ("      Currently loaded are: ");
		
		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		
		for (int i=0; i<plugins.size(); i++) {
			System.out.print(plugins.get(i).getId());
			if (i < plugins.size()-1) { System.out.print(", "); }			
		}
		
		System.out.println();
		System.out.println();
		System.out.println("      Arguments:");
		System.out.println("      -i, --interactive       Run interactive mode to init Syncany folder.");
		System.out.println();
		System.out.println("  up [<args>]");
		System.out.println("      Detect local changes and upload to repo (commit)");
		System.out.println();
		System.out.println("      Arguments:");
		System.out.println("      -F, --force-upload      Force upload even if remote changes exist (will conflict!).");
		System.out.println("      -c, --no-cleanup        Do not merge own databases in repo.");
		System.out.println("      -f, --force-checksum    Force checksum comparison, if not enabled mod. date/size is used.");
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
