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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config.ConfigException;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.RepoTO;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;

/**
 * The config helper provides convenience functions to load the configuration from
 * the local application repo.
 *  
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ConfigHelper {
	private static final Logger logger = Logger.getLogger(ConfigHelper.class.getSimpleName());	
	
	public static Config loadConfig(File localDir) throws ConfigException {
		if (localDir == null) {
			throw new ConfigException("Argument localDir cannot be null.");
		}
		
		File appDir = new File(localDir+"/"+Config.DIR_APPLICATION);
		
		if (appDir.exists()) {
			logger.log(Level.INFO, "Loading config from {0} ...", localDir);				

			ConfigTO configTO = ConfigHelper.loadConfigTO(localDir);
			RepoTO repoTO = ConfigHelper.loadRepoTO(localDir, configTO);
			
			return new Config(localDir, configTO, repoTO);
		}		
		else {
			logger.log(Level.INFO, "Not loading config, app dir does not exist: {0}", appDir);
			return null;
		}
	}
	
    public static ConfigTO loadConfigTO(File localDir) throws ConfigException {
		File configFile = new File(localDir+"/"+Config.DIR_APPLICATION+"/"+Config.FILE_CONFIG);
		
		if (!configFile.exists()) {
			throw new ConfigException("Cannot find config file at "+configFile+". Try connecting to a repository using 'connect', or 'init' to create a new one.");
		}
		
		return ConfigTO.load(configFile);		
	}
    
    public static RepoTO loadRepoTO(File localDir, ConfigTO configTO) throws ConfigException {
		File repoFile = new File(localDir+"/"+Config.DIR_APPLICATION+"/"+Config.FILE_REPO);
		
		if (!repoFile.exists()) {
			throw new ConfigException("Cannot find repository file at "+repoFile+". Try connecting to a repository using 'connect', or 'init' to create a new one.");
		}
		
		try {
			if (CipherUtil.isEncrypted(repoFile)) {
				return loadEncryptedRepoTO(repoFile, configTO);
			}
			else {
				return loadPlaintextRepoTO(repoFile, configTO);
			}
		}
		catch (Exception e) {
			throw new ConfigException("Cannot load repo file: "+e.getMessage(), e);
		}
	}
    
    private static RepoTO loadEncryptedRepoTO(File repoFile, ConfigTO configTO) throws Exception {
    	logger.log(Level.INFO, "Loading encrypted repo file from {0} ...", repoFile);				

		SaltedSecretKey masterKey = configTO.getMasterKey();
		
		if (masterKey == null) {
			throw new ConfigException("Repo file is encrypted, but master key not set in config file.");
		}
		
		String repoFileStr = new String(CipherUtil.decrypt(new FileInputStream(repoFile), masterKey));
		return new Persister().read(RepoTO.class, repoFileStr);
    }
    
    private static RepoTO loadPlaintextRepoTO(File repoFile, ConfigTO configTO) throws Exception {
    	logger.log(Level.INFO, "Loading (unencrypted) repo file from {0} ...", repoFile);
		return new Persister().read(RepoTO.class, repoFile);
    }
    
    /**
     * Helper method to find the local sync directory, starting from a path equal
     * or inside the local sync directory. If the starting path is not inside or equal
     * to the local directory, <tt>null</tt> is returned. 
     * 
     * <p>To find the local directory, the method looks for a file named 
     * "{@link Config#DIR_APPLICATION}/{@link Config#FILE_CONFIG}". If it is found, it stops.
     * If not, it continues looking in the parent directory.
     * 
     * <p>Example: If /home/user/Syncany is the local sync directory and /home/user/NotSyncany
     * is not a local directory, the method will return the following:
     * 
     * <ul>
     *  <li>findLocalDirInPath(/home/user/Syncany) -&gt; /home/user/Syncany</li>
     *  <li>findLocalDirInPath(/home/user/Syncany/some/subfolder) -&gt; /home/user/Syncany</li>
     *  <li>findLocalDirInPath(/home/user/NotSyncany) -&gt;null</li>
     * </ul>
     * 
     * @param startingPath Path to start the search from
     * @return Returns the local directory (if found), or <tt>null</tt> otherwise
     */
    public static File findLocalDirInPath(File startingPath) {
    	try {
			File currentSearchFolder = startingPath.getCanonicalFile();
			
			while (currentSearchFolder != null) {
				File possibleAppDir = new File(currentSearchFolder+"/"+Config.DIR_APPLICATION);
				File possibleConfigFile = new File(possibleAppDir+"/"+Config.FILE_CONFIG);
					
				if (possibleAppDir.exists() && possibleConfigFile.exists()) {
					return possibleAppDir.getParentFile().getCanonicalFile();
				}
				
				currentSearchFolder = currentSearchFolder.getParentFile();
			}
			 
			return null;
    	}
    	catch (IOException e) {
    		throw new RuntimeException("Unable to determine local directory starting from: "+startingPath, e);
    	}
	}
}
