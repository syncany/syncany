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
import org.syncany.chunk.NoTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;

/**
 * 
 * @author Philipp C. Heckel
 */
public class Config {
	private String machineName;	
	private File localDir;
	private File appDir;
	private File cacheDir;
	private File databaseDir;

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
    	
    	initLogging();
    }
    
	public Config(ConfigTO configTO) throws Exception {		
		machineName = configTO.getMachineName();
		
		initDirectories(configTO);
		initCache();
    	initEncryption(configTO);
		initChunkingFramework(configTO);
    	initConnectionPlugin(configTO);    
	}		

	private void initDirectories(ConfigTO configTO) {
		localDir = new File(configTO.getLocalDir());
		appDir = new File(configTO.getAppDir());
		cacheDir = new File(configTO.getCacheDir());
		databaseDir = new File(configTO.getDatabaseDir());				
	}
	

	private void initCache() {
		cache = new Cache(cacheDir);
	}	

	private void initChunkingFramework(ConfigTO configTO) throws EncryptionException {
		// TODO [low] make chunking options configurable			
		chunker = new FixedOffsetChunker(16 * 1024);
		multiChunker = new CustomMultiChunker(512 * 1024);//new TarMultiChunker(512 * 1024);
		
		if (encryption != null) {
			Cipher encCipher = encryption.createEncCipher(configTO.getEncryption().getPass());
			Cipher decCipher = encryption.createDecCipher(configTO.getEncryption().getPass());

			transformer = new GzipCompressor(new CipherEncrypter(encCipher, decCipher));
		}
		else {
			transformer = new NoTransformer();
		}
	}
	
	private void initEncryption(ConfigTO configTO) throws EncryptionException {
		if (configTO.getEncryption() != null && configTO.getEncryption().isEnabled()) {
	    	encryption = new Encryption();		
	    	encryption.setPassword(configTO.getEncryption().getPass());
	    	encryption.setSalt("SALT"); // TODO: What to use as salt?
		}
	}
	
	private void initConnectionPlugin(ConfigTO configTO) throws Exception {
		if (configTO.getConnection() != null) {
			Plugin plugin = Plugins.get(configTO.getConnection().getType());
	    	
	    	if (plugin == null) {
	    		throw new Exception("Plugin not supported: " + configTO.getConnection().getType());
	    	}
	    	
	    	connection = plugin.createConnection();
	    	connection.init(configTO.getConnection().getSettings());
		}
	}

	public File getAppDir() {
		return appDir;
	}

	public void setAppDir(File appDir) {
		this.appDir = appDir;
	}
	
	public void setCacheDir(File file) {
		cacheDir = file;
		cache = new Cache(cacheDir);
	}

	public File getCacheDir() {
		return cacheDir;
	}
	
	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}		

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
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

	public File getDatabaseDir() {
		return databaseDir;
	}
	
	public File getDatabaseFile() {
		return new File(databaseDir+File.separator+"local.db");	
	}

	public void setDatabaseDir(File databaseDir) {
		this.databaseDir = databaseDir;
	}
	
	private static void initLogging() {
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
}
