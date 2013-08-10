package org.syncany.operations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Branch;
import org.syncany.database.Branches;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.DatabaseXmlDAO;
import org.syncany.database.VectorClock;
import org.syncany.database.VectorClock.VectorClockComparison;

public class SyncDownOperation extends Operation {
	private static final Logger logger = Logger.getLogger(SyncDownOperation.class.getSimpleName());
	
	public SyncDownOperation(Config config) {
		super(config);
	}	
	
	public void execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync down' at client "+profile.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");		
		
		File localDatabaseFile = new File(profile.getAppDatabaseDir()+"/local.db");		
		Database db = loadLocalDatabase(localDatabaseFile);
		
		// 0. Create TM
		TransferManager transferManager = profile.getConnection().createTransferManager();

		// 1. Check which remote databases to download based on the last local vector clock		
		List<RemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(db, transferManager);
		
		// 2. Download the remote databases to the local cache folder
		List<File> unknownRemoteDatabasesInCache = downloadUnknownRemoteDatabases(transferManager, unknownRemoteDatabases);
		
		// 3. Read version headers (vector clocks)
		Branches unknownRemoteBranches = readUnknownDatabaseVersionHeaders(unknownRemoteDatabasesInCache);
		
		// 4. Determine winner branch
		logger.log(Level.INFO, "Detect updates and conflicts ...");
		logger.log(Level.INFO, "- DatabaseVersionUpdateDetector results:");
		DatabaseVersionUpdateDetector databaseVersionUpdateDetector = new DatabaseVersionUpdateDetector();
		
		Branch localBranch = db.getBranch();		
		Branches fullRemoteBranches = databaseVersionUpdateDetector.fillRemoteBranches(localBranch, unknownRemoteBranches);
		
		Branches allBranches = fullRemoteBranches.clone();
		allBranches.add(profile.getMachineName(), localBranch);
		
		DatabaseVersionHeader lastCommonHeader = databaseVersionUpdateDetector.findLastCommonDatabaseVersionHeader(localBranch, fullRemoteBranches);		
		TreeMap<String, DatabaseVersionHeader> firstConflictingHeaders = databaseVersionUpdateDetector.findFirstConflictingDatabaseVersionHeader(lastCommonHeader, allBranches);		
		TreeMap<String, DatabaseVersionHeader> winningFirstConflictingHeaders = databaseVersionUpdateDetector.findWinningFirstConflictingDatabaseVersionHeaders(firstConflictingHeaders);		
		Entry<String, DatabaseVersionHeader> winnersWinnersLastDatabaseVersionHeader = databaseVersionUpdateDetector.findWinnersWinnersLastDatabaseVersionHeader(winningFirstConflictingHeaders, allBranches);
		
		logger.log(Level.FINER, "   + localBranch: "+localBranch);
		logger.log(Level.FINER, "   + fullRemoteBranches: "+fullRemoteBranches);
		logger.log(Level.FINER, "   + allBranches: "+allBranches);
		logger.log(Level.FINER, "   + lastCommonHeader: "+lastCommonHeader);
		logger.log(Level.FINER, "   + firstConflictingHeaders: "+firstConflictingHeaders);
		logger.log(Level.FINER, "   + winningFirstConflictingHeaders: "+winningFirstConflictingHeaders);
		logger.log(Level.FINER, "   + winnersWinnersLastDatabaseVersionHeader: "+winnersWinnersLastDatabaseVersionHeader);

		String winnersName = winnersWinnersLastDatabaseVersionHeader.getKey();
		Branch winnersBranch = allBranches.getBranch(winnersName);
		
		logger.log(Level.INFO, "- Compared branches: "+allBranches);
		logger.log(Level.INFO, "- Winner is "+winnersName+" with branch "+winnersBranch);
				
		if (profile.getMachineName().equals(winnersName)) {
			if (winnersBranch.size() > localBranch.size()) {
				throw new RuntimeException("TODO implement use case 'restore remote backup'");
			}
			
			logger.log(Level.INFO, "- I won, noting to do locally");
		}
		else {
			logger.log(Level.INFO, "- Someone else won, now determine what to do ...");
			
			Branch localPruneBranch = databaseVersionUpdateDetector.findLosersPruneBranch(localBranch, winnersBranch);
			logger.log(Level.INFO, "- Database versions to REMOVE locally: "+localPruneBranch);
			
			if (localPruneBranch.size() == 0) {
				logger.log(Level.INFO, "  + Nothing to prune locally. No conflicts. Only updates. Nice!");
			}
			
			Branch winnersApplyBranch = databaseVersionUpdateDetector.findWinnersApplyBranch(localBranch, winnersBranch);
			logger.log(Level.INFO, "- Database versions to APPLY locally: "+winnersApplyBranch);
			
			if (winnersApplyBranch.size() == 0) {
				logger.log(Level.WARNING, "   ++++ NOTHING TO UPDATE FROM WINNER. This should not happen.");
			}
			else {
				logger.log(Level.INFO, "- Loading winners database ...");
				Database winnersDatabase = readClientDatabase(winnersName, unknownRemoteDatabasesInCache);
				
				for (DatabaseVersionHeader applyDatabaseVersionHeader : winnersApplyBranch.getAll()) {
					logger.log(Level.INFO, "   + Applying database version "+applyDatabaseVersionHeader.getVectorClock());
					
					DatabaseVersion applyDatabaseVersion = winnersDatabase.getDatabaseVersions(applyDatabaseVersionHeader.getVectorClock());									
					db.addDatabaseVersion(applyDatabaseVersion);
					
					// TODO Do something with this dbv
				}
				
				// TODO Do something on the file system!
				logger.log(Level.INFO, "- Saving local database tp "+localDatabaseFile+" ...");
				saveLocalDatabase(db, localDatabaseFile);
			}
			
		}		
		
		//throw new Exception("Not yet fully implemented.");
	}	


	private Database readClientDatabase(String clientName, List<File> remoteDatabases) throws IOException {
		// Sort files (db-a-1 must be before db-a-2 !)
		Collections.sort(remoteDatabases);
		
		DatabaseDAO databaseDAO = new DatabaseXmlDAO();
		Database clientDatabase = new Database(); // Database cannot be reused, since these might be different clients
		
		for (File remoteDatabaseInCache : remoteDatabases) {
			RemoteDatabaseFile rdf = new RemoteDatabaseFile(remoteDatabaseInCache);
			
			if (clientName.equals(rdf.getClientName())) {
				databaseDAO.load(clientDatabase, rdf);	
			}
		}
		
		return clientDatabase;
	}

	private Branches readUnknownDatabaseVersionHeaders(List<File> remoteDatabases) throws IOException {
		// Sort files (db-a-1 must be before db-a-2 !)
		Collections.sort(remoteDatabases);
		
		// Read database files
		Branches unknownRemoteBranches = new Branches();
		DatabaseDAO dbDAO = new DatabaseXmlDAO();
		
		for (File remoteDatabaseInCache : remoteDatabases) {
			Database remoteDatabase = new Database(); // Database cannot be reused, since these might be different clients
			
			RemoteDatabaseFile rdf = new RemoteDatabaseFile(remoteDatabaseInCache);
			dbDAO.load(remoteDatabase, rdf);		// FIXME This is very, very, very inefficient, DB is loaded and then discarded	
			List<DatabaseVersion> remoteDatabaseVersions = remoteDatabase.getDatabaseVersions();			
			
			// Pupulate branches
			Branch remoteClientBranch = unknownRemoteBranches.getBranch(rdf.getClientName(), true);
			
			for (DatabaseVersion remoteDatabaseVersion : remoteDatabaseVersions) {
				DatabaseVersionHeader header = remoteDatabaseVersion.getHeader();
				remoteClientBranch.add(header);
			}
		}
		
		return unknownRemoteBranches;
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
			DatabaseDAO dbDAO = new DatabaseXmlDAO();
			
			RemoteDatabaseFile rdf = new RemoteDatabaseFile(remoteDatabaseInCache);
			dbDAO.load(remoteDatabase, rdf);		
			
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
