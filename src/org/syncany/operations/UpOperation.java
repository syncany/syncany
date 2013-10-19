package org.syncany.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.RemoteDatabaseFile;
import org.syncany.database.VectorClock;
import org.syncany.database.XmlDatabaseDAO;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;
import org.syncany.operations.LsRemoteOperation.RemoteStatusOperationResult;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.UpOperation.UpOperationResult.UpResultCode;
import org.syncany.util.StringUtil;


public class UpOperation extends Operation {
	private static final Logger logger = Logger.getLogger(UpOperation.class.getSimpleName());

	public static final int MIN_KEEP_DATABASE_VERSIONS = 5;
	public static final int MAX_KEEP_DATABASE_VERSIONS = 15;
	
	private UpOperationOptions options;
	private UpOperationResult result;
	private TransferManager transferManager; 
	private Database loadedDatabase;
	private Database dirtyDatabase;
	
	public UpOperation(Config config) {
		this(config, null, new UpOperationOptions());
	}	
	
	public UpOperation(Config config, Database database) {
		this(config, database, new UpOperationOptions());
	}	
	
	public UpOperation(Config config, Database database, UpOperationOptions options) {
		super(config);		
		
		this.options = options;
		this.result = new UpOperationResult();
		this.transferManager = config.getConnection().createTransferManager();
		this.loadedDatabase = database;
	}
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync up' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		// Load database
		Database database = (loadedDatabase != null) 
				? loadedDatabase
				: ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();
		
		// TODO [low] this is ugly. Add to LoadDatabaseOperation somehow
		if (config.getDirtyDatabaseFile().exists()) {
			dirtyDatabase = loadDirtyDatabase();
		}
		
		// Find local changes
		ChangeSet statusChangeSet = ((StatusOperationResult) new StatusOperation(config, database, options.getStatusOptions()).execute()).getChangeSet();
		result.getStatusResult().setChangeSet(statusChangeSet);
		
		if (!statusChangeSet.hasChanges()) {
			logger.log(Level.INFO, "Local database is up-to-date (change set). NOTHING TO DO!");
			result.setResultCode(UpResultCode.OK_NO_CHANGES);
			
			return result;
		}
		
		// Find remote changes (unless --force is enabled)
		if (!options.forceUploadEnabled()) {
			List<RemoteFile> unknownRemoteDatabases = ((RemoteStatusOperationResult) new LsRemoteOperation(config, database, transferManager).execute()).getUnknownRemoteDatabases();
			
			if (unknownRemoteDatabases.size() > 0) {
				logger.log(Level.INFO, "There are remote changes. Call 'down' first or use --force, Luke!.");
				result.setResultCode(UpResultCode.NOK_UNKNOWN_DATABASES);
				
				return result;
			}
			else {
				logger.log(Level.INFO, "No remote changes, ready to upload.");
			}
		}
		else {
			logger.log(Level.INFO, "Force (--force) is enabled, ignoring potential remote changes.");
		}
		
		List<File> locallyUpdatedFiles = determineLocallyUpdatedFiles(statusChangeSet);
		
		// Index
		DatabaseVersion newDatabaseVersion = index(locallyUpdatedFiles, database);
		
		if (newDatabaseVersion.getFileHistories().size() == 0) {
			logger.log(Level.INFO, "Local database is up-to-date. NOTHING TO DO!");			
			result.setResultCode(UpResultCode.OK_NO_CHANGES);
			
			return result;
		}
		else {
			logger.log(Level.INFO, "Adding newest database version "+newDatabaseVersion.getHeader()+" to local database ...");
			database.addDatabaseVersion(newDatabaseVersion);
				
			logger.log(Level.INFO, "Uploading new multichunks ...");
			uploadMultiChunks(database.getLastDatabaseVersion().getMultiChunks());
			
			long newestLocalDatabaseVersion = newDatabaseVersion.getVectorClock().get(config.getMachineName());

			DatabaseRemoteFile remoteDeltaDatabaseFile = new DatabaseRemoteFile("db-"+config.getMachineName()+"-"+newestLocalDatabaseVersion);
			File localDeltaDatabaseFile = config.getCache().getDatabaseFile(remoteDeltaDatabaseFile.getName());	

			logger.log(Level.INFO, "Saving local delta database file ...");
			logger.log(Level.INFO, "- Saving versions from: "+newDatabaseVersion.getHeader()+", to: "+newDatabaseVersion.getHeader()+") to file "+localDeltaDatabaseFile+" ...");
			saveLocalDatabase(database, newDatabaseVersion, newDatabaseVersion, localDeltaDatabaseFile);
			
			logger.log(Level.INFO, "- Uploading local delta database file ...");
			uploadLocalDatabase(localDeltaDatabaseFile, remoteDeltaDatabaseFile);
			
			if (options.cleanupEnabled()) {
				cleanupOldDatabases(database, newestLocalDatabaseVersion);
			}
			
			logger.log(Level.INFO, "Saving local database to file "+config.getDatabaseFile()+" ...");  
			saveLocalDatabase(database, config.getDatabaseFile());
			
			logger.log(Level.INFO, "Sync up done.");
		}
		
		if (config.getDirtyDatabaseFile().exists()) {
			logger.log(Level.INFO, "- Deleting dirty.db from: "+config.getDirtyDatabaseFile());
			config.getDirtyDatabaseFile().delete(); 
		}
		
		// Result
		updateResultChangeSet(newDatabaseVersion);
		result.setResultCode(UpResultCode.OK_APPLIED_CHANGES);
		
		return result;
	}	
	
	private List<File> determineLocallyUpdatedFiles(ChangeSet statusChangeSet) {
		List<File> locallyUpdatedFiles = new ArrayList<File>();
		
		for (String relativeFilePath : statusChangeSet.getNewFiles()) {
			locallyUpdatedFiles.add(new File(config.getLocalDir()+File.separator+relativeFilePath));			
		}
		
		for (String relativeFilePath : statusChangeSet.getChangedFiles()) {
			locallyUpdatedFiles.add(new File(config.getLocalDir()+File.separator+relativeFilePath));			
		}
		
		return locallyUpdatedFiles;
	}

	private void updateResultChangeSet(DatabaseVersion newDatabaseVersion) {
		ChangeSet changeSet = result.getChangeSet();
		
		for (PartialFileHistory partialFileHistory : newDatabaseVersion.getFileHistories()) {
			FileVersion lastFileVersion = partialFileHistory.getLastVersion();
			
			switch (lastFileVersion.getStatus()) {
				case NEW:
					changeSet.getNewFiles().add(lastFileVersion.getPath());
					break;
					
				case CHANGED:
				case RENAMED:
					changeSet.getChangedFiles().add(lastFileVersion.getPath());
					break;
					
				case DELETED:
					changeSet.getDeletedFiles().add(lastFileVersion.getPath());
					break;
			}			
		}		
	}

	private Database loadDirtyDatabase() throws IOException {
		Database aDirtyDatabase = new Database();
		DatabaseDAO dao = new XmlDatabaseDAO(config.getTransformer());
		
		dao.load(aDirtyDatabase, config.getDirtyDatabaseFile());
		
		return aDirtyDatabase;
	}

	private void uploadMultiChunks(Collection<MultiChunkEntry> multiChunksEntries) throws InterruptedException, StorageException {
		for (MultiChunkEntry multiChunkEntry : multiChunksEntries) {
			if (dirtyDatabase != null && dirtyDatabase.getMultiChunk(multiChunkEntry.getId()) != null) {
				logger.log(Level.INFO, "- Ignoring multichunk (from dirty database, already uploaded), "+StringUtil.toHex(multiChunkEntry.getId())+" ...");	
			}
			else {
				File localMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId());
				MultiChunkRemoteFile remoteMultiChunkFile = new MultiChunkRemoteFile(localMultiChunkFile.getName());
				
				logger.log(Level.INFO, "- Uploading multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" from "+localMultiChunkFile+" to "+remoteMultiChunkFile+" ...");
				transferManager.upload(localMultiChunkFile, remoteMultiChunkFile);
				
				logger.log(Level.INFO, "  + Removing "+StringUtil.toHex(multiChunkEntry.getId())+" locally ...");
				localMultiChunkFile.delete();
			}
		}		
	}

	private void uploadLocalDatabase(File localDatabaseFile, DatabaseRemoteFile remoteDatabaseFile) throws InterruptedException, StorageException {		
		logger.log(Level.INFO, "- Uploading "+localDatabaseFile+" to "+remoteDatabaseFile+" ..."); 		
		transferManager.upload(localDatabaseFile, remoteDatabaseFile);
	}

	private DatabaseVersion index(List<File> localFiles, Database database) throws FileNotFoundException, IOException {			
		// Get last vector clock
		String previousClient = null;
		VectorClock lastVectorClock = null;
		
		if (database.getLastDatabaseVersion() != null) {
			previousClient = database.getLastDatabaseVersion().getClient();
			lastVectorClock = database.getLastDatabaseVersion().getVectorClock();
		}
		else {
			previousClient = null;
			lastVectorClock = new VectorClock();
		}
		
		// New vector clock
		VectorClock newVectorClock = lastVectorClock.clone();

		Long lastLocalValue = lastVectorClock.getClock(config.getMachineName());
		Long lastDirtyLocalValue = (dirtyDatabase != null) ? dirtyDatabase.getLastDatabaseVersion().getVectorClock().getClock(config.getMachineName()) : null;
		
		Long newLocalValue = null;
		
		if (lastDirtyLocalValue != null) {
			newLocalValue = lastDirtyLocalValue+1; // TODO [medium] Does this lead to problems? C-1 does not exist! Possible problems with DatabaseReconciliator?
		}
		else {		
			if (lastLocalValue != null) {
				newLocalValue = lastLocalValue+1;
			}
			else {
				newLocalValue = 1L;
			}
		}
		
		newVectorClock.setClock(config.getMachineName(), newLocalValue);		

		// Index
		Deduper deduper = new Deduper(config.getChunker(), config.getMultiChunker(), config.getTransformer());
		Indexer indexer = new Indexer(config, deduper, database, dirtyDatabase);
		
		DatabaseVersion newDatabaseVersion = indexer.index(localFiles);	
	
		newDatabaseVersion.setVectorClock(newVectorClock);
		newDatabaseVersion.setTimestamp(new Date());	
		newDatabaseVersion.setClient(config.getMachineName());
		newDatabaseVersion.setPreviousClient(previousClient);
						
		return newDatabaseVersion;
	}
	
	private void cleanupOldDatabases(Database database, long newestLocalDatabaseVersion) throws Exception {
		// Retrieve and sort machine's database versions
		Map<String, RemoteFile> ownRemoteDatabaseFiles = transferManager.list("db-"+config.getMachineName()+"-"); // TODO [low] Use file prefix or other method
		List<RemoteDatabaseFile> ownDatabaseFiles = new ArrayList<RemoteDatabaseFile>();	
		
		for (RemoteFile ownRemoteDatabaseFile : ownRemoteDatabaseFiles.values()) {
			ownDatabaseFiles.add(new RemoteDatabaseFile(ownRemoteDatabaseFile.getName()));
		}
		
		Collections.sort(ownDatabaseFiles);
	
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
					toDeleteDatabaseFiles.add(new DatabaseRemoteFile("db-"+config.getMachineName()+"-"+localVersion)); // TODO [low] Do this differently db-...
				}
			}
		}
		
		if (firstMergeDatabaseVersion == null || lastMergeDatabaseVersion == null) {
			throw new Exception("Cannot cleanup: unable to find first/last database version: first = "+firstMergeDatabaseFile+"/"+firstMergeDatabaseVersion+", last = "+lastMergeDatabaseFile+"/"+lastMergeDatabaseVersion);
		}
		
		// Now write merge file
		File localMergeDatabaseVersionFile = config.getCache().getDatabaseFile("db-"+config.getMachineName()+"-"+lastMergeDatabaseFile.getClientVersion());
		DatabaseRemoteFile remoteMergeDatabaseVersionFile = new DatabaseRemoteFile(localMergeDatabaseVersionFile.getName());
		
		logger.log(Level.INFO, "   + Writing new merge file (from "+firstMergeDatabaseVersion.getHeader()+", to "+lastMergeDatabaseVersion.getHeader()+") to file "+localMergeDatabaseVersionFile+" ...");

		DatabaseDAO databaseDAO = new XmlDatabaseDAO(config.getTransformer()); 			
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

	public static class UpOperationOptions implements OperationOptions {
		private StatusOperationOptions statusOptions = new StatusOperationOptions();
		private boolean forceUploadEnabled = false;
		private boolean cleanupEnabled = true;

		public StatusOperationOptions getStatusOptions() {
			return statusOptions;
		}

		public void setStatusOptions(StatusOperationOptions statusOptions) {
			this.statusOptions = statusOptions;
		}

		public boolean forceUploadEnabled() {
			return forceUploadEnabled;
		}

		public void setForceUploadEnabled(boolean forceUploadEnabled) {
			this.forceUploadEnabled = forceUploadEnabled;
		}

		public boolean cleanupEnabled() {
			return cleanupEnabled;
		}

		public void setCleanupEnabled(boolean cleanupEnabled) {
			this.cleanupEnabled = cleanupEnabled;
		}
	}
	
	public static class UpOperationResult implements OperationResult {		
		public enum UpResultCode { OK_APPLIED_CHANGES, OK_NO_CHANGES, NOK_UNKNOWN_DATABASES };
		
		private UpResultCode resultCode;
		private StatusOperationResult statusResult = new StatusOperationResult(); 
		private ChangeSet uploadChangeSet = new ChangeSet();
		
		public UpResultCode getResultCode() {
			return resultCode;
		}
		
		public void setResultCode(UpResultCode resultCode) {
			this.resultCode = resultCode;
		}
		
		public void setStatusResult(StatusOperationResult statusResult) {
			this.statusResult = statusResult;
		}
		
		public void setUploadChangeSet(ChangeSet uploadChangeSet) {
			this.uploadChangeSet = uploadChangeSet;
		}
		
		public StatusOperationResult getStatusResult() {
			return statusResult;
		}
		
		public ChangeSet getChangeSet() {
			return uploadChangeSet;
		}
	}
}
