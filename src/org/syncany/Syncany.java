package org.syncany;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.commands.Command;
import org.syncany.commands.SyncDownCommand;
import org.syncany.commands.SyncUpCommand;
import org.syncany.config.ConfigTO;
import org.syncany.config.Profile;

import com.google.gson.JsonSyntaxException;

public class Syncany {
	private static final Logger logger = Logger.getLogger(Syncany.class.getSimpleName());	
	public enum CommandArgument { SYNC_UP, SYNC_DOWN };
	
	private String[] args;
	private CommandArgument commandArgument;
	private Command command;
	private File configFile;
	private Profile profile;	
		
	public static void main(String[] args) throws Exception {
		new Syncany(args).start();
	}
	
	public Syncany(String[] args) {
		this.args = args;
	}
	
	public void start() throws Exception {
		readCommandLineArguments(args);
		
		initProfileFromConfigFile(configFile);
		initCommand(profile);
		
		runCommand();
	}	

	private void readCommandLineArguments(String[] args) throws Exception {
		if (args.length == 3) { // -c config.json up
			readCommandLineArgumentsWithSpecificConfigFile(args);
		}
		else if (args.length == 1) { // up
			readCommandLineArgumentsWithoutConfigFile(args);			
		}
		else {
			throw new Exception("Invalid command line argument syntax.");
		}
	}
	
	private void readCommandLineArgumentsWithoutConfigFile(String[] args2) throws Exception {
		String defaultConfigFileArgument = "config.json";
		
		readCommandLineArgumentConfigFile(defaultConfigFileArgument);
		readCommandLineArgumentCommand(args[1]);		
	}

	private void readCommandLineArgumentsWithSpecificConfigFile(String[] args) throws Exception {
		// -c config.json
		if (!"-c".equals(args[0])) {
			throw new Exception("Invalid command line syntax.");
		}
		
		readCommandLineArgumentConfigFile(args[1]);
		readCommandLineArgumentCommand(args[2]);		
	}
	
	private void readCommandLineArgumentConfigFile(String configFileArgument) throws Exception {
		configFile = new File(configFileArgument);
		
		if (!configFile.exists()) {
			throw new Exception("Given config file does not exist.");
		}		
	}
	
	private void readCommandLineArgumentCommand(String commandArgument) throws Exception {
		if ("up".equals(commandArgument)) {
			this.commandArgument = CommandArgument.SYNC_UP;
		}
		else if ("down".equals(commandArgument)) {
			this.commandArgument = CommandArgument.SYNC_DOWN;
		}
		else {
			throw new Exception("Given operation is unknown.");
		}		
	}

	private void initProfileFromConfigFile(File configFile) throws JsonSyntaxException, IOException, Exception {
		ConfigTO configTO = ConfigTO.load(configFile);
		profile = new Profile(configTO);
		
		createProfileDirectories();
	}
	
	private void createProfileDirectories() throws Exception {   
		logger.log(Level.INFO, "Create profile directories ...");
		
    	profile.getAppDir().mkdirs();
    	profile.getAppCacheDir().mkdirs();
    	profile.getAppDatabaseDir().mkdirs();
	}	

	private void initCommand(Profile profile) throws Exception {
		if (commandArgument == CommandArgument.SYNC_UP) {
			command = new SyncUpCommand(profile);
		}
		else if (commandArgument == CommandArgument.SYNC_DOWN) {
			command = new SyncDownCommand(profile);
		}
		else {
			throw new Exception("Unknown operation.");
		}
	}	
		
	private void runCommand() throws Exception {
		command.execute();		
	}
}
