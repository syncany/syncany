package org.syncany.operations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionIdentifier;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;

public class SyncDownOperation extends Operation {
	private static final Logger logger = Logger.getLogger(SyncDownOperation.class.getSimpleName());
	
	public SyncDownOperation(Config profile) {
		super(profile);
	}	
	
	public void execute() throws Exception {
		logger.log(Level.INFO, "Sync down  ...");
		
		File localDatabaseFile = new File(profile.getAppDatabaseDir()+"/local.db");		
		Database db = loadLocalDatabase(localDatabaseFile);
		
		// 0. Create TM
		TransferManager transferManager = profile.getConnection().createTransferManager();

		// 1. check which remote databases to download based on the last local vector clock		
		List<RemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(db, transferManager);
		
		// 2. download the remote databases to the local cache folder
		List<File> unknownRemoteDatabasesInCache = downloadUnknownRemoteDatabases(transferManager, unknownRemoteDatabases);
		
		Map<String, List<DatabaseVersionIdentifier>> unknownDatabaseVersions = readDatabaseVersionIdentifierPerMachine(unknownRemoteDatabasesInCache);
		
		detectUpdates(unknownDatabaseVersions);
		// 3. read the remote databases
		// 4. compare the remote databases based on the file histories contained in them and figure out the winning file histories
		detectUpdates(db, unknownRemoteDatabasesInCache);
		
		// 5. figure out which 
		// 2. xxx
		//
		//db.getLastDatabaseVersion().getVectorClock();
		
		throw new Exception("Not yet fully implemented.");
		//return false;
	}	

	private void detectUpdates(Map<String, List<DatabaseVersionIdentifier>> unknownDatabaseVersions) {
		// 1. collect conflict-free dbvs
		// 2. collect conflicts
		// 3. gather winner
		// 4. collect winner
		
	}

	private Map<String, List<DatabaseVersionIdentifier>> readDatabaseVersionIdentifierPerMachine(List<File> remoteDatabases) throws IOException {
		Map<String, List<DatabaseVersionIdentifier>> databaseVersionIdentifiers = new HashMap<String,List<DatabaseVersionIdentifier>>();
		
		Database remoteDatabase = new Database();
		DatabaseDAO dbDAO = new DatabaseDAO();
		
		for (File remoteDatabaseInCache : remoteDatabases) {
			dbDAO.load(remoteDatabase, remoteDatabaseInCache);

			RemoteDatabaseFile remoteDatabaseFile = new RemoteDatabaseFile(remoteDatabaseInCache.getName());
			
			String clientName = remoteDatabaseFile.getClientName();
			
			List<DatabaseVersionIdentifier> clientDatabaseIdentifiers = databaseVersionIdentifiers.get(clientName);
			
			if (clientDatabaseIdentifiers == null) {
				clientDatabaseIdentifiers = new ArrayList<DatabaseVersionIdentifier>();
				databaseVersionIdentifiers.put(clientName, clientDatabaseIdentifiers);
			} 
			
			Map<Long,DatabaseVersion> remoteDatabaseVersions = remoteDatabase.getDatabaseVersions();			
			
			for (DatabaseVersion remoteDatabaseVersion : remoteDatabaseVersions.values()) {
				DatabaseVersionIdentifier id = remoteDatabaseVersion.getId();
				clientDatabaseIdentifiers.add(id);
			}
		}
		
		return databaseVersionIdentifiers;
	}

	private List<RemoteFile> listUnknownRemoteDatabases(Database db, TransferManager transferManager) throws StorageException {
		logger.log(Level.INFO, "Retrieving remote database list.");
		
		List<RemoteFile> unknownRemoteDatabasesList = new ArrayList<RemoteFile>();

		Map<String, RemoteFile> remoteDatabaseFiles = transferManager.list("db-");
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
	
	private List<File> downloadUnknownRemoteDatabases(TransferManager transferManager, List<RemoteFile> unknownRemoteDatabases) throws StorageException {
		logger.log(Level.INFO, "Downloading unknown databases.");
		List<File> unknownRemoteDatabasesInCache = new ArrayList<File>();
		
		for (RemoteFile remoteFile : unknownRemoteDatabases) {
			File unknownRemoteDatabaseFileInCache = profile.getCache().getDatabaseFile(remoteFile.getName());

			logger.log(Level.INFO, "- Downloading {0} to local cache at {1}", new Object[] { remoteFile.getName(), unknownRemoteDatabaseFileInCache });
			transferManager.download(remoteFile, unknownRemoteDatabaseFileInCache);
			
			unknownRemoteDatabasesInCache.add(unknownRemoteDatabaseFileInCache);
		}
		
		return unknownRemoteDatabasesInCache;
	}		

	private void detectUpdates(Database db, List<File> remoteDatabasesInCache) throws Exception {
		Database newLocalDatabase = db; // TODO shouldn't we clone this in case this goes wrong?
		VectorClock localVectorClock = newLocalDatabase.getLastDatabaseVersion().getVectorClock();

		logger.log(Level.INFO, "Reconciling local database with remote databases ...");
		logger.log(Level.INFO, "- Local database version: {0}", localVectorClock.toString());
		
		VectorClock latestRemoteVectorClock = null;
		File latestRemoteDatabase = null;
		List<File> conflictRemoteDatabases = new ArrayList<File>(); 
		
		for (File remoteDatabaseInCache : remoteDatabasesInCache) {
			logger.log(Level.INFO, "- Processing remote database. Reading from {0} ...", remoteDatabaseInCache);
			
			Database remoteDatabase = new Database();
			DatabaseDAO dbDAO = new DatabaseDAO();
			
			dbDAO.load(remoteDatabase, remoteDatabaseInCache);
			
			VectorClock remoteVectorClock = remoteDatabase.getLastDatabaseVersion().getVectorClock();
			VectorClockComparison localDatabaseIs = VectorClock.compare(localVectorClock, remoteVectorClock);
									
			logger.log(Level.INFO, "  + Success. Remote database version: {0}", remoteVectorClock.toString());

			if (localDatabaseIs == VectorClockComparison.EQUAL) {
				logger.log(Level.INFO, "  + Database versions are equal. Nothing to do.");
			}
			else if (localDatabaseIs == VectorClockComparison.GREATER) {
				logger.log(Level.INFO, "  + Local database is greater. Nothing to do.");
			}
			else if (localDatabaseIs == VectorClockComparison.SMALLER) {
				logger.log(Level.INFO, "  + Local database is SMALLER. Local update needed!");
				
				if (latestRemoteVectorClock != null) {
					VectorClockComparison latestRemoteDatabaseIs = VectorClock.compare(latestRemoteVectorClock, remoteVectorClock);
					
					if (latestRemoteDatabaseIs == VectorClockComparison.SMALLER) {
						latestRemoteDatabase = remoteDatabaseInCache;
						latestRemoteVectorClock = remoteVectorClock;
					}
				}
				//updateLocalDatabase
			}
			else if (localDatabaseIs == VectorClockComparison.SIMULTANEOUS) {
				logger.log(Level.INFO, "  + Databases are SIMULATANEOUS. Reconciliation needed!");
			}
		}
		
		throw new Exception("This is nowhere near done.");
	}	

}
