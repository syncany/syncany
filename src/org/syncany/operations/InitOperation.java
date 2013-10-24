package org.syncany.operations;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
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
import org.syncany.crypto.CipherSuite;
import org.syncany.crypto.CipherUtil;

public class InitOperation extends Operation {
        private static final Logger logger = Logger.getLogger(InitOperation.class.getSimpleName());        
        private InitOperationOptions options;
        
        public InitOperation(InitOperationOptions options) {
            super(null);
            this.options = options;
        }        
                
        public OperationResult execute() throws Exception {
			logger.log(Level.INFO, "");
			logger.log(Level.INFO, "Running 'Init'");
			logger.log(Level.INFO, "--------------------------------------------");                      
			        
			// Create local .syncany directory
			File appDir = new File(options.getConfigTO().getLocalDir()+"/"+Config.DEFAULT_DIR_APPLICATION);
			File configFile = new File(appDir+"/"+Config.DEFAULT_FILE_CONFIG);
			File repoFile = new File(appDir+"/"+Config.DEFAULT_FILE_REPO);
			
			if (!appDir.exists()) {
				appDir.mkdir();
			}
			
			// Unset 'localDir' (we don't want to save it!)
			options.getConfigTO().setLocalDir(null);			
			
			// Save config.xml and repo file
			writeXmlFile(options.getConfigTO(), configFile);
			writeXmlFile(options.getRepoTO(), new File(repoFile+"-NOT-USED.xml")); // TODO [low] Remove this, not used
			
			String shareLink = null;
			
			if (options.isEncryptionEnabled()) {
				writeEncryptedXmlFile(options.getRepoTO(), repoFile, options.getCipherSuites(), options.getPassword());				
				shareLink = getEncryptedLink(options.getConfigTO().getConnection(), options.getCipherSuites(), options.getPassword());
			}	
			else {
				writeXmlFile(options.getRepoTO(), repoFile); 				
				shareLink = getPlaintextLink(options.getConfigTO().getConnection()); 
			}	
			
			uploadRepoFile();
						
			return new InitOperationResult(shareLink);
        }                
        
    	private void writeXmlFile(Object source, File file) throws Exception {
       		Serializer serializer = new Persister();
    		serializer.write(source, file);	
    	}	
    	
    	private void writeEncryptedXmlFile(RepoTO repoTO, File file, List<CipherSuite> cipherSuites, String password) throws Exception {				
    		ByteArrayOutputStream plaintextRepoOutputStream = new ByteArrayOutputStream();
    		Serializer serializer = new Persister();
    		serializer.write(repoTO, plaintextRepoOutputStream);
    		
    		CipherUtil.encrypt(new ByteArrayInputStream(plaintextRepoOutputStream.toByteArray()), new FileOutputStream(file), cipherSuites, password);
    	}		
    	
    	private void uploadRepoFile() throws Exception {
    		//TransferManager transferManager = connection.createTransferManager();
    		
    		//throw new Exception("upload not yet implemented.");
    		// TODO FIXME [high] Upload repo file
    		
    		
    		
    	}    	

    	private String getEncryptedLink(ConnectionTO connectionTO, List<CipherSuite> cipherSuites, String password) throws Exception {
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
        	private ConfigTO configTO;
        	private RepoTO repoTO;
        	private boolean encryptionEnabled;
        	private List<CipherSuite> cipherSuites;
        	private String password;
			
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

			public List<CipherSuite> getCipherSuites() {
				return cipherSuites;
			}

			public void setCipherSuites(List<CipherSuite> cipherSuites) {
				this.cipherSuites = cipherSuites;
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
            
			public InitOperationResult(String shareLink) {
				this.shareLink = shareLink;
			}

			public String getShareLink() {
				return shareLink;
			}                       
        }
}