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
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.crypto.CipherUtil;
import org.syncany.crypto.SaltedSecretKey;

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
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Connect'");
		logger.log(Level.INFO, "--------------------------------------------");
		
		transferManager = createTransferManager(options.getConfigTO().getConnectionTO());
		
		// Create local .syncany directory
		
		File tmpRepoFile = downloadFile(transferManager, new RemoteFile("repo")); // TODO [low] naming stuff
		File tmpMasterFile = null;
		
		if (CipherUtil.isEncrypted(tmpRepoFile)) {
			SaltedSecretKey masterKey = null;
			
			if (options.getConfigTO().getMasterKey() != null) {
				masterKey = options.getConfigTO().getMasterKey(); // TODO [medium] Also create master file! 
			}
			else {
				tmpMasterFile = downloadFile(transferManager, new RemoteFile("master")); // TODO [low] naming stuff
				MasterTO masterTO = readMasterFile(tmpMasterFile);
				
				String masterKeyPassword = getOrAskPasswordRepoFile();
				byte[] masterKeySalt = masterTO.getSalt();
				
				masterKey = CipherUtil.createMasterKey(masterKeyPassword, masterKeySalt); // This takes looong!			
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
		File configFile = new File(appDir+"/"+Config.DEFAULT_FILE_CONFIG);
		File repoFile = new File(appDir+"/"+Config.DEFAULT_FILE_REPO);
		File masterFile = new File(appDir+"/"+Config.DEFAULT_FILE_MASTER);
		
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
	
	private String decryptRepoFile(File file, SaltedSecretKey masterKey) throws Exception {
		try {
			FileInputStream encryptedRepoConfig = new FileInputStream(file);
			return CipherUtil.decryptToString(encryptedRepoConfig, masterKey);			
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
