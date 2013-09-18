package org.syncany.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Database;
import org.syncany.database.RemoteDatabaseFile;
import org.syncany.database.VectorClock;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;

public class RemoteStatusOperation extends Operation {
	private static final Logger logger = Logger.getLogger(RemoteStatusOperation.class.getSimpleName());	
	private Database loadedDatabase;
	private TransferManager loadedTransferManager;
	
	public RemoteStatusOperation(Config config) {
		this(config, null, null);
	}	
	
	public RemoteStatusOperation(Config config, Database database, TransferManager transferManager) {
		super(config);		
		
		this.loadedDatabase = database;
		this.loadedTransferManager = transferManager;
	}	
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Remote Status' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		Database database = (loadedDatabase != null) 
				? loadedDatabase
				: ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();		
		
		TransferManager transferManager = (loadedTransferManager != null)
				? loadedTransferManager
				: config.getConnection().createTransferManager();
		
		List<RemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(database, transferManager);		
		return new RemoteStatusOperationResult(unknownRemoteDatabases);
	}		
	
	private List<RemoteFile> listUnknownRemoteDatabases(Database db, TransferManager transferManager) throws StorageException {
		logger.log(Level.INFO, "Retrieving remote database list.");
		
		List<RemoteFile> unknownRemoteDatabasesList = new ArrayList<RemoteFile>();

		Map<String, RemoteFile> remoteDatabaseFiles = transferManager.list("db-");
		
		// No local database yet
		if (db.getLastDatabaseVersion() == null) {
			return new ArrayList<RemoteFile>(remoteDatabaseFiles.values());
		}
		
		// At least one local database version exists
		else {
			VectorClock knownDatabaseVersions = db.getLastDatabaseVersion().getVectorClock();
			
			for (RemoteFile remoteFile : remoteDatabaseFiles.values()) {
				RemoteDatabaseFile remoteDatabaseFile = new RemoteDatabaseFile(remoteFile.getName());
				
				String clientName = remoteDatabaseFile.getClientName();
				Long knownClientVersion = knownDatabaseVersions.get(clientName);
						
				if (knownClientVersion != null) {
					if (remoteDatabaseFile.getClientVersion() > knownClientVersion) {
						logger.log(Level.INFO, "- Remote database {0} is new.", remoteFile.getName());
						unknownRemoteDatabasesList.add(remoteFile);
					}
					else {
						logger.log(Level.INFO, "- Remote database {0} is already known. Ignoring.", remoteFile.getName());
						// Do nothing. We know this database.
					}
				}
				
				else {
					logger.log(Level.INFO, "- Remote database {0} is new.", remoteFile.getName());
					unknownRemoteDatabasesList.add(remoteFile);
				}				
			}
			
			return unknownRemoteDatabasesList;			
		}
	}
	
	public class RemoteStatusOperationResult implements OperationResult {
		private List<RemoteFile> unknownRemoteDatabases;
		
		public RemoteStatusOperationResult(List<RemoteFile> unknownRemoteDatabases) {
			this.unknownRemoteDatabases = unknownRemoteDatabases;
		}

		public List<RemoteFile> getUnknownRemoteDatabases() {
			return unknownRemoteDatabases;
		}
	}
}
