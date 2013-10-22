package org.syncany.cli;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.syncany.config.ConfigTO.ConnectionSettings;
import org.syncany.config.ConfigTO.EncryptionSettings;
import org.syncany.config.Encryption;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.operations.InitOperation.InitOperationOptions;

public class InitCommand extends Command {
	@Override
	public boolean needConfigFile() {	
		return false;
	}
	
	@Override
	public int execute(String[] operationArgs) throws Exception {
		runInitOperation(operationArgs);
		return 0;
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
				throw new Exception("Argument <plugin> is required.");
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
		File skelConfigFile = client.init(operationOptions);
		
		out.println("Repository initialized, skeleton config at "+skelConfigFile);
		out.println("Please edit the config now, then use '--help' option.");
	}

	private void runInitInteractive(InitOperationOptions operationOptions) throws Exception {
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
		out.println("  [1] AES/GCM/NoPadding, 128 bit (weak, but no country restrictions)");
		out.println("  [2] AES/GCM/NoPadding, 256 bit (strong, but restricted in some countries)");
		out.println("  [3] Twofish/GCM/NoPadding, 256 bit (strong, but restricted in some countries)");
		out.println();
		
		String cipherStr = null;
		int keySize = 0;
		boolean unlimitedCryptoNeeded = false;
		
		while (cipherStr == null) {
			cipherStr = console.readLine("Cipher [1]: ");
			
			if ("".equals(cipherStr) || "1".equals(cipherStr)) {
				cipherStr = "AES/GCM/NoPadding";
				keySize = 128;
				unlimitedCryptoNeeded = false;
			}
			else if ("2".equals(cipherStr) || "3".equals(cipherStr)) {
				out.println();
				out.println("This cipher/keysize might not be allowed in your country.");
				String yesno = console.readLine("Are you sure you want to use it (y/n)? ");
				
				if (yesno.toLowerCase().startsWith("y")) {
					if ("1".equals(cipherStr)) {
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
						throw new Exception("Unable to enable unlimited crypto. Check out: http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html");
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
}
