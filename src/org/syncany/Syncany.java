package org.syncany;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.syncany.chunk.chunking.Chunk;
import org.syncany.chunk.chunking.Chunker;
import org.syncany.chunk.chunking.FixedOffsetChunker;
import org.syncany.chunk.multi.CustomMultiChunker;
import org.syncany.chunk.multi.MultiChunk;
import org.syncany.chunk.multi.MultiChunker;
import org.syncany.config.Config;
import org.syncany.config.Profile;
import org.syncany.config.Settings;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginInfo;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.experimental.db.ChunkEntry;
import org.syncany.experimental.db.FileContent;
import org.syncany.experimental.db.Database;
import org.syncany.experimental.db.FileHistory;
import org.syncany.experimental.db.FileVersion;
import org.syncany.experimental.db.MultiChunkEntry;
import org.syncany.experimental.trash.RepositoryDatabase;
import org.syncany.experimental.trash.RepositoryDatabaseVersion;
import org.syncany.experimental.trash.UpstreamStatus;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

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
		sy.up(cfg);
	}
	
	public Syncany() {
		// Read config
		// ...
	}
	
	public void up(Config cfg) {
		
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
    	profile.getRepository().getEncryption().setSalt("SALT"); // TODO: What to use as salt?    	
    	profile.getRepository().setChunkSize(Constants.DEFAULT_CHUNK_SIZE); // TODO: Make configurable

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
    	
    	// load DB
    	//if (p)
	}
	
}
