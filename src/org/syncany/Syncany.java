package org.syncany;

import java.io.File;
import java.io.IOException;

import org.syncany.config.ConfigTO;
import org.syncany.config.Profile;

import com.google.gson.JsonSyntaxException;

public class Syncany {
	public enum Operation { SYNC_UP, SYNC_DOWN };
	
	private String[] args;
	private Operation operation;
	private File configFile;
	private Profile profile;	
	private Application application;
		
	public static void main(String[] args) throws Exception {
		new Syncany(args).start();
	}
	
	public Syncany(String[] args) {
		this.args = args;
	}
	
	public void start() throws Exception {
		readCommandLineArguments(args);
		
		initProfileFromConfigFile(configFile);
		initApplication(profile);
		
		runOperation(application, operation);
	}
	
	private void readCommandLineArguments(String[] args) throws Exception {
		if (args.length == 3) { // -f config.json up
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
		readCommandLineArgumentOperation(args[1]);		
	}

	private void readCommandLineArgumentsWithSpecificConfigFile(String[] args) throws Exception {
		// -f config.json
		if (!"-f".equals(args[0])) {
			throw new Exception("Invalid command line syntax.");
		}
		
		readCommandLineArgumentConfigFile(args[1]);
		readCommandLineArgumentOperation(args[2]);		
	}
	
	private void readCommandLineArgumentConfigFile(String configFileArgument) throws Exception {
		configFile = new File(configFileArgument);
		
		if (!configFile.exists()) {
			throw new Exception("Given config file does not exist.");
		}		
	}
	
	private void readCommandLineArgumentOperation(String operationArgument) throws Exception {
		if ("up".equals(operationArgument)) {
			operation = Operation.SYNC_UP;
		}
		else if ("down".equals(operationArgument)) {
			operation = Operation.SYNC_DOWN;
		}
		else {
			throw new Exception("Given operation is unknown.");
		}		
	}

	private void initProfileFromConfigFile(File configFile) throws JsonSyntaxException, IOException, Exception {
		ConfigTO configTO = ConfigTO.load(configFile);
		profile = new Profile(configTO);
	}
	
	private void initApplication(Profile profile) throws Exception {
		application = new Application(profile);
		application.initProfileDirectories();
	}
	
	private void runOperation(Application application, Operation operation) throws Exception {
		if (operation == Operation.SYNC_UP) {
			application.syncUp();
		}
		else if (operation == Operation.SYNC_UP) {
			application.syncDown();
		}
		else {
			throw new Exception("Unknown operation.");
		}
	}
}
