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

import org.syncany.chunk.Chunker;
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
        
	public Config(ConfigTO configTO) throws Exception {		
		machineName = configTO.getMachineName();
		
		initDirectories(configTO);
		initChunkingFramework(configTO);
    	initEncryption(configTO);
    	initConnectionPlugin(configTO);    
	}
	
	public Config(String password) throws Exception {
		chunker = new FixedOffsetChunker(16 * 1024); // FIXME Duplicate code
		multiChunker = new CustomMultiChunker(512 * 1024);  // FIXME Duplicate code
		transformer = new GzipCompressor();	 // FIXME Duplicate code	        
		
    	encryption = new Encryption();	 // FIXME Duplicate code	
    	encryption.setPassword(password); // FIXME Duplicate code
    	encryption.setSalt("SALT"); // TODO: What to use as salt?      // FIXME Duplicate code			
	}
	
	private void initDirectories(ConfigTO configTO) {
		localDir = new File(configTO.getLocalDir());
		appDir = new File(configTO.getAppDir());
		appCacheDir = new File(configTO.getCacheDir());
		appDatabaseDir = new File(configTO.getDatabaseDir());
		
		cache = new Cache(appCacheDir);
	}

	private void initChunkingFramework(ConfigTO configTO) {
		chunker = new FixedOffsetChunker(16 * 1024);
		multiChunker = new CustomMultiChunker(512 * 1024);
		transformer = new GzipCompressor();		        		
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

    public Encryption getEncryption() {
        return encryption;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
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

	public void setAppDatabaseDir(File appDatabaseDir) {
		this.appDatabaseDir = appDatabaseDir;
	}
}
