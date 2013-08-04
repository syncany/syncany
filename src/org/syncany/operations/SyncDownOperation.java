package org.syncany.operations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
	
	public SyncDownOperation(Config profile) {
		super(profile);
	}	
	
	public void execute() throws Exception {
		logger.log(Level.INFO, "Sync down  ...");
		
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
		

		DatabaseVersionUpdateDetector databaseVersionUpdateDetector = new DatabaseVersionUpdateDetector();
		
		Branch localBranch = db.getBranch();		
		Branches fullRemoteBranches = databaseVersionUpdateDetector.fillRemoteBranches(localBranch, unknownRemoteBranches);
		Branches allBranches = fullRemoteBranches.clone();
		allBranches.add(profile.getMachineName(), localBranch);
		
		DatabaseVersionHeader lastCommonHeader = databaseVersionUpdateDetector.findLastCommonDatabaseVersionHeader(localBranch, fullRemoteBranches);
		TreeMap<String, DatabaseVersionHeader> firstConflictingHeaders = databaseVersionUpdateDetector.findFirstConflictingDatabaseVersionHeader(lastCommonHeader, profile.getMachineName(), localBranch, fullRemoteBranches);
		TreeMap<String, DatabaseVersionHeader> winningFirstConflictingHeaders = databaseVersionUpdateDetector.findWinningFirstConflictingDatabaseVersionHeaders(firstConflictingHeaders);
		Entry<String, DatabaseVersionHeader> winnersWinnersLastDatabaseVersionHeader = databaseVersionUpdateDetector.findWinnersWinnersLastDatabaseVersionHeader(winningFirstConflictingHeaders, allBranches);
		
		String winnersName = winnersWinnersLastDatabaseVersionHeader.getKey();
		Branch winnersBranch = allBranches.getBranch(winnersName);
		
		logger.log(Level.INFO, "Sync down compared: "+allBranches);
		logger.log(Level.INFO, "Sync down winner is "+winnersName+" with: "+winnersBranch);
		// TODO implement this: compare DatabaseVersionUpdateDetectorTest
		
		
		//detectUpdates(unknownDatabaseVersionHeaders);
		// 3. read the remote databases
		// 4. compare the remote databases based on the file histories contained in them and figure out the winning file histories
		//detectUpdates(db, unknownRemoteDatabasesInCache);
		
		// 5. figure out which 
		// 2. xxx
		//
		//db.getLastDatabaseVersion().getVectorClock();
		
		//throw new Exception("Not yet fully implemented.");
		//return false;
	}	

	private void detectUpdates(List<DatabaseVersionHeader> unknownDatabaseVersionHeaders) {
		// 0. Create ascending order, 
		List<DatabaseVersionHeader> sortedUnknownDatabaseVersionHeaders = new ArrayList<DatabaseVersionHeader>(unknownDatabaseVersionHeaders);		
		Collections.sort(sortedUnknownDatabaseVersionHeaders, new DatabaseVersionHeaderComparator());
		
		// 1. Get all conflicts
		List<DatabaseVersionHeaderPair> conflicts = new ArrayList<DatabaseVersionHeaderPair>();
		Set<DatabaseVersionHeader> conflictHeaders = new HashSet<DatabaseVersionHeader>();
		
		for (int i=0; i<unknownDatabaseVersionHeaders.size(); i++) { // compare all clocks to each other
			for (int j=i+1; j<unknownDatabaseVersionHeaders.size(); j++) {
				if (j != i) {
					DatabaseVersionHeader header1 = unknownDatabaseVersionHeaders.get(i);
					DatabaseVersionHeader header2 = unknownDatabaseVersionHeaders.get(j);
					
					VectorClockComparison vectorClockComparison = VectorClock.compare(header1.getVectorClock(), header2.getVectorClock());
					
					if (vectorClockComparison == VectorClockComparison.SIMULTANEOUS) {
						conflictHeaders.add(header1);
						conflictHeaders.add(header2);						
						conflicts.add(new DatabaseVersionHeaderPair(header1, header2));
					}
				}
			}
		}		
		
		// 2. Determine the first conflict-free database version headers (1..n)
		List<DatabaseVersionHeader> firstUnconflictingUnknownDatabaseVersionHeaders = new ArrayList<DatabaseVersionHeader>();
		
		DatabaseVersionHeader thisHeader = null;
		DatabaseVersionHeader lastHeader = null;
		
		for (int i=0; i < sortedUnknownDatabaseVersionHeaders.size(); i++) {
			thisHeader = sortedUnknownDatabaseVersionHeaders.get(i);
			
			if (conflictHeaders.contains(thisHeader)) {	// stop when first conflict found			 
				break; // TODO (A, see below!)
			}
			
			if (lastHeader != null && lastHeader.equals(thisHeader)) { // ignore duplicates
				continue;
			}
			
			firstUnconflictingUnknownDatabaseVersionHeaders.add(thisHeader);			
			lastHeader = thisHeader;
		}
		
		
		
		// TODO Use the the databaseVersionUpdateDetector
		// TODO 
		// TODO !
		
		
		// 2. collect conflicts
		// 3. gather winner
		// 4. collect winner
		
	}
	
	private class DatabaseVersionHeaderComparator implements Comparator<DatabaseVersionHeader> {

		@Override
		public int compare(DatabaseVersionHeader header1, DatabaseVersionHeader header2) {
			VectorClockComparison vectorClockComparison = VectorClock.compare(header1.getVectorClock(), header2.getVectorClock());
			
			if (vectorClockComparison == VectorClockComparison.GREATER) {
				return 1;
			}
			else if (vectorClockComparison == VectorClockComparison.SMALLER) {
				return -1;
			}
			else {
				return 0;						
			}
		}
		
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
