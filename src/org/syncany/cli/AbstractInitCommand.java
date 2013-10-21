package org.syncany.cli;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Encryption;
import org.syncany.config.to.LocalTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.ChunkerTO;
import org.syncany.config.to.RepoTO.MultichunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
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
	
	protected String machineName;
	protected File appDir;
	protected String password;
	
	public AbstractInitCommand() {
		console = System.console();
	}	

	protected void writeLocalFile() throws Exception {
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

	protected void writeRepoFile() throws Exception {
		// Make transfer object
		RepoTO repoTO = new RepoTO();
				
		// Chunker (default for now)
		ChunkerTO chunkerTO = new ChunkerTO();
		chunkerTO.setType("fixed");
		chunkerTO.setSettings(new HashMap<String, String>());
		chunkerTO.getSettings().put("size", "16");
		
		// Multichunker (default for now)
		MultichunkerTO multichunkerTO = new MultichunkerTO();
		multichunkerTO.setType("zip");
		multichunkerTO.setSettings(new HashMap<String,String>());
		multichunkerTO.getSettings().put("size", "512");
		
		// Transformers
		List<TransformerTO> transformersTO = new ArrayList<TransformerTO>();

		TransformerTO gzipTransformerTO = new TransformerTO();
		gzipTransformerTO.setType("gzip");
		transformersTO.add(gzipTransformerTO);
		
		// Add to repo transfer object
		repoTO.setChunker(chunkerTO);
		repoTO.setMultichunker(multichunkerTO);
		repoTO.setTransformers(transformersTO);
		
		// Write file
		File file = new File(appDir+"/repo.xml");
		out.println("- Writing "+file);		
		
		Serializer serializer = new Persister();
		serializer.write(repoTO, file);
		
		
		/*
		
		// Create salted HMAC
		byte[] salt = createSalt();		
		SecretKeySpec keySpec = createHmacKeySpec(password, salt);

		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(keySpec);
		
		byte[] hmac = mac.doFinal(repoXmlPayloadBytes);
		
		// Make envelope
		StringBuilder xmlEnvelope = new StringBuilder();
		
		xmlEnvelope.append("<repo xmlns=\"http://syncany.org/repo/1\">\n");
		xmlEnvelope.append("\t<envelope>\n");
		xmlEnvelope.append(repoXmlPayloadStr);
		xmlEnvelope.append("\t</envelope>\n");
		xmlEnvelope.append("\t<signature>\n");
		xmlEnvelope.append("\t\t<salt>").append(StringUtil.toHex(salt)).append("</salt>\n");
		xmlEnvelope.append("\t\t<hmac>").append(StringUtil.toHex(hmac)).append("</hmac>\n");
		xmlEnvelope.append("\t</signature>\n");
		xmlEnvelope.append("</repo>");
		
		// Write file
		File repoFile = new File(appDir+"/repo.xml");
		out.println("- Writing "+repoFile);		
		
		PrintWriter out = new PrintWriter(new FileWriter(repoFile));		
		out.print(xmlEnvelope.toString());
		out.close();		*/
	}
	
	protected byte[] createSalt() {
    	byte[] salt = new byte[20];    	
    	new SecureRandom().nextBytes(salt);
    	
    	return salt;
    }
	
	 protected SecretKeySpec createHmacKeySpec(String password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
    	// Derive secret key from password 
    	SecretKeyFactory factory = SecretKeyFactory.getInstance(Encryption.KEY_DERIVATION_FUNCTION);
        KeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, 1000, 20);
        SecretKey secretKey = factory.generateSecret(pbeKeySpec); 
        
        return new SecretKeySpec(secretKey.getEncoded(), "HmacSHA1");          
    }

	protected void initPassword() {
		out.println();
		out.println("The password is used to encrypt data on the remote storage.");
		out.println("Please choose it wisely.");
		out.println();
		
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
	}

	protected void initRepoTest() throws Exception {
		// Test connection
		File tmpRepoFile = File.createTempFile("syncanyrepo", "tmp");
		
		try {
			out.print("Trying to connect ... ");
			TransferManager transferManager = connection.createTransferManager();
			Map<String, RemoteFile> repoFileList = transferManager.list("repo");
			
			if (repoFileList.containsKey("repo")) {
				transferManager.download(new RemoteFile("repo"), tmpRepoFile);	
			}			
		}
		catch (Exception e) {
			throw new Exception("Unable to connect to repository.", e);
		}		
	}

	protected void initPluginSettings() throws StorageException {
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

	protected void initPlugin() {
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

	protected void initMachineName() throws UnknownHostException {
		machineName = new String(
			  InetAddress.getLocalHost().getHostName() 
			+ System.getProperty("user.name")
			+ Math.abs(new Random().nextInt())
		).replaceAll("[^a-zA-Z0-9]", "");		
	}

}
