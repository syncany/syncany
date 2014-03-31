/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import org.syncany.chunk.FixedChunker;
import org.syncany.chunk.MultiChunker;
import org.syncany.chunk.NoTransformer;
import org.syncany.chunk.Transformer;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.RepoTO.MultiChunkerTO;
import org.syncany.config.to.RepoTO.TransformerTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.StorageException;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.database.DatabaseConnectionFactory;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

/**
 * The config class is the central point to configure a Syncany instance. It is mainly
 * used in the operations, but parts of it are also used in other parts of the 
 * application -- especially file locations and names.
 * 
 * <p>An instance of the <tt>Config</tt> class must be created through the transfer 
 * objects {@link ConfigTO} and {@link RepoTO}. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Config {	
	public static final String DIR_APPLICATION = ".syncany";
	public static final String DIR_CACHE = "cache";
	public static final String DIR_DATABASE = "db";
	public static final String DIR_LOG = "logs";
	public static final String FILE_CONFIG = "config.xml";
	public static final String FILE_REPO = "syncany";
	public static final String FILE_MASTER = "master";
	public static final String FILE_IGNORE = ".syignore";
		
	private byte[] repoId;
	private String machineName;
	private String displayName;
	private File localDir;
	private File appDir;
	private File cacheDir;
	private File databaseDir;
	private File logDir;
	
	private SaltedSecretKey masterKey;

	private Cache cache;	
	private Connection connection;
    private Chunker chunker;
    private MultiChunker multiChunker;
    private Transformer transformer;
    private IgnoredFiles ignoredFiles;
      
    static {    	    	
    	Logging.init();
    }
    
	public Config(File aLocalDir, ConfigTO configTO, RepoTO repoTO) throws ConfigException {
		if (aLocalDir == null || configTO == null || repoTO == null) {
			throw new ConfigException("Arguments aLocalDir, configTO and repoTO cannot be null.");
		}
		
		initNames(configTO);
		initMasterKey(configTO);
		initDirectories(aLocalDir);
		initCache();
		initIgnoredFile();
		initRepo(repoTO);
    	initConnection(configTO);  	
	}		
	
	private void initNames(ConfigTO configTO) throws ConfigException {
		if (configTO.getMachineName() == null || !configTO.getMachineName().matches("[a-zA-Z0-9]+")) {
			throw new ConfigException("Machine name cannot be empty and must be only characters and numbers (A-Z, 0-9).");
		}
		
		machineName = configTO.getMachineName();
		displayName = configTO.getDisplayName();
	}
	
	private void initMasterKey(ConfigTO configTO) {
		masterKey = configTO.getMasterKey(); // can be null			
	}

	private void initDirectories(File aLocalDir) throws ConfigException {
		localDir = FileUtil.getCanonicalFile(aLocalDir);		
		appDir = FileUtil.getCanonicalFile(new File(localDir+File.separator+DIR_APPLICATION));
		cacheDir = FileUtil.getCanonicalFile(new File(appDir+File.separator+DIR_CACHE));
		databaseDir = FileUtil.getCanonicalFile(new File(appDir+File.separator+DIR_DATABASE));
		logDir = FileUtil.getCanonicalFile(new File(appDir+File.separator+DIR_LOG));
	}
	
	private void initCache() {
		cache = new Cache(cacheDir);
	}	
	
	private void initIgnoredFile() throws ConfigException {
		File ignoreFile = new File(localDir, FILE_IGNORE);
		ignoredFiles = new IgnoredFiles(ignoreFile);
	}

	private void initRepo(RepoTO repoTO) throws ConfigException {
		try {
			initRepoId(repoTO);
			initChunker(repoTO);
			initMultiChunker(repoTO);
			initTransformers(repoTO);
		}
		catch (Exception e) {
			throw new ConfigException("Unable to initialize repository information from config.", e);
		}
	}
	
	private void initRepoId(RepoTO repoTO) {
		repoId = repoTO.getRepoId();
	}

	private void initChunker(RepoTO repoTO) throws Exception {
		// TODO [feature request] make chunking options configurable, something like this:
		//  chunker = Chunker.getInstance(repoTO.getChunker().getType());
		//  chunker.init(repoTO.getChunker().getSettings());
		
		chunker = new FixedChunker(512*1024, "SHA1");
		
		/*new MimeTypeChunker(
			new FixedChunker(64*1024, "SHA1"),
			new FixedChunker(2*1024*1024, "SHA1"),
			Arrays.asList(new String[] {
				"application/x-gzip",
				"application/x-compressed.*",
				"application/zip",		
				"application/x-java-archive",	
				"application/octet-stream",
				"application/x-sharedlib",
				"application/x-executable",
				"application/x-iso9660-image",
				"image/.+",
				"audio/.+",
				"video/.+",				
			})
		);*/
	}

	private void initMultiChunker(RepoTO repoTO) throws ConfigException {
		MultiChunkerTO multiChunkerTO = repoTO.getMultiChunker();

		if (multiChunkerTO == null) {
			throw new ConfigException("No multichunker in repository config.");			
		}
		
		multiChunker = MultiChunker.getInstance(multiChunkerTO.getType());
		
		if (multiChunker == null) {
			throw new ConfigException("Invalid multichunk type or settings: " + multiChunkerTO.getType());
		}
		
		multiChunker.init(multiChunkerTO.getSettings());
	}

	private void initTransformers(RepoTO repoTO) throws Exception {
		if (repoTO.getTransformers() == null || repoTO.getTransformers().size() == 0) {
			transformer = new NoTransformer();
		}
		else {			
			ArrayList<TransformerTO> transformerTOs = new ArrayList<TransformerTO>(repoTO.getTransformers());
			Transformer lastTransformer = null;
			
			for (int i=transformerTOs.size()-1; i>=0; i--) {
				TransformerTO transformerTO = transformerTOs.get(i);
				Transformer transformer = Transformer.getInstance(transformerTO.getType());
				
				if (transformer == null) {
					throw new ConfigException("Cannot find transformer '"+transformerTO.getType()+"'");
				}
				
				if (transformer instanceof CipherTransformer) { // Dirty workaround
					transformerTO.getSettings().put(CipherTransformer.PROPERTY_MASTER_KEY, StringUtil.toHex(getMasterKey().getEncoded()));
					transformerTO.getSettings().put(CipherTransformer.PROPERTY_MASTER_KEY_SALT, StringUtil.toHex(getMasterKey().getSalt()));
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

	private void initConnection(ConfigTO configTO) throws ConfigException {
		if (configTO.getConnectionTO() != null) {
			Plugin plugin = Plugins.get(configTO.getConnectionTO().getType());
	    	
	    	if (plugin == null) {
	    		throw new ConfigException("Plugin not supported: " + configTO.getConnectionTO().getType());
	    	}
	    	
	    	try {
		    	connection = plugin.createConnection();
		    	connection.init(configTO.getConnectionTO().getSettings());
	    	}
	    	catch (StorageException e) {
	    		throw new ConfigException("Cannot initialize storage: "+e.getMessage(), e);
	    	}
		}
	}
	
	public java.sql.Connection createDatabaseConnection() {
		return DatabaseConnectionFactory.createConnection(getDatabaseFile());
	}

	public File getCacheDir() {
		return cacheDir;
	}	
	
	public File getAppDir() {
		return appDir;
	}

	public String getMachineName() {
		return machineName;
	}

	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}			

    public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public byte[] getRepoId() {
		return repoId;
	}
    
    public Chunker getChunker() {
        return chunker;
    }
    
	public Cache getCache() {
		return cache;
	}
	
	public IgnoredFiles getIgnoredFiles() {
		return ignoredFiles;
	}

	public MultiChunker getMultiChunker() {
		return multiChunker;
	}

	public Transformer getTransformer() {
		return transformer;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public File getLocalDir() {
		return localDir;
	}

	public File getDatabaseDir() {
		return databaseDir;
	}	

	public SaltedSecretKey getMasterKey() {
		return masterKey;
	}

	public File getDatabaseFile() {
		return new File(databaseDir+File.separator+"local.db");	
	}	

	public File getLogDir() {
		return logDir;
	}
	
	public static class ConfigException extends Exception {
		private static final long serialVersionUID = 4414807565457521855L;

	    public ConfigException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public ConfigException(String message) {
	        super(message);
	    }
	}
}
