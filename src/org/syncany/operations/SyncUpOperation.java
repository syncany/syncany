package org.syncany.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseXmlDAO;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.VectorClock;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.util.StringUtil;

public class SyncUpOperation extends Operation {
	private static final Logger logger = Logger.getLogger(SyncUpOperation.class.getSimpleName());

	public static final int MIN_KEEP_DATABASE_VERSIONS = 5;
	public static final int MAX_KEEP_DATABASE_VERSIONS = 15;
	
	private TransferManager transferManager; 
	
	public SyncUpOperation(Config config) {
		super(config);		
		this.transferManager = config.getConnection().createTransferManager();
	}	
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync up' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		// logger.log(Level.INFO, "Loading local database ...");		
		// Database db = loadLocalDatabase(config.getDatabaseFile());

		// logger.log(Level.INFO, "Starting index process ...");
		// List<File> localFiles = FileUtil.getRecursiveFileList(config.getLocalDir(), true);
		
		// Load database
		Database database = ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();
		
		// Find local changes
		ChangeSet changeSet = ((StatusOperationResult) new StatusOperation(config, database).execute()).getChangeSet();

		if (!changeSet.hasChanges()) {
			logger.log(Level.INFO, "Local database is up-to-date (change set). NOTHING TO DO!");
			return new SyncUpOperationResult();
		}
		
		List<File> locallyUpdatedFiles = new ArrayList<File>();
		locallyUpdatedFiles.addAll(changeSet.getNewFiles());
		locallyUpdatedFiles.addAll(changeSet.getChangedFiles());
		
		// Index
		DatabaseVersion lastDirtyDatabaseVersion = index(locallyUpdatedFiles, database);
		
		if (lastDirtyDatabaseVersion.getFileHistories().size() == 0) {
			logger.log(Level.INFO, "Local database is up-to-date. NOTHING TO DO!");
		}
		else {
			logger.log(Level.INFO, "Adding newest database version "+lastDirtyDatabaseVersion.getHeader()+" to local database ...");
			database.addDatabaseVersion(lastDirtyDatabaseVersion);
	
			logger.log(Level.INFO, "Saving local database to file "+config.getDatabaseFile()+" ...");
			saveLocalDatabase(database, config.getDatabaseFile());
			
			logger.log(Level.INFO, "Uploading new multichunks ...");
			uploadMultiChunks(database.getLastDatabaseVersion().getMultiChunks());
			
			long newestLocalDatabaseVersion = lastDirtyDatabaseVersion.getVectorClock().get(config.getMachineName());

			RemoteFile remoteDeltaDatabaseFile = new RemoteFile("db-"+config.getMachineName()+"-"+newestLocalDatabaseVersion);
			File localDeltaDatabaseFile = config.getCache().getDatabaseFile(remoteDeltaDatabaseFile.getName());	

			logger.log(Level.INFO, "Saving local delta database file ...");
			logger.log(Level.INFO, "- Saving versions from: "+lastDirtyDatabaseVersion.getHeader()+", to: "+lastDirtyDatabaseVersion.getHeader()+") to file "+localDeltaDatabaseFile+" ...");
			saveLocalDatabase(database, lastDirtyDatabaseVersion, lastDirtyDatabaseVersion, localDeltaDatabaseFile);
			
			logger.log(Level.INFO, "- Uploading local delta database file ...");
			uploadLocalDatabase(localDeltaDatabaseFile, remoteDeltaDatabaseFile);
			
			cleanupOldDatabases(database, newestLocalDatabaseVersion);
			
			logger.log(Level.INFO, "Sync up done.");
		}
		
		return new SyncUpOperationResult();
	}	
	
	private void uploadMultiChunks(Collection<MultiChunkEntry> multiChunksEntries) throws InterruptedException, StorageException {
		for (MultiChunkEntry multiChunkEntry : multiChunksEntries) {
			File localMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId());
			RemoteFile remoteMultiChunkFile = new RemoteFile(localMultiChunkFile.getName());
			
			logger.log(Level.INFO, "- Uploading multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" from "+localMultiChunkFile+" to "+remoteMultiChunkFile+" ...");
			transferManager.upload(localMultiChunkFile, remoteMultiChunkFile);
			
			logger.log(Level.INFO, "  + Removing "+StringUtil.toHex(multiChunkEntry.getId())+" locally ...");
			localMultiChunkFile.delete();
		}		
	}

	private void uploadLocalDatabase(File localDatabaseFile, RemoteFile remoteDatabaseFile) throws InterruptedException, StorageException {		
		logger.log(Level.INFO, "- Uploading "+localDatabaseFile+" to "+remoteDatabaseFile+" ..."); 		
		transferManager.upload(localDatabaseFile, remoteDatabaseFile);
	}

	private DatabaseVersion index(List<File> localFiles, Database db) throws FileNotFoundException, IOException {			
		// Get last vector clock
		String previousClient = null;
		VectorClock lastVectorClock = null;
		
		if (db.getLastDatabaseVersion() != null) {
			previousClient = db.getLastDatabaseVersion().getClient();
			lastVectorClock = db.getLastDatabaseVersion().getVectorClock();
		}
		else {
			previousClient = null;
			lastVectorClock = new VectorClock();
		}
		
		// New vector clock
		VectorClock newVectorClock = lastVectorClock.clone();

		Long lastLocalValue = lastVectorClock.getClock(config.getMachineName());
		Long newLocalValue = (lastLocalValue == null) ? 1 : lastLocalValue+1;
		
		newVectorClock.setClock(config.getMachineName(), newLocalValue);		

		// Index
		Deduper deduper = new Deduper(config.getChunker(), config.getMultiChunker(), config.getTransformer());
		Indexer indexer = new Indexer(config, deduper, db);
		
		DatabaseVersion newDatabaseVersion = indexer.index(localFiles);	
	
		newDatabaseVersion.setVectorClock(newVectorClock);
		newDatabaseVersion.setTimestamp(new Date());	
		newDatabaseVersion.setClient(config.getMachineName());
		newDatabaseVersion.setPreviousClient(previousClient);
						
		return newDatabaseVersion;
	}
	
	private void cleanupOldDatabases(Database database, long newestLocalDatabaseVersion) throws Exception {
		// Retrieve and sort machine's database versions
		Map<String, RemoteFile> ownRemoteDatabaseFiles = transferManager.list("db-"+config.getMachineName()+"-");
		List<RemoteDatabaseFile> ownDatabaseFiles = new ArrayList<RemoteDatabaseFile>();	
		
		for (RemoteFile ownRemoteDatabaseFile : ownRemoteDatabaseFiles.values()) {
			ownDatabaseFiles.add(new RemoteDatabaseFile(ownRemoteDatabaseFile.getName()));
		}
		
		Collections.sort(ownDatabaseFiles, new RemoteDatabaseFileComparator());
	
		// Now merge
		if (ownDatabaseFiles.size() <= MAX_KEEP_DATABASE_VERSIONS) {
			logger.log(Level.INFO, "- No cleanup necessary ("+ownDatabaseFiles.size()+" database files, max. "+MAX_KEEP_DATABASE_VERSIONS+")");
			return;
		}
		
		logger.log(Level.INFO, "- Performing cleanup ("+ownDatabaseFiles.size()+" database files, max. "+MAX_KEEP_DATABASE_VERSIONS+") ...");
		
		RemoteDatabaseFile firstMergeDatabaseFile = ownDatabaseFiles.get(0);
		RemoteDatabaseFile lastMergeDatabaseFile = ownDatabaseFiles.get(ownDatabaseFiles.size()-MIN_KEEP_DATABASE_VERSIONS-1);
		
		DatabaseVersion firstMergeDatabaseVersion = null;
		DatabaseVersion lastMergeDatabaseVersion = null;
		
		List<RemoteFile> toDeleteDatabaseFiles = new ArrayList<RemoteFile>();
		
		for (DatabaseVersion databaseVersion : database.getDatabaseVersions()) {
			Long localVersion = databaseVersion.getVectorClock().get(config.getMachineName());

			if (localVersion != null) {				
				if (firstMergeDatabaseVersion == null) {
					firstMergeDatabaseVersion = databaseVersion;
				}
			
				if (lastMergeDatabaseVersion == null && localVersion == lastMergeDatabaseFile.getClientVersion()) {
					lastMergeDatabaseVersion = databaseVersion;
					break;
				}
				
				if (localVersion < lastMergeDatabaseFile.getClientVersion()) {
					toDeleteDatabaseFiles.add(new RemoteFile("db-"+config.getMachineName()+"-"+localVersion));
				}
			}
		}
		
		if (firstMergeDatabaseVersion == null || lastMergeDatabaseVersion == null) {
			throw new Exception("Cannot cleanup: unable to find first/last database version: first = "+firstMergeDatabaseFile+"/"+firstMergeDatabaseVersion+", last = "+lastMergeDatabaseFile+"/"+lastMergeDatabaseVersion);
		}
		
		// Now write merge file
		File localMergeDatabaseVersionFile = config.getCache().getDatabaseFile("db-"+config.getMachineName()+"-"+lastMergeDatabaseFile.getClientVersion());
		RemoteFile remoteMergeDatabaseVersionFile = new RemoteFile(localMergeDatabaseVersionFile.getName());
		
		logger.log(Level.INFO, "   + Writing new merge file (from "+firstMergeDatabaseVersion.getHeader()+", to "+lastMergeDatabaseVersion.getHeader()+") to file "+localMergeDatabaseVersionFile+" ...");

		DatabaseDAO databaseDAO = new DatabaseXmlDAO(); 			
		databaseDAO.save(database, null/*firstMergeDatabaseVersion*/, lastMergeDatabaseVersion, localMergeDatabaseVersionFile);
		
		logger.log(Level.INFO, "   + Uploading new file "+remoteMergeDatabaseVersionFile+" from local file "+localMergeDatabaseVersionFile+" ...");
		transferManager.delete(remoteMergeDatabaseVersionFile); // TODO [high] TM cannot overwrite, might lead to chaos if operation does not finish uploading the new merge file, this might happen often if new file is bigger!
		transferManager.upload(localMergeDatabaseVersionFile, remoteMergeDatabaseVersionFile);

		// And delete others	
		for (RemoteFile toDeleteRemoteFile : toDeleteDatabaseFiles) {
			logger.log(Level.INFO, "   + Deleting remote file "+toDeleteRemoteFile+" ...");
			transferManager.delete(toDeleteRemoteFile);
		}
	}

	// TODO [medium] Duplicate code in SyncDownOperation
	public static class RemoteDatabaseFileComparator implements Comparator<RemoteDatabaseFile> {
		@Override
		public int compare(RemoteDatabaseFile r1, RemoteDatabaseFile r2) {
			int clientNameCompare = r1.getClientName().compareTo(r2.getClientName());
			
			if (clientNameCompare != 0) {
				return clientNameCompare;
			}
			else {
				return (int) (r1.getClientVersion() - r2.getClientVersion());
			}
		}
		
	}	

	public class SyncUpOperationResult implements OperationResult {
		// TODO [low] Return something for 'up' operation
	}
}
