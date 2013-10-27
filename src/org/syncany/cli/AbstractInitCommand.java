package org.syncany.cli;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.util.StringUtil;

public abstract class AbstractInitCommand extends Command {
	protected Console console;
	
	protected Plugin plugin;
	protected Map<String, String> pluginSettings;
	protected Connection connection;
	
	protected File localDir;
	
	public AbstractInitCommand() {
		console = System.console();
	}	
	
	protected ConfigTO createConfigTO(File localDir, String password) throws Exception {
		ConfigTO configTO = new ConfigTO();
		
		configTO.setMachineName(getDefaultMachineName());
		
		if (password != null) {
			configTO.setPassword(password);
		}		

		ConnectionTO connectionTO = new ConnectionTO();
		connectionTO.setType(plugin.getId());
		connectionTO.setSettings(pluginSettings);
		
		configTO.setConnection(connectionTO);
		
		return configTO;
	}
	
	protected void writeXmlFile(Object source, File file) throws Exception {
		out.println("- Writing "+file);		

		Serializer serializer = new Persister();
		serializer.write(source, file);	
	}	
	
	protected void initPluginSettings(List<String> pluginSettingsOptList) throws Exception {		
		pluginSettings = new HashMap<String, String>();
		
		// Fill settings map
		for (String pluginSettingKeyValue : pluginSettingsOptList) {
			String[] keyValue = pluginSettingKeyValue.split("=", 2);
			
			if (keyValue.length != 2) {
				throw new Exception("Invalid setting: "+pluginSettingKeyValue);
			}
			
			pluginSettings.put(keyValue[0], keyValue[1]);
		}
		
		// Check if all mandatory are set
		for (String mandatorySetting : connection.getMandatorySettings()) {
			if (!pluginSettings.containsKey(mandatorySetting)) {
				throw new Exception("Not all mandatory settings are set ("+StringUtil.join(connection.getMandatorySettings(), ", ")+"). Use -Psettingname=.. to set it.");
			}
		}	
		
		connection.init(pluginSettings);
	}

	protected void initPlugin(String pluginStr) throws Exception {
		plugin = Plugins.get(pluginStr);
		
		if (plugin == null) {
			throw new Exception("ERROR: Plugin '"+pluginStr+"' does not exist.");
		}
		
		connection = plugin.createConnection();
	}

	protected void askPluginSettings() throws StorageException {
		pluginSettings = new HashMap<String, String>();
		
		String[] mandatorySettings = connection.getMandatorySettings();
		String[] optionalSettings = connection.getOptionalSettings();
		
		out.println();
		out.println("Connection details for "+plugin.getName()+" connection:");
		
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
	}

	protected void askPlugin() {
		String pluginStr = null;
		
		List<Plugin> plugins = new ArrayList<Plugin>(Plugins.list());
		String pluginsList = "";
		
		for (int i=0; i<plugins.size(); i++) {
			pluginsList += plugins.get(i).getId();
			if (i < plugins.size()-1) { pluginsList += ", "; }			
		}
		
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
		localDir = new File(".").getCanonicalFile();								
	}	

	protected String getDefaultMachineName() throws UnknownHostException {
		return new String(
			  InetAddress.getLocalHost().getHostName() 
			+ System.getProperty("user.name")
			+ Math.abs(new Random().nextInt())
		).replaceAll("[^a-zA-Z0-9]", "");		
	}
	
}
