package org.syncany;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.operations.DaemonOperation.DaemonOperationOptions;
import org.syncany.operations.InitOperation;
import org.syncany.operations.InitOperation.InitOperationOptions;
import org.syncany.operations.InitOperation.InitOperationResult;
import org.syncany.operations.RemoteStatusOperation;
import org.syncany.operations.RemoteStatusOperation.RemoteStatusOperationResult;
import org.syncany.operations.RestoreOperation;
import org.syncany.operations.RestoreOperation.RestoreOperationOptions;
import org.syncany.operations.StatusOperation;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.DaemonOperation;
import org.syncany.operations.SyncDownOperation;
import org.syncany.operations.SyncDownOperation.SyncDownOperationOptions;
import org.syncany.operations.SyncDownOperation.SyncDownOperationResult;
import org.syncany.operations.SyncOperation;
import org.syncany.operations.SyncOperation.SyncOperationOptions;
import org.syncany.operations.SyncOperation.SyncOperationResult;
import org.syncany.operations.SyncUpOperation;
import org.syncany.operations.SyncUpOperation.SyncUpOperationOptions;
import org.syncany.operations.SyncUpOperation.SyncUpOperationResult;

public class Client {
	protected static final Logger logger = Logger.getLogger(Client.class.getSimpleName());	
	protected Config config;
	
	public Client() {
		// Fressen
	}
	
	public void setConfig(Config config) {
		this.config = config;
	}
	
	public Config getConfig() {
		return config;
	}

	public void createDirectories() throws Exception {   
		logger.log(Level.INFO, "Creating profile directories ...");
		
		config.getCacheDir().mkdirs();
		config.getDatabaseDir().mkdirs();
		config.getLogDir().mkdirs();
	}		
	
	public SyncUpOperationResult up() throws Exception {
		return up(new SyncUpOperationOptions());
	}
	
	public SyncUpOperationResult up(SyncUpOperationOptions options) throws Exception {
		return (SyncUpOperationResult) new SyncUpOperation(config, null, options).execute();
	}
	
	public SyncDownOperationResult down() throws Exception {
		return down(new SyncDownOperationOptions());
	}
	
	public SyncDownOperationResult down(SyncDownOperationOptions options) throws Exception {
		return (SyncDownOperationResult) new SyncDownOperation(config, null, options).execute();
	}
	
	public SyncOperationResult sync() throws Exception {
		return sync(new SyncOperationOptions());
	}
	
	public SyncOperationResult sync(SyncOperationOptions options) throws Exception {
		return (SyncOperationResult) new SyncOperation(config, null, options).execute();
	}

	public ChangeSet status() throws Exception {
		return status(new StatusOperationOptions());		
	}
	
	public ChangeSet status(StatusOperationOptions options) throws Exception {
		return ((StatusOperationResult) new StatusOperation(config, null, options).execute()).getChangeSet();		
	}	

	public List<RemoteFile> remoteStatus() throws Exception {
		return ((RemoteStatusOperationResult) new RemoteStatusOperation(config).execute()).getUnknownRemoteDatabases();
	}

	public void restore(RestoreOperationOptions options) throws Exception {
		new RestoreOperation(config, options).execute();		
	}

	public void daemon(DaemonOperationOptions options) throws Exception {
		new DaemonOperation(config, options).execute();		
	}	

	public File init(InitOperationOptions options) throws Exception {
		return ((InitOperationResult) new InitOperation(options).execute()).getSkelConfigFile();		
	}
}
