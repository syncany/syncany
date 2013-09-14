package org.syncany;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.config.ConfigTO;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.util.FileUtil;

public class Syncany {
	private static final Logger logger = Logger.getLogger(Syncany.class.getSimpleName());	
	public enum CommandArgument { SYNC_UP, SYNC_DOWN, STATUS };
	
	private String[] args;
	private Client client;
	private CommandArgument operationArgument;
	private File configFile;
		
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
		if (args.length == 3) { // -c config.json up/down/status
			readCommandLineArgumentsWithSpecificConfigFile(args);
		}
		else if (args.length == 1) { // up
			readCommandLineArgumentsWithoutConfigFile(args);			
		}
		else {
			showUsageAndExit("Invalid syntax.");
		}
	}
	
	private void readCommandLineArgumentsWithoutConfigFile(String[] args2) throws Exception {
		String defaultConfigFileArgument = "config.json";
		
		readCommandLineArgumentConfigFile(defaultConfigFileArgument);
		readCommandLineArgumentCommand(args[0]);		
	}

	private void readCommandLineArgumentsWithSpecificConfigFile(String[] args) throws Exception {
		// -c config.json
		if (!"-c".equals(args[0])) {
			showUsageAndExit();
		}
		
		readCommandLineArgumentConfigFile(args[1]);
		readCommandLineArgumentCommand(args[2]);		
	}
	
	private void readCommandLineArgumentConfigFile(String configFileArgument) throws Exception {
		configFile = new File(configFileArgument);
		
		if (!configFile.exists()) {
			showUsageAndExit("Given config file does not exist.");
		}		
	}
	
	private void readCommandLineArgumentCommand(String commandArgument) throws Exception {
		if ("up".equals(commandArgument)) {
			this.operationArgument = CommandArgument.SYNC_UP;
		}
		else if ("down".equals(commandArgument)) {
			this.operationArgument = CommandArgument.SYNC_DOWN;
		}
		else if ("status".equals(commandArgument)) {
			this.operationArgument = CommandArgument.STATUS;
		}
		else {
			showUsageAndExit("Given command is unknown.");
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
		System.out.println("       syncany [-c config.json] status -  Get status");
		
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
			client.up();
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
