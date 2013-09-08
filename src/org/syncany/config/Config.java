/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import org.syncany.chunk.Chunker;
import org.syncany.chunk.CipherEncrypter;
import org.syncany.chunk.CustomMultiChunker;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.GzipCompressor;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.Transformer;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;

/**
 * 
 * @author Philipp C. Heckel
 */
public class Config {
	private File localDir;
	private File appDir;
	private File appCacheDir;
	private File appDatabaseDir;
	private String machineName;	
    private int chunkSize;

	private Cache cache;
	
	private Connection connection;
    private Encryption encryption;
    private Chunker chunker;
    private MultiChunker multiChunker;
    private Transformer transformer;
      
    static {    	    	
        // Dynamically load logging config from logging.properties instead of having to provide the
    	// Java property at runtime: -Djava.util.logging.config.file=logging.properties
    	
        // This code is HERE because the Config class is used almost everywhere
        // and initialized in the beginning.

    	try {
    		// Use file if exists, else use file embedded in JAR
    		File logConfig = new File("logging.properties");
    		InputStream logConfigInputStream;
    		
    		if (logConfig.exists() && logConfig.canRead()) {
    			logConfigInputStream = new FileInputStream(new File("logging.properties"));
    		}
    		else {
    			logConfigInputStream = Config.class.getResourceAsStream("/logging.properties");
    		}
    		
    	    LogManager.getLogManager().readConfiguration(logConfigInputStream);
    	}
    	catch (Exception e) {
    	    Logger.getAnonymousLogger().severe("Could not load logging.properties file from file system or JAR.");
    	    Logger.getAnonymousLogger().severe(e.getMessage());
    	    
    	    e.printStackTrace();
    	}
    }
    
	public Config(ConfigTO configTO) throws Exception {		
		machineName = configTO.getMachineName();
		
		initDirectories(configTO);
		initChunkingFramework(configTO);
    	initEncryption(configTO);
    	initConnectionPlugin(configTO);    
	}
	
	public Config(String password) throws Exception {
		initChunkingFrameworkDefaults(password);		        
		
    	encryption = new Encryption();	 	
    	encryption.setPassword(password);  
    	encryption.setSalt("SALT"); 			
	}
	
	private void initDirectories(ConfigTO configTO) {
		localDir = new File(configTO.getLocalDir());
		appDir = new File(configTO.getAppDir());
		appCacheDir = new File(configTO.getCacheDir());
		appDatabaseDir = new File(configTO.getDatabaseDir());
		
		cache = new Cache(appCacheDir);
	}

	private void initChunkingFramework(ConfigTO configTO) throws EncryptionException {
		// TODO [low] make chunking options configurable
		initChunkingFrameworkDefaults(configTO.getEncryption().getPass());
	}
	
	private void initChunkingFrameworkDefaults(String password) throws EncryptionException {
		// Create ciphers
		Encryption encryption = new Encryption();
		Cipher encCipher = encryption.createEncCipher(password);
		Cipher decCipher = encryption.createDecCipher(password);
		
		// Chunking framework		
		chunker = new FixedOffsetChunker(16 * 1024);
		multiChunker = new CustomMultiChunker(512 * 1024);//new TarMultiChunker(512 * 1024);
		transformer = new GzipCompressor(new CipherEncrypter(encCipher, decCipher));//new GzipCompressor(); // TODO [high] Use encryption!    
	}
	
	private void initEncryption(ConfigTO configTO) throws EncryptionException {
    	encryption = new Encryption();		
    	encryption.setPassword(configTO.getEncryption().getPass());
    	encryption.setSalt("SALT"); // TODO: What to use as salt?    			
	}
	
	private void initConnectionPlugin(ConfigTO configTO) throws Exception {
		Plugin plugin = Plugins.get(configTO.getConnection().getType());
    	
    	if (plugin == null) {
    		throw new Exception("Plugin not supported: " + configTO.getConnection().getType());
    	}
    	
    	connection = plugin.createConnection();
    	connection.init(configTO.getConnection().getSettings());    	
	}

	public File getAppDir() {
		return appDir;
	}

	public void setAppDir(File appDir) {
		this.appDir = appDir;
	}
	
	public void setAppCacheDir(File file) {
		appCacheDir = file;
		cache = new Cache(appCacheDir);
	}

	public File getAppCacheDir() {
		return appCacheDir;
	}
	
	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}	
	
    public int getChunkSize() {
        return chunkSize;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Chunker getChunker() {
        return chunker;
    }

    public void setChunker(Chunker chunker) {
        this.chunker = chunker;
    }
    
	public Cache getCache() {
		return cache;
	}

	public MultiChunker getMultiChunker() {
		return multiChunker;
	}

	public void setMultiChunker(MultiChunker multiChunker) {
		this.multiChunker = multiChunker;
	}

	public Transformer getTransformer() {
		return transformer;
	}

	public void setTransformer(Transformer transformer) {
		this.transformer = transformer;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public File getLocalDir() {
		return localDir;
	}

	public void setLocalDir(File localDir) {
		this.localDir = localDir;
	}

	public File getAppDatabaseDir() {
		return appDatabaseDir;
	}
	
	public File getAppDatabaseFile() {
		return new File(appDatabaseDir+File.separator+"local.db");	
	}

	public void setAppDatabaseDir(File appDatabaseDir) {
		this.appDatabaseDir = appDatabaseDir;
	}
}
