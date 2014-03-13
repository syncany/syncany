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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.MasterTO;
import org.syncany.config.to.RepoTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.connection.plugins.MasterRemoteFile;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.RepoRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.connection.plugins.TransferManager.StorageTestResult;
import org.syncany.crypto.CipherException;
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
	
	private static final Pattern LINK_PATTERN = Pattern.compile("^syncany://storage/1/(?:(not-encrypted/)(.+)|([^-]+-(.+)))$");
	private static final int LINK_PATTERN_GROUP_NOT_ENCRYPTED_FLAG = 1;
	private static final int LINK_PATTERN_GROUP_NOT_ENCRYPTED_ENCODED = 2;
	private static final int LINK_PATTERN_GROUP_ENCRYPTED_MASTER_KEY_SALT = 3;
	private static final int LINK_PATTERN_GROUP_ENCRYPTED_ENCODED = 4;

	private ConnectOperationOptions options;
	private ConnectOperationResult result;
	private ConnectOperationListener listener;
    private TransferManager transferManager;
	
	public ConnectOperation(ConnectOperationOptions options, ConnectOperationListener listener) {
		super(null);
		
		this.options = options;
		this.result = null;
		this.listener = listener;
	}		
	
	@Override
	public ConnectOperationResult execute() throws IOException, StorageException, CipherException {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Connect'");
		logger.log(Level.INFO, "--------------------------------------------");
		
		// Create valid configTO & transfer manager
		ConfigTO configTO = createConfigTO();		
		transferManager = createTransferManager(configTO.getConnectionTO());
		
		// Test the repo
		if (!performRepoTest()) {
			logger.log(Level.INFO, "- Connecting to the repo failed, repo already exists or cannot be created: " + result.getResultCode());			
			return result;
		}

		logger.log(Level.INFO, "- Connecting to the repo was successful");
		
		
		// Create local .syncany directory		
		File tmpRepoFile = downloadFile(transferManager, new RepoRemoteFile());
		File tmpMasterFile = null;
		
		if (CipherUtil.isEncrypted(tmpRepoFile)) {
			SaltedSecretKey masterKey = null;
			
			if (configTO.getMasterKey() != null) {
				masterKey = configTO.getMasterKey();
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
			
			configTO.setMasterKey(masterKey);
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
		
		writeXmlFile(configTO, configFile);
		FileUtils.copyFile(tmpRepoFile, repoFile);
		tmpRepoFile.delete();
		
		if (tmpMasterFile != null) {
			FileUtils.copyFile(tmpMasterFile, masterFile);
			tmpMasterFile.delete();
		}
				
		return new ConnectOperationResult(ConnectResultCode.OK);
	}		

	private ConfigTO createConfigTO() throws StorageException, CipherException {
		ConfigTO configTO = options.getConfigTO();
		
		if (options.getStrategy() == ConnectOptionsStrategy.CONNECTION_TO) {
			return configTO;
		}
		else if (options.getStrategy() == ConnectOptionsStrategy.CONNECTION_LINK) {
			return createConfigTOFromLink(configTO, options.getConnectLink());
		}
		else {
			throw new RuntimeException("Unhandled connect strategy: "+options.getStrategy());
		}
	}

	private ConfigTO createConfigTOFromLink(ConfigTO configTO, String link) throws StorageException, CipherException {
		Matcher linkMatcher = LINK_PATTERN.matcher(link);
		
		if (!linkMatcher.matches()) {
			throw new StorageException("Invalid link provided, must start with syncany:// and match link pattern.");
		}
		
		String notEncryptedFlag = linkMatcher.group(LINK_PATTERN_GROUP_NOT_ENCRYPTED_FLAG);
		
		String plaintext = null;
		boolean isEncryptedLink = notEncryptedFlag == null;
		
		if (isEncryptedLink) {
			String masterKeySaltStr = linkMatcher.group(LINK_PATTERN_GROUP_ENCRYPTED_MASTER_KEY_SALT);
			String ciphertext = linkMatcher.group(LINK_PATTERN_GROUP_ENCRYPTED_ENCODED);
			
			byte[] masterKeySalt = Base64.decodeBase64(masterKeySaltStr);
			byte[] ciphertextBytes = Base64.decodeBase64(ciphertext);

			String password = getOrAskPasswordRepoFile();
			
			if (listener != null) {
				listener.notifyCreateMasterKey();
			}
			
			SaltedSecretKey masterKey = CipherUtil.createMasterKey(password, masterKeySalt);
			
			ByteArrayInputStream encryptedStorageConfig = new ByteArrayInputStream(ciphertextBytes);			
			plaintext = new String(CipherUtil.decrypt(encryptedStorageConfig, masterKey));			
			
			configTO.setMasterKey(masterKey);
		}
		else {
			String encodedPlaintext = linkMatcher.group(LINK_PATTERN_GROUP_NOT_ENCRYPTED_ENCODED);
			plaintext = new String(Base64.decodeBase64(encodedPlaintext));
		}
		
		try {
			Serializer serializer = new Persister();
			ConnectionTO connectionTO = serializer.read(ConnectionTO.class, plaintext);		
			
			Plugin plugin = Plugins.get(connectionTO.getType());
			
			if (plugin == null) {
				throw new StorageException("Link contains unknown connection type '"+connectionTO.getType()+"'. Corresponding plugin not found.");
			}
		
			configTO.setConnectionTO(connectionTO);
		}
		catch (Exception e) {
			throw new StorageException(e);
		}
		
		return configTO;			
	}

	private boolean performRepoTest() {
		StorageTestResult repoTestResult = transferManager.test();
		
		switch (repoTestResult) {
		case NO_CONNECTION:
			result = new ConnectOperationResult(ConnectResultCode.NOK_NO_CONNECTION);
			return false;
						
		case REPO_EXISTS:
			return true;
			
		case REPO_EXISTS_BUT_INVALID:
			result = new ConnectOperationResult(ConnectResultCode.NOK_INVALID_REPO);
			return false;

		case NO_REPO:
		case NO_REPO_CANNOT_CREATE:
			result = new ConnectOperationResult(ConnectResultCode.NOK_NO_REPO);
			return false;
			 
		default:
			throw new RuntimeException("Test result "+repoTestResult+" should have been handled before.");
		}		
	}

	private String getOrAskPasswordRepoFile() {
		if (options.getPassword() == null) {
			if (listener == null) {
				throw new RuntimeException("Repository file is encrypted, but password cannot be queried (no listener).");
			}
			
			return listener.getPasswordCallback();
		}
		else {
			return options.getPassword();
		}		
	}

	protected File downloadFile(TransferManager transferManager, RemoteFile remoteFile) throws StorageException {		
		try {
			File tmpRepoFile = File.createTempFile("syncanyfile", "tmp");

			transferManager.download(remoteFile, tmpRepoFile); 			
			return tmpRepoFile;			
		}
		catch (Exception e) {
			throw new StorageException("Unable to connect to repository.", e);
		}		
	}
	
	private SaltedSecretKey createMasterKeyFromPassword(String masterPassword, byte[] masterKeySalt) throws CipherException {
		if (listener != null) {
			listener.notifyCreateMasterKey();
		}
		
		SaltedSecretKey masterKey = CipherUtil.createMasterKey(masterPassword, masterKeySalt);
		return masterKey;
	}
	
	private String decryptRepoFile(File file, SaltedSecretKey masterKey) throws CipherException {
		try {
			FileInputStream encryptedRepoConfig = new FileInputStream(file);
			return new String(CipherUtil.decrypt(encryptedRepoConfig, masterKey));			
		}
		catch (Exception e) {
			throw new CipherException("Invalid password given, or repo file corrupt.", e);
		}		
	}		
	
	private void verifyRepoFile(String repoFileStr) throws StorageException {
		try {
			Serializer serializer = new Persister();
			serializer.read(RepoTO.class, repoFileStr);
		}
		catch (Exception e) {
			throw new StorageException("Repo file corrupt.", e);
		}	
	}
	
	private MasterTO readMasterFile(File tmpMasterFile) throws StorageException {
		try {
			Serializer serializer = new Persister();
			return serializer.read(MasterTO.class, tmpMasterFile);
		}
		catch (Exception e) {
			throw new StorageException("Master file corrupt.", e);
		}			
	}

	public static interface ConnectOperationListener {
		public String getPasswordCallback();
		public void notifyCreateMasterKey();
	}	
	
	public enum ConnectOptionsStrategy {
		CONNECTION_LINK, CONNECTION_TO
	}
	
	public static class ConnectOperationOptions implements OperationOptions {
		private ConnectOptionsStrategy strategy;
		private ConfigTO configTO;
		private String connectLink;
		private File localDir;
		private String password;
		
		public ConnectOptionsStrategy getStrategy() {
			return strategy;
		}

		public void setStrategy(ConnectOptionsStrategy strategy) {
			this.strategy = strategy;
		}

		public String getConnectLink() {
			return connectLink;
		}

		public void setConnectLink(String connectionLink) {
			this.connectLink = connectionLink;
		}

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
		 
	public enum ConnectResultCode {
		OK, NOK_NO_REPO, NOK_INVALID_REPO, NOK_NO_CONNECTION
	}
	
    public static class ConnectOperationResult implements OperationResult {
        private ConnectResultCode resultCode = ConnectResultCode.OK;

        public ConnectOperationResult() {
			// Nothing here
		}
        
        public ConnectOperationResult(ConnectResultCode resultCode) {
			this.resultCode = resultCode;
		}

		public ConnectResultCode getResultCode() {
			return resultCode;
		}

		public void setResultCode(ConnectResultCode resultCode) {
			this.resultCode = resultCode;
		}                
    }
}
