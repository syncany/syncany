package org.syncany.cli;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.to.LocalTO;
import org.syncany.config.to.StorageTO;
import org.syncany.config.to.StorageTO.ConnectionTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

public abstract class AbstractInitCommand extends Command {
	protected Console console;
	
	protected Plugin plugin;
	protected Map<String, String> pluginSettings;
	protected Connection connection;
	
	protected File appDir;
	
	public AbstractInitCommand() {
		console = System.console();
	}	

	protected void writeLocalFile(String machineName, String password) throws Exception {
		LocalTO localTO = new LocalTO();
		localTO.setMachineName(machineName);
		
		if (password != null) {
			localTO.setPassword(password);
		}		

		// Write file
		File file = new File(appDir+"/local.xml");
		out.println("- Writing "+file);		

		Serializer serializer = new Persister();
		serializer.write(localTO, file);		
	}	

	protected void writeStorageFile() throws Exception {
		// Make transfer object
		StorageTO storageTO = new StorageTO();
		
		ConnectionTO connectionTO = new ConnectionTO();
		connectionTO.setType(plugin.getId());
		connectionTO.setSettings(pluginSettings);
		
		storageTO.setConnection(connectionTO);
		
		// Write file
		File file = new File(appDir+"/storage.xml");
		out.println("- Writing "+file);		

		Serializer serializer = new Persister();
		serializer.write(storageTO, file);		
	}

	protected String askPassword() {
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
		
		return password;
	}

	protected File downloadEncryptedRepoFile() throws Exception {
		// Test connection
		File tmpRepoFile = File.createTempFile("syncanyrepo", "tmp");
		
		try {
			out.print("Trying to connect ... ");
			TransferManager transferManager = connection.createTransferManager();
			Map<String, RemoteFile> repoFileList = transferManager.list("repo");
			
			if (repoFileList.containsKey("repo")) {
				transferManager.download(new RemoteFile("repo"), tmpRepoFile);
				return tmpRepoFile;
			}			
			else {
				return null;
			}
		}
		catch (Exception e) {
			throw new Exception("Unable to connect to repository.", e);
		}		
	}

	protected void askPluginSettings() throws StorageException {
		pluginSettings = new HashMap<String, String>();
		
		String[] mandatorySettings = connection.getMandatorySettings();
		String[] optionalSettings = connection.getOptionalSettings();
		
		out.println();
		out.println("This plugin has the following settings.");
		
		for (String settingKey : mandatorySettings) {
			String settingValue = null;
			
			while (settingValue == null) {
				out.print("- "+settingKey+": ");
				settingValue = console.readLine();
				
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
			String settingValue = console.readLine();
			
			if (!"".equals(settingValue)) {
				pluginSettings.put(settingKey, settingValue);
			}
		}

		connection.init(pluginSettings);
		//operationOptions.setConnection(new ConnectionSettings(plugin.getId(), pluginSettings));		
	}

	protected void askPlugin() {
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
			pluginStr = console.readLine();
			
			plugin = Plugins.get(pluginStr);
			
			if (plugin == null) {
				out.println("ERROR: Plugin '"+pluginStr+"' does not exist.");
				out.println();
				
				pluginStr = null;
			}
		}
		
		connection = plugin.createConnection();
	}

	protected void initAppDir() throws IOException {
		appDir = new File(".").getCanonicalFile();								
	}	

	protected String getDefaultMachineName() throws UnknownHostException {
		return new String(
			  InetAddress.getLocalHost().getHostName() 
			+ System.getProperty("user.name")
			+ Math.abs(new Random().nextInt())
		).replaceAll("[^a-zA-Z0-9]", "");		
	}
	
}
