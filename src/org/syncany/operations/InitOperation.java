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
import org.syncany.config.to.RepoTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.crypto.CipherSpec;
import org.syncany.crypto.CipherUtil;

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
		
		if (encryptedRepoFileExistsOnRemoteStorage(transferManager)) {
			throw new Exception("Repo already exists. Use 'connect' command to connect to existing repository."); 
		}
		
		// Create local .syncany directory
		File appDir = createAppDirs(options.getLocalDir());	
		File configFile = new File(appDir+"/"+Config.DEFAULT_FILE_CONFIG);
		File repoFile = new File(appDir+"/"+Config.DEFAULT_FILE_REPO);
		
		// Save config.xml and repo file
		writeXmlFile(options.getConfigTO(), configFile);
		writeXmlFile(options.getRepoTO(), new File(repoFile+"-NOT-USED.xml")); // TODO [low] Remove this, not used
		
		String shareLink = null;
		boolean shareLinkEncrypted = false;
		
		if (options.isEncryptionEnabled()) {
			writeEncryptedXmlFile(options.getRepoTO(), repoFile, options.getCipherSpecs(), options.getPassword());				
			
			shareLink = getEncryptedLink(options.getConfigTO().getConnectionTO(), options.getCipherSpecs(), options.getPassword());
			shareLinkEncrypted = true;
		}	
		else {
			writeXmlFile(options.getRepoTO(), repoFile);
			
			shareLink = getPlaintextLink(options.getConfigTO().getConnectionTO());
			shareLinkEncrypted = false;
		}	
		
		uploadRepoFile(repoFile, transferManager);
					
		return new InitOperationResult(shareLink, shareLinkEncrypted);
    }          
    
	private TransferManager createTransferManager(ConnectionTO connectionTO) throws StorageException {
		Plugin plugin = Plugins.get(connectionTO.getType());
		
		Connection connection = plugin.createConnection();
		connection.init(connectionTO.getSettings());
		
		return connection.createTransferManager();
	}

	protected boolean encryptedRepoFileExistsOnRemoteStorage(TransferManager transferManager) throws Exception {
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
	
	private void uploadRepoFile(File repoFile, TransferManager transferManager) throws Exception {    		
		transferManager.upload(repoFile, new RemoteFile("repo")); // TODO [low] Naming stuff
	}    	

	private String getEncryptedLink(ConnectionTO connectionTO, List<CipherSpec> cipherSuites, String password) throws Exception {
		ByteArrayOutputStream plaintextOutputStream = new ByteArrayOutputStream();
		Serializer serializer = new Persister();
		serializer.write(connectionTO, plaintextOutputStream);
		
		byte[] encryptedConnectionBytes = CipherUtil.encrypt(new ByteArrayInputStream(plaintextOutputStream.toByteArray()), cipherSuites, password);
		String encryptedEncodedStorageXml = new String(Base64.encodeBase64(encryptedConnectionBytes, false));
		
		return "syncany://storage/1/"+encryptedEncodedStorageXml;				
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