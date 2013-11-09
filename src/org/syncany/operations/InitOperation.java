/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.to.ConfigTO;
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.config.to.MasterTO;
import org.syncany.config.to.RepoTO;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;
import org.syncany.util.StringUtil;

public class InitOperation extends AbstractInitOperation {
    private static final Logger logger = Logger.getLogger(InitOperation.class.getSimpleName());        
    private InitOperationOptions options;
    private TransferManager transferManager;
    
    public InitOperation(InitOperationOptions options) {
        super(null);
        this.options = options;
    }        
            
    public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Init'");
		logger.log(Level.INFO, "--------------------------------------------");                      

		transferManager = createTransferManager(options.getConfigTO().getConnectionTO());
		
		if (repoFileExistsOnRemoteStorage(transferManager)) {
			throw new Exception("Repo already exists. Use 'connect' command to connect to existing repository."); 
		}
		
		// Create local .syncany directory
		File appDir = createAppDirs(options.getLocalDir());	
		File configFile = new File(appDir+"/"+Config.DEFAULT_FILE_CONFIG);
		File repoFile = new File(appDir+"/"+Config.DEFAULT_FILE_REPO);
		File masterFile = new File(appDir+"/"+Config.DEFAULT_FILE_MASTER);
		
		// Save config.xml and repo file
		
		String shareLink = null;
		boolean shareLinkEncrypted = false;
		
		if (options.isEncryptionEnabled()) {
			SaltedSecretKey masterKey = createMasterKeyFromPassword(options.getPassword()); // This takes looong!			
			options.getConfigTO().setMasterKey(masterKey);
			
			writeXmlFile(new MasterTO(masterKey.getSalt()), masterFile);
			writeEncryptedXmlFile(options.getRepoTO(), repoFile, options.getCipherSpecs(), masterKey);				
			
			shareLink = getEncryptedLink(options.getConfigTO().getConnectionTO(), options.getCipherSpecs(), masterKey);
			shareLinkEncrypted = true;
		}	
		else {
			writeXmlFile(options.getRepoTO(), repoFile);
			
			shareLink = getPlaintextLink(options.getConfigTO().getConnectionTO());
			shareLinkEncrypted = false;
		}	
		
		writeXmlFile(options.getConfigTO(), configFile);
		writeXmlFile(options.getRepoTO(), new File(repoFile+"-NOT-USED.xml")); // TODO [low] Remove this, not used

		// Make remote changes
		transferManager.init();
		
		if (masterFile.exists()) {
			uploadMasterFile(masterFile, transferManager);
		}
		
		uploadRepoFile(repoFile, transferManager);
					
		return new InitOperationResult(shareLink, shareLinkEncrypted);
    }          
    
	private SaltedSecretKey createMasterKeyFromPassword(String password) throws Exception {
		SaltedSecretKey masterKey = CipherUtil.createMasterKey(password);
		
		System.out.println(StringUtil.toHex(masterKey.getEncoded()));
		return masterKey;
	}

	protected boolean repoFileExistsOnRemoteStorage(TransferManager transferManager) throws Exception {
		try {
			Map<String, RemoteFile> repoFileList = transferManager.list("repo"); // TODO [medium] naming stuff
			
			if (repoFileList.containsKey("repo")) {
				return true;
			}			
			else {
				return false;
			}
		}
		catch (Exception e) {
			throw new Exception("Unable to connect to repository.", e);
		}		
	}
	
	private void uploadMasterFile(File masterFile, TransferManager transferManager) throws Exception {    		
		transferManager.upload(masterFile, new RemoteFile("master")); // TODO [low] Naming stuff
	}  
	
	private void uploadRepoFile(File repoFile, TransferManager transferManager) throws Exception {    		
		transferManager.upload(repoFile, new RemoteFile("repo")); // TODO [low] Naming stuff
	}    	

	private String getEncryptedLink(ConnectionTO connectionTO, List<CipherSpec> cipherSuites, SaltedSecretKey masterKey) throws Exception {
		ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
		Serializer serializer = new Persister();
		serializer.write(connectionTO, plaintextOutputStream);
		
		byte[] masterKeySalt = masterKey.getSalt();
		String masterKeySaltEncodedStr = new String(Base64.encodeBase64(masterKeySalt, false));
		
		byte[] encryptedConnectionBytes = CipherUtil.encrypt(new ByteArrayInputStream(plaintextOutputStream.toByteArray()), cipherSuites, masterKey);
		String encryptedEncodedStorageXml = new String(Base64.encodeBase64(encryptedConnectionBytes, false));
		
		return "syncany://storage/1/"+masterKeySaltEncodedStr+"-"+encryptedEncodedStorageXml;				
	}
	
	private String getPlaintextLink(ConnectionTO connectionTO) throws Exception {
		ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
		Serializer serializer = new Persister();
		serializer.write(connectionTO, plaintextOutputStream);
		
		byte[] plaintextStorageXml = plaintextOutputStream.toByteArray();
		String plaintextEncodedStorageXml = new String(Base64.encodeBase64(plaintextStorageXml, false));
		
		return "syncany://storage/1/not-encrypted/"+plaintextEncodedStorageXml;			
	}
 
    public static class InitOperationOptions implements OperationOptions {
    	private File localDir;
    	private ConfigTO configTO;
    	private RepoTO repoTO;
    	private boolean encryptionEnabled;
    	private List<CipherSpec> cipherSpecs;
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
		
		public RepoTO getRepoTO() {
			return repoTO;
		}
		
		public void setRepoTO(RepoTO repoTO) {
			this.repoTO = repoTO;
		}

		public boolean isEncryptionEnabled() {
			return encryptionEnabled;
		}

		public void setEncryptionEnabled(boolean encryptionEnabled) {
			this.encryptionEnabled = encryptionEnabled;
		}

		public List<CipherSpec> getCipherSpecs() {
			return cipherSpecs;
		}

		public void setCipherSpecs(List<CipherSpec> cipherSpecs) {
			this.cipherSpecs = cipherSpecs;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}  						
    }
    
    public class InitOperationResult implements OperationResult {
        private String shareLink;
    	private boolean shareLinkEncrypted;
        
		public InitOperationResult(String shareLink, boolean shareLinkEncrypted) {
			this.shareLink = shareLink;
			this.shareLinkEncrypted = shareLinkEncrypted;
		}

		public String getShareLink() {
			return shareLink;
		}

		public boolean isShareLinkEncrypted() {
			return shareLinkEncrypted;
		}   				
    }
}