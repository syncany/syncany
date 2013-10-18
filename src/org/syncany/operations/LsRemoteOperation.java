package org.syncany.operations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

public class LsRemoteOperation extends Operation {
	private static final Logger logger = Logger.getLogger(LsRemoteOperation.class.getSimpleName());	
	private Database loadedDatabase;
	private TransferManager loadedTransferManager;
	private Set<String> alreadyDownloadedRemoteDatabases;
	
	public LsRemoteOperation(Config config) {
		this(config, null, null);
	}	
	
	public LsRemoteOperation(Config config, Database database, TransferManager transferManager) {
		super(config);		
		
		this.loadedDatabase = database;
		this.loadedTransferManager = transferManager;
		this.alreadyDownloadedRemoteDatabases = new HashSet<String>();
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
		
		alreadyDownloadedRemoteDatabases = readAlreadyDownloadedDatabasesListFromFile();
		List<RemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(database, transferManager, alreadyDownloadedRemoteDatabases);
		
		
		return new RemoteStatusOperationResult(unknownRemoteDatabases);
	}		

	private Set<String> readAlreadyDownloadedDatabasesListFromFile() throws IOException {
		// TODO [low] This is dirty!
		alreadyDownloadedRemoteDatabases.clear();
		
		if (config.getKnownDatabaseListFile().exists()) {
			BufferedReader br = new BufferedReader(new FileReader(config.getKnownDatabaseListFile()));
			
			String line = null;
			while (null != (line = br.readLine())) {
				alreadyDownloadedRemoteDatabases.add(line);
			}
		}		
		
		return alreadyDownloadedRemoteDatabases;
	}

	private List<RemoteFile> listUnknownRemoteDatabases(Database db, TransferManager transferManager, Set<String> alreadyDownloadedRemoteDatabases2) throws StorageException {
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
					if (remoteDatabaseFile.getClientVersion() <= knownClientVersion) {
						logger.log(Level.INFO, "- Remote database {0} is already known. Ignoring.", remoteFile.getName());
					}
					else if (alreadyDownloadedRemoteDatabases.contains(remoteFile.getName())) {
						logger.log(Level.INFO, "- Remote database {0} is already known (in knowndbs.list). Ignoring.", remoteFile.getName());
					}
					else {
						logger.log(Level.INFO, "- Remote database {0} is new.", remoteFile.getName());
						unknownRemoteDatabasesList.add(remoteFile);
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
