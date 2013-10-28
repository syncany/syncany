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
import org.syncany.config.to.ConfigTO.ConnectionTO;
import org.syncany.config.to.RepoTO;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.Plugin;
import org.syncany.connection.plugins.Plugins;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.crypto.CipherUtil;

public class ConnectOperation extends AbstractInitOperation {
	private static final Logger logger = Logger.getLogger(ConnectOperation.class.getSimpleName());		
	
	private ConnectOperationOptions options;
	private ConnectOperationListener listener;
	
	public ConnectOperation(ConnectOperationOptions options, ConnectOperationListener listener) {
		super(null);
		
		this.options = options;
		this.listener = listener;
	}		
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Connect'");
		logger.log(Level.INFO, "--------------------------------------------");
		
		// Create local .syncany directory
		
		File tmpRepoFile = downloadRepoFile(options.getConfigTO().getConnectionTO());
		
		if (CipherUtil.isEncrypted(tmpRepoFile)) {
			String password = getOrAskPasswordForRepoFile();
			
			String repoFileStr = decryptRepoFile(tmpRepoFile, password);			
			verifyRepoFile(repoFileStr);
			
			options.getConfigTO().setPassword(password);
		}
		else {
			String repoFileStr = FileUtils.readFileToString(tmpRepoFile);			
			verifyRepoFile(repoFileStr);			
		}

		// Success, now do the work!
		File appDir = createAppDirs(options.getLocalDir());	
		File configFile = new File(appDir+"/"+Config.DEFAULT_FILE_CONFIG);
		File repoFile = new File(appDir+"/"+Config.DEFAULT_FILE_REPO);
		
		writeXmlFile(options.getConfigTO(), configFile);
		FileUtils.copyFile(tmpRepoFile, repoFile);
		tmpRepoFile.delete();
				
		return new ConnectOperationResult();
	}		

	private String getOrAskPasswordForRepoFile() throws Exception {
		String password = null;
		
		if (options.getConfigTO().getPassword() == null) {
			if (listener == null) {
				throw new Exception("Repository file is encrypted, but password cannot be queried (no listener).");
			}
			
			password = listener.getPasswordCallback();
		}
		else {
			password = options.getConfigTO().getPassword();
		}
		
		return password;		
	}

	protected File downloadRepoFile(ConnectionTO connectionTO) throws Exception {
		File tmpRepoFile = File.createTempFile("syncanyrepo", "tmp");
		
		try {
			Plugin plugin = Plugins.get(connectionTO.getType());
			
			Connection connection = plugin.createConnection();
			connection.init(connectionTO.getSettings());
			
			TransferManager transferManager = connection.createTransferManager();
			transferManager.download(new RemoteFile("repo"), tmpRepoFile); // TODO [medium] Naming stuff
			transferManager.disconnect();
			
			return tmpRepoFile;			
		}
		catch (Exception e) {
			throw new Exception("Unable to connect to repository.", e);
		}		
	}
	
	private String decryptRepoFile(File file, String password) throws Exception {
		try {
			FileInputStream encryptedRepoConfig = new FileInputStream(file);
			return CipherUtil.decryptToString(encryptedRepoConfig, password);			
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

	public static interface ConnectOperationListener {
		public String getPasswordCallback();
	}	
	
	public static class ConnectOperationOptions implements OperationOptions {
		private File localDir;
		private ConfigTO configTO;
		
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
	}
		 
    public static class ConnectOperationResult implements OperationResult {
        // Nothing		
    }
}
