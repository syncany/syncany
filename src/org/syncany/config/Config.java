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
import java.util.ArrayList;

import org.syncany.chunk.Chunker;
import org.syncany.chunk.CipherTransformer;
import org.syncany.chunk.FixedOffsetChunker;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.NoTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.chunk.ZipMultiChunker;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.util.FileUtil;

/**
 * 
 * @author Philipp C. Heckel
 */
public class Config {
	public static final String DEFAULT_DIR_APPLICATION = ".syncany";
	public static final String DEFAULT_DIR_CACHE = "cache";
	public static final String DEFAULT_DIR_DATABASE = "db";
	public static final String DEFAULT_DIR_LOG = "logs";
	public static final String DEFAULT_FILE_CONFIG = "config.xml";
	public static final String DEFAULT_FILE_REPO = "repo";
	
	private String machineName;	
	private File localDir;
	private File appDir;
	private File cacheDir;
	private File databaseDir;
	private File logDir;
	
	private String password;

	private Cache cache;	
	private Connection connection;
    private Chunker chunker;
    private MultiChunker multiChunker;
    private Transformer transformer;
      
    static {    	    	
        // Dynamically load logging config from logging.properties instead of having to provide the
    	// Java property at runtime: -Djava.util.logging.config.file=logging.properties
    	
        // This code is HERE because the Config class is used almost everywhere
        // and initialized in the beginning.
    	
    	Logging.init();
    }
    
	public Config(ConfigTO configTO, RepoTO repoTO) throws Exception {		
		// Initialize config
		// WARNING: Do not move around without knowing what you are doing!
		initMachineName(configTO);
		initPassword(configTO);
		initDirectories(configTO);
		initCache();
		initRepo(repoTO);
    	initConnection(configTO);    
	}		

	private void initMachineName(ConfigTO configTO) throws ConfigException {
		if (configTO.getMachineName() == null || !configTO.getMachineName().matches("[a-zA-Z0-9]+")) {
			throw new ConfigException("Machine name cannot be empty and must be only characters and numbers (A-Z, 0-9).");
		}
		
		machineName = configTO.getMachineName();
	}
	
	private void initPassword(ConfigTO configTO) {
		password = configTO.getPassword(); // can be null!
	}

	private void initDirectories(ConfigTO configTO) throws ConfigException {
		// App folder
		if (configTO.getAppDir() != null) {			
			appDir = FileUtil.getCanonicalFile(new File(configTO.getAppDir()));
			
			if (!appDir.exists()) {
				throw new ConfigException("Directory 'appDir' must exist if it is explicitly specified: "+appDir);
			}			
		}
		else if (configTO.getConfigFile() != null) {
			appDir = FileUtil.getCanonicalFile(new File(configTO.getConfigFile()).getParentFile());
			
			if (!DEFAULT_DIR_APPLICATION.equals(appDir.getName())) {
				throw new ConfigException("Directory 'appDir' must exist either be explicitly specified, or folder of config file must be named '.syncany' to derive it.");
			}
		}
		else {
			throw new ConfigException("Not a config error: Either 'appDir' or 'configFile' must be set."); 
		}
					
		// Local folder
		if (configTO.getLocalDir() != null) {
			localDir = FileUtil.getCanonicalFile(new File(configTO.getLocalDir()));
			
			if (!localDir.exists()) {
				throw new ConfigException("Directory 'localDir' must exist if it is explicitly specified: "+localDir);
			}
		}
		else {
			localDir = FileUtil.getCanonicalFile(appDir.getParentFile());
		}
		
		// Cache folder
		if (configTO.getCacheDir() != null) {
			cacheDir = FileUtil.getCanonicalFile(new File(configTO.getCacheDir()));
			
			if (!cacheDir.exists()) {
				throw new ConfigException("Directory 'cacheDir' must exist if it is explicitly specified: "+cacheDir);
			}
		}
		else {
			cacheDir = FileUtil.getCanonicalFile(new File(appDir+File.separator+DEFAULT_DIR_CACHE));
		}
		
		// Database folder
		if (configTO.getDatabaseDir() != null) {
			databaseDir = FileUtil.getCanonicalFile(new File(configTO.getDatabaseDir()));
			
			if (!databaseDir.exists()) {
				throw new ConfigException("Directory 'databaseDir' must exist if it is explicitly specified: "+databaseDir);
			}
		}
		else {
			databaseDir = FileUtil.getCanonicalFile(new File(appDir+File.separator+DEFAULT_DIR_DATABASE));
		}	
		
		// Log folder
		if (configTO.getLogDir() != null) {
			logDir = FileUtil.getCanonicalFile(new File(configTO.getLogDir()));
			
			if (!logDir.exists()) {
				throw new ConfigException("Directory 'logDir' must exist if it is explicitly specified: "+logDir);
			}
		}
		else {
			logDir = FileUtil.getCanonicalFile(new File(appDir+File.separator+DEFAULT_DIR_LOG));
		}	
	}
	
	private void initCache() {
		cache = new Cache(cacheDir);
	}	

	private void initRepo(RepoTO repoTO) throws Exception {
		// TODO [low] make chunking options configurable
		
		chunker = new FixedOffsetChunker(16*1024); //new TTTDChunker(16*1024);// 
		multiChunker = new ZipMultiChunker(2048*1024);
		
		if (repoTO.getTransformerTOs() == null || repoTO.getTransformerTOs().size() == 0) {
			transformer = new NoTransformer();
		}
		else {			
			ArrayList<TransformerTO> transformerTOs = new ArrayList<TransformerTO>(repoTO.getTransformerTOs());
			Transformer lastTransformer = null;
			
			for (int i=transformerTOs.size()-1; i>=0; i--) {
				TransformerTO transformerTO = transformerTOs.get(i);
				Transformer transformer = Transformer.getInstance(transformerTO.getType());
				
				if (transformer == null) {
					throw new Exception("Cannot find transformer '"+transformerTO.getType()+"'");
				}
				
				if (transformer instanceof CipherTransformer) { // Dirty workaround
					transformerTO.getSettings().put(CipherTransformer.PROPERTY_PASSWORD, getPassword());
				}
				
				transformer.init(transformerTO.getSettings());
				
				if (lastTransformer != null) {
					transformer.setNextTransformer(lastTransformer);
				}
				
				lastTransformer = transformer;
			}
			
			transformer = lastTransformer;
		}
	}
	
	private void initConnection(ConfigTO configTO) throws Exception {
		if (configTO.getConnectionTO() != null) {
			Plugin plugin = Plugins.get(configTO.getConnectionTO().getType());
	    	
	    	if (plugin == null) {
	    		throw new Exception("Plugin not supported: " + configTO.getConnectionTO().getType());
	    	}
	    	
	    	connection = plugin.createConnection();
	    	connection.init(configTO.getConnectionTO().getSettings());
		}
	}
	
	public void setCacheDir(File file) {
		cacheDir = file;
		cache = new Cache(cacheDir);
	}

	public File getCacheDir() {
		return cacheDir;
	}	
	
	public File getAppDir() {
		return appDir;
	}

	public void setAppDir(File appDir) {
		this.appDir = appDir;
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
	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public File getDatabaseFile() {
		return new File(databaseDir+File.separator+"local.db");	
	}
	
	public File getDirtyDatabaseFile() {
		return new File(databaseDir+File.separator+"dirty.db");	
	}
	
	public File getKnownDatabaseListFile() {
		return new File(databaseDir+File.separator+"knowndbs.list");	
	}

	public void setDatabaseDir(File databaseDir) {
		this.databaseDir = databaseDir;
	}	

	public File getLogDir() {
		return logDir;
	}
	
	public void setLogDir(File logDir) {
		this.logDir = logDir;
	}
	
	public static class ConfigException extends Exception {
		private static final long serialVersionUID = 4414807565457521855L;

		public ConfigException(Throwable cause) {
	        super(cause);
	    }

	    public ConfigException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public ConfigException(String message) {
	        super(message);
	    }

	    public ConfigException() {
	    }    
	}
}
