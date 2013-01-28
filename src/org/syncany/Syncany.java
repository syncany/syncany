package org.syncany;

import java.io.File;

import org.syncany.config.Config;
import org.syncany.config.Profile;
import org.syncany.config.Settings;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginInfo;
import org.syncany.connection.plugins.Plugins;

public class Syncany {
	public static void main(String[] args) throws Exception {
		Syncany sy = new Syncany();

		// Parse arguments
		File configFile = new File(System.getProperty("user.dir")+File.separator+"config.json");
		
		if (args.length > 0) {
			configFile = new File(args[0]);
		}
				
		// Read config
		Config cfg = new Config(configFile);
		sy.init(cfg);
		
		// sy up
		sy.up();
	}
	
	public Syncany() {
		// Read config
		// ...
	}
	
	public void up() {
		
	}	
	
	public void init(Config configFile) throws Exception {   
	  	Profile profile; 
    	Connection conn = null;
    	File rootFolder;
    	
    	// create the needed directories if not already present
    	new File(configFile.getAppDir()).mkdirs();
    	new File(configFile.getCacheDir()).mkdirs();
    	new File(configFile.getRootDir()).mkdirs();
    	

    	/* ATTENTION: Keep this order in mind 
		 * Create and configure Settings first,
		 * then create Profile */
    	
    	// SETTING EVERYTHING IN SETTINGS-CLASS
    	Settings.createInstance(
    			new File(configFile.getAppDir()), 
    			new File(configFile.getCacheDir()),
    			configFile.getMachineName()
    	);

    	profile = Profile.getInstance();

    	// set encryption password & salt
    	profile.getRepository().getEncryption().setPassword(configFile.getEncryption().getPass());
    	// TODO: What to use as salt?
    	profile.getRepository().getEncryption().setSalt("SALT"); 
    	
    	profile.getRepository().setChunkSize(Constants.DEFAULT_CHUNK_SIZE); 

    	// Load the required plugin
    	PluginInfo plugin = Plugins.get(configFile.getConnection().getType());
    	
    	if (plugin == null) {
    		throw new Exception("Plugin not supported: " + configFile.getConnection().getType());
    	}
    	
    	// initialize connection
    	conn = plugin.createConnection();
    	conn.init(configFile.getConnection().getSettings());
 
    	profile.getRepository().setConnection(conn);
    	rootFolder = new File(configFile.getRootDir());
    	profile.setRoot(rootFolder);     	
	}
	
}
