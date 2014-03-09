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
package org.syncany.operations;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.MasterTO;
import org.syncany.config.to.RepoTO;
import org.syncany.connection.plugins.MasterRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.RepoRemoteFile;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;

/**
 * The connect operation connects to an existing repository at a given remote storage
 * location. Its responsibilities include:
 * 
 * <ul>
 *   <li>Downloading of the repo file. If it is encrypted, also downloading the master
 *       file to allow decrypting the repo file.</li>
 *   <li>If encrypted: Querying the user for the password and creating the master key using
 *       the password and the master salt.</li>
 *   <li>If encrypted: Decrypting and verifying the repo file.</li>
 *   <li>Creating the local Syncany folder structure in the local directory (.syncany 
 *       folder and the sub-structure) and copying the repo/master file to it.</li>
 * </ul> 
 *   
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ConnectOperation extends AbstractInitOperation {
	private static final Logger logger = Logger.getLogger(ConnectOperation.class.getSimpleName());		
	
	private ConnectOperationOptions options;
	private ConnectOperationListener listener;
    private TransferManager transferManager;
	
	public ConnectOperation(ConnectOperationOptions options, ConnectOperationListener listener) {
		super(null);
		
		this.options = options;
		this.listener = listener;
	}		
	
	@Override
	public ConnectOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Connect'");
		logger.log(Level.INFO, "--------------------------------------------");
		
		transferManager = createTransferManager(options.getConfigTO().getConnectionTO());
		
		// Create local .syncany directory
		
		File tmpRepoFile = downloadFile(transferManager, new RepoRemoteFile());
		File tmpMasterFile = null;
		
		if (CipherUtil.isEncrypted(tmpRepoFile)) {
			SaltedSecretKey masterKey = null;
			
			if (options.getConfigTO().getMasterKey() != null) {
				masterKey = options.getConfigTO().getMasterKey();
				tmpMasterFile = File.createTempFile("masterfile", "tmp");
				writeXmlFile(new MasterTO(masterKey.getSalt()), tmpMasterFile);
			}
			else {
				tmpMasterFile = downloadFile(transferManager, new MasterRemoteFile());
				MasterTO masterTO = readMasterFile(tmpMasterFile);
				
				String masterKeyPassword = getOrAskPasswordRepoFile();
				byte[] masterKeySalt = masterTO.getSalt();
				
				masterKey = createMasterKeyFromPassword(masterKeyPassword, masterKeySalt); // This takes looong!			
			}						
			
			String repoFileStr = decryptRepoFile(tmpRepoFile, masterKey);			
			verifyRepoFile(repoFileStr);
			
			options.getConfigTO().setMasterKey(masterKey);
		}
		else {
			String repoFileStr = FileUtils.readFileToString(tmpRepoFile);			
			verifyRepoFile(repoFileStr);			
		}

		// Success, now do the work!
		File appDir = createAppDirs(options.getLocalDir());	
		File configFile = new File(appDir+"/"+Config.FILE_CONFIG);
		File repoFile = new File(appDir+"/"+Config.FILE_REPO);
		File masterFile = new File(appDir+"/"+Config.FILE_MASTER);
		
		writeXmlFile(options.getConfigTO(), configFile);
		FileUtils.copyFile(tmpRepoFile, repoFile);
		tmpRepoFile.delete();
		
		if (tmpMasterFile != null) {
			FileUtils.copyFile(tmpMasterFile, masterFile);
			tmpMasterFile.delete();
		}
				
		return new ConnectOperationResult();
	}		

	private String getOrAskPasswordRepoFile() throws Exception {
		if (options.getPassword() == null) {
			if (listener == null) {
				throw new Exception("Repository file is encrypted, but password cannot be queried (no listener).");
			}
			
			return listener.getPasswordCallback();
		}
		else {
			return options.getPassword();
		}		
	}

	protected File downloadFile(TransferManager transferManager, RemoteFile remoteFile) throws Exception {
		File tmpRepoFile = File.createTempFile("syncanyfile", "tmp");
		
		try {
			transferManager.download(remoteFile, tmpRepoFile); 			
			return tmpRepoFile;			
		}
		catch (Exception e) {
			throw new Exception("Unable to connect to repository.", e);
		}		
	}
	
	private SaltedSecretKey createMasterKeyFromPassword(String masterPassword, byte[] masterKeySalt) throws Exception {
		if (listener != null) {
			listener.notifyCreateMasterKey();
		}
		
		SaltedSecretKey masterKey = CipherUtil.createMasterKey(masterPassword, masterKeySalt);
		return masterKey;
	}
	
	private String decryptRepoFile(File file, SaltedSecretKey masterKey) throws Exception {
		try {
			FileInputStream encryptedRepoConfig = new FileInputStream(file);
			return new String(CipherUtil.decrypt(encryptedRepoConfig, masterKey));			
		}
		catch (Exception e) {
			throw new Exception("Invalid password given, or repo file corrupt.");
		}		
	}		
	
	private void verifyRepoFile(String repoFileStr) throws Exception {
		try {
			Serializer serializer = new Persister();
			serializer.read(RepoTO.class, repoFileStr);
		}
		catch (Exception e) {
			throw new Exception("Repo file corrupt.");
		}	
	}
	
	private MasterTO readMasterFile(File tmpMasterFile) throws Exception {
		Serializer serializer = new Persister();
		return serializer.read(MasterTO.class, tmpMasterFile);
	}

	public static interface ConnectOperationListener {
		public String getPasswordCallback();
		public void notifyCreateMasterKey();
	}	
	
	public static class ConnectOperationOptions implements OperationOptions {
		private File localDir;
		private ConfigTO configTO;
		private String password;
		
		public File getLocalDir() {
			return localDir;
		}

		public void setLocalDir(File localDir) {
			this.localDir = localDir;
		}

		public ConfigTO getConfigTO() {
			return configTO;
		}

		public void setConfigTO(ConfigTO configTO) {
			this.configTO = configTO;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
		 
    public static class ConnectOperationResult implements OperationResult {
        // Nothing		
    }
}
