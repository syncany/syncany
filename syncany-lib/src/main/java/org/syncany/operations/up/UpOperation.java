/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.operations.up;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;
import org.syncany.config.LocalEventBus;
import org.syncany.database.ChunkEntry;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.SqlDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.database.dao.DatabaseXmlSerializer.DatabaseReadType;
import org.syncany.operations.AbstractTransferOperation;
import org.syncany.operations.ChangeSet;
import org.syncany.operations.cleanup.CleanupOperation;
import org.syncany.operations.daemon.messages.UpEndSyncExternalEvent;
import org.syncany.operations.daemon.messages.UpStartSyncExternalEvent;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.ls_remote.LsRemoteOperation;
import org.syncany.operations.ls_remote.LsRemoteOperationResult;
import org.syncany.operations.status.StatusOperation;
import org.syncany.operations.status.StatusOperationResult;
import org.syncany.operations.up.UpOperationResult.UpResultCode;
import org.syncany.plugins.transfer.RemoteTransaction;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;
import org.syncany.plugins.transfer.to.ActionTO;
import org.syncany.plugins.transfer.to.ActionTO.ActionStatus;
import org.syncany.plugins.transfer.to.ActionTO.ActionType;
import org.syncany.plugins.transfer.to.TransactionTO;

/**
 * The up operation implements a central part of Syncany's business logic. It analyzes the local
 * folder, deduplicates new or changed files and uploads newly packed multichunks to the remote
 * storage. The up operation is the complement to the {@link DownOperation}.
 * 
 * <p>The general operation flow is as follows:
 * <ol>
 *   <li>Load local database (if not already loaded)</li>
 *   <li>Analyze local directory using the {@link StatusOperation} to determine any changed/new/deleted files</li>
 *   <li>Determine if there are unknown remote databases using the {@link LsRemoteOperation}, and skip the rest if there are</li>
 *   <li>If there are changes, use the {@link Deduper} and {@link Indexer} to create a new {@link DatabaseVersion} 
 *       (including new chunks, multichunks, file contents and file versions).</li>
 *   <li>Upload new multichunks (if any) using a {@link TransferManager}</li>
 *   <li>Save new {@link DatabaseVersion} to a new (delta) {@link MemoryDatabase} and upload it</li>
 *   <li>Add delta database to local database and store it locally</li>
 * </ol>
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class UpOperation extends AbstractTransferOperation {
	private static final Logger logger = Logger.getLogger(UpOperation.class.getSimpleName());

	public static final String ACTION_ID = "up";

	private LocalEventBus eventBus;

	private UpOperationOptions options;
	private UpOperationResult result;

	private SqlDatabase localDatabase;
	private RemoteTransaction remoteTransaction;

	public UpOperation(Config config) {
		this(config, new UpOperationOptions());
	}

	public UpOperation(Config config, UpOperationOptions options) {
		super(config, ACTION_ID);

		this.eventBus = LocalEventBus.getInstance();
		this.options = options;
		this.result = new UpOperationResult();
		this.localDatabase = new SqlDatabase(config);
		this.remoteTransaction = new RemoteTransaction(config, transferManager);
	}

	@Override
	public UpOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync up' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		fireStartEvent();

		if (!checkPreconditions()) {
			fireEndEvent();
			return result;
		}

		// Upload action file (lock for cleanup)
		startOperation();

		DatabaseVersion newDatabaseVersion = null;
		TransactionRemoteFile transactionRemoteFile = null;
		boolean resuming = false;

		if (options.isResume()) {
			newDatabaseVersion = attemptResumeTransaction();

			if (newDatabaseVersion != null) {
				logger.log(Level.INFO, "Found local transaction to resume.");
				resuming = true;

				logger.log(Level.INFO, "Attempting to find transactionRemoteFile");

				List<TransactionRemoteFile> transactions = transferManager.getTransactionsByClient(config.getMachineName());

				if (transactions == null) {
					// We have blocking transactions
					stopBecauseOfBlockingTransactions();
					return result;
				}

				if (transactions.size() != 1) {
					logger.log(Level.INFO, "Unable to find (unique) transactionRemoteFile. Not resuming.");
					resuming = false;
					transferManager.clearResumableTransactions();
				}
				else {
					transactionRemoteFile = transactions.get(0);
				}
			}
			else {
				transferManager.clearResumableTransactions();
			}
		}

		if (!resuming) {
			boolean blockingTransactionExist = !transferManager.cleanTransactions();

			if (blockingTransactionExist) {
				stopBecauseOfBlockingTransactions();
				return result;
			}

			ChangeSet localChanges = result.getStatusResult().getChangeSet();
			List<File> locallyUpdatedFiles = extractLocallyUpdatedFiles(localChanges);

			// Index
			newDatabaseVersion = index(locallyUpdatedFiles);

			if (newDatabaseVersion.getFileHistories().size() == 0) {
				logger.log(Level.INFO, "Local database is up-to-date. NOTHING TO DO!");
				result.setResultCode(UpResultCode.OK_NO_CHANGES);

				finishOperation();
				fireEndEvent();

				return result;
			}

			// Add multichunks to transaction
			logger.log(Level.INFO, "Uploading new multichunks ...");
			addMultiChunksToTransaction(newDatabaseVersion.getMultiChunks());
		}

		// Create delta database and commit transaction
		writeAndAddDeltaDatabase(newDatabaseVersion, resuming);

		boolean committingFailed = true;
		Thread writeResumeFilesShutDownHook = createAndAddShutdownHook(newDatabaseVersion);

		try {
			if (!resuming) {
				remoteTransaction.commit();
			}
			else {
				remoteTransaction.commit(config.getTransactionFile(), transactionRemoteFile);
			}

			localDatabase.commit();
			committingFailed = false;
		}
		catch (Exception e) {
			localDatabase.rollback();
			throw e;
		}
		finally {
			removeShutdownHook(writeResumeFilesShutDownHook);

			if (committingFailed) {
				writeResumeFilesShutDownHook.run();
			}
		}

		// Save local database
		logger.log(Level.INFO, "Persisting local SQL database (new database version {0}) ...", newDatabaseVersion.getHeader().toString());
		long newDatabaseVersionId = localDatabase.writeDatabaseVersion(newDatabaseVersion);

		logger.log(Level.INFO, "Removing DIRTY database versions from database ...");
		localDatabase.removeDirtyDatabaseVersions(newDatabaseVersionId);

		// Finish 'up' before 'cleanup' starts
		finishOperation();

		logger.log(Level.INFO, "Sync up done.");

		// Result
		addNewDatabaseChangesToResultChanges(newDatabaseVersion, result.getChangeSet());
		result.setResultCode(UpResultCode.OK_CHANGES_UPLOADED);

		fireEndEvent();

		return result;
	}

	private Thread createAndAddShutdownHook(final DatabaseVersion newDatabaseVersion) {
		Thread writeResumeFilesShutDownHook = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					logger.log(Level.INFO, "Persisting status of UpOperation to " + config.getStateDir() + " ...");

					// Writing transaction file to state dir
					remoteTransaction.writeToFile(null, config.getTransactionFile());

					// Writing database representation of new database version to state dir
					MemoryDatabase memoryDatabase = new MemoryDatabase();
					memoryDatabase.addDatabaseVersion(newDatabaseVersion);

					DatabaseXmlSerializer dao = new DatabaseXmlSerializer();
					dao.save(memoryDatabase.getDatabaseVersions(), config.getTransactionDatabaseFile());
				}
				catch (Exception e) {
					logger.log(Level.WARNING, "Failure when persisting status of Up: ", e);
				}
			}
		}, "ResumeShtdwn");

		logger.log(Level.INFO, "Adding shutdown hook (to allow resuming the upload) ...");

		Runtime.getRuntime().addShutdownHook(writeResumeFilesShutDownHook);
		return writeResumeFilesShutDownHook;
	}

	private void removeShutdownHook(Thread writeResumeFilesShutDownHook) {
		Runtime.getRuntime().removeShutdownHook(writeResumeFilesShutDownHook);
	}

	private void fireStartEvent() {
		eventBus.post(new UpStartSyncExternalEvent(config.getLocalDir().getAbsolutePath()));
	}

	private void fireEndEvent() {
		eventBus.post(new UpEndSyncExternalEvent(config.getLocalDir().getAbsolutePath(), result.getResultCode(), result.getChangeSet()));
	}

	private void stopBecauseOfBlockingTransactions() throws StorageException {
		logger.log(Level.INFO, "Another client is blocking the repo with unfinished cleanup.");
		result.setResultCode(UpResultCode.NOK_REPO_BLOCKED);

		finishOperation();
		fireEndEvent();
	}

	private boolean checkPreconditions() throws Exception {
		// Find local changes
		StatusOperation statusOperation = new StatusOperation(config, options.getStatusOptions());
		StatusOperationResult statusOperationResult = statusOperation.execute();
		ChangeSet localChanges = statusOperationResult.getChangeSet();

		result.getStatusResult().setChangeSet(localChanges);

		if (!localChanges.hasChanges()) {
			logger.log(Level.INFO, "Local database is up-to-date (change set). NOTHING TO DO!");
			result.setResultCode(UpResultCode.OK_NO_CHANGES);

			return false;
		}

		// Check if other operations are running
		if (otherRemoteOperationsRunning(CleanupOperation.ACTION_ID)) {
			logger.log(Level.INFO, "* Cleanup running. Skipping down operation.");
			result.setResultCode(UpResultCode.NOK_UNKNOWN_DATABASES);

			return false;
		}

		// Find remote changes (unless --force is enabled)
		if (!options.forceUploadEnabled()) {
			LsRemoteOperationResult lsRemoteOperationResult = new LsRemoteOperation(config, transferManager).execute();
			List<DatabaseRemoteFile> unknownRemoteDatabases = lsRemoteOperationResult.getUnknownRemoteDatabases();

			if (unknownRemoteDatabases.size() > 0) {
				logger.log(Level.INFO, "There are remote changes. Call 'down' first or use --force-upload you must, Luke!");
				logger.log(Level.FINE, "Unknown remote databases are: " + unknownRemoteDatabases);
				result.setResultCode(UpResultCode.NOK_UNKNOWN_DATABASES);

				return false;
			}
			else {
				logger.log(Level.INFO, "No remote changes, ready to upload.");
			}
		}
		else {
			logger.log(Level.INFO, "Force (--force-upload) is enabled, ignoring potential remote changes.");
		}

		return true;
	}

	private void writeAndAddDeltaDatabase(DatabaseVersion newDatabaseVersion, boolean resuming) throws InterruptedException, StorageException,
			IOException,
			SQLException {
		// Clone database version (necessary, because the original must not be touched)
		DatabaseVersion deltaDatabaseVersion = newDatabaseVersion.clone();

		// Add dirty data (if existent)
		addDirtyData(deltaDatabaseVersion);

		// New delta database
		MemoryDatabase deltaDatabase = new MemoryDatabase();
		deltaDatabase.addDatabaseVersion(deltaDatabaseVersion);

		// Save delta database locally
		long newestLocalDatabaseVersion = getNewestDatabaseFileVersion(config.getMachineName(), localDatabase.getKnownDatabases());
		DatabaseRemoteFile remoteDeltaDatabaseFile = new DatabaseRemoteFile(config.getMachineName(), newestLocalDatabaseVersion + 1);
		File localDeltaDatabaseFile = config.getCache().getDatabaseFile(remoteDeltaDatabaseFile.getName());

		logger.log(Level.INFO, "Saving local delta database, version {0} to file {1} ... ", new Object[] { deltaDatabaseVersion.getHeader(),
				localDeltaDatabaseFile });

		saveDeltaDatabase(deltaDatabase, localDeltaDatabaseFile);

		if (!resuming) {
			// Upload delta database, if we are not resuming (in which case the db is in the transaction already)
			logger.log(Level.INFO, "- Uploading local delta database file ...");
			uploadLocalDatabase(localDeltaDatabaseFile, remoteDeltaDatabaseFile);
		}
		// Remember uploaded database as known.
		List<DatabaseRemoteFile> newDatabaseRemoteFiles = new ArrayList<DatabaseRemoteFile>();
		newDatabaseRemoteFiles.add(remoteDeltaDatabaseFile);
		localDatabase.writeKnownRemoteDatabases(newDatabaseRemoteFiles);
	}

	protected void saveDeltaDatabase(MemoryDatabase db, File localDatabaseFile) throws IOException {
		logger.log(Level.INFO, "- Saving database to " + localDatabaseFile + " ...");

		DatabaseXmlSerializer dao = new DatabaseXmlSerializer(config.getTransformer());
		dao.save(db.getDatabaseVersions(), localDatabaseFile);
	}

	private void addDirtyData(DatabaseVersion newDatabaseVersion) {
		Iterator<DatabaseVersion> dirtyDatabaseVersions = localDatabase.getDirtyDatabaseVersions();

		if (!dirtyDatabaseVersions.hasNext()) {
			logger.log(Level.INFO, "No DIRTY data found in database (no dirty databases); Nothing to do here.");
		}
		else {
			logger.log(Level.INFO, "Adding DIRTY data to new database version: ");

			while (dirtyDatabaseVersions.hasNext()) {
				DatabaseVersion dirtyDatabaseVersion = dirtyDatabaseVersions.next();

				logger.log(Level.INFO, "- Adding chunks/multichunks/filecontents from database version " + dirtyDatabaseVersion.getHeader());

				for (ChunkEntry chunkEntry : dirtyDatabaseVersion.getChunks()) {
					newDatabaseVersion.addChunk(chunkEntry);
				}

				for (MultiChunkEntry multiChunkEntry : dirtyDatabaseVersion.getMultiChunks()) {
					newDatabaseVersion.addMultiChunk(multiChunkEntry);
				}

				for (FileContent fileContent : dirtyDatabaseVersion.getFileContents()) {
					newDatabaseVersion.addFileContent(fileContent);
				}
			}
		}
	}

	private List<File> extractLocallyUpdatedFiles(ChangeSet localChanges) {
		List<File> locallyUpdatedFiles = new ArrayList<File>();

		for (String relativeFilePath : localChanges.getNewFiles()) {
			locallyUpdatedFiles.add(new File(config.getLocalDir() + File.separator + relativeFilePath));
		}

		for (String relativeFilePath : localChanges.getChangedFiles()) {
			locallyUpdatedFiles.add(new File(config.getLocalDir() + File.separator + relativeFilePath));
		}

		return locallyUpdatedFiles;
	}

	private void addNewDatabaseChangesToResultChanges(DatabaseVersion newDatabaseVersion, ChangeSet resultChanges) {
		for (PartialFileHistory partialFileHistory : newDatabaseVersion.getFileHistories()) {
			FileVersion lastFileVersion = partialFileHistory.getLastVersion();

			switch (lastFileVersion.getStatus()) {
			case NEW:
				resultChanges.getNewFiles().add(lastFileVersion.getPath());
				break;

			case CHANGED:
			case RENAMED:
				resultChanges.getChangedFiles().add(lastFileVersion.getPath());
				break;

			case DELETED:
				resultChanges.getDeletedFiles().add(lastFileVersion.getPath());
				break;
			}
		}
	}

	private void addMultiChunksToTransaction(Collection<MultiChunkEntry> multiChunksEntries) throws InterruptedException, StorageException {
		List<MultiChunkId> dirtyMultiChunkIds = localDatabase.getDirtyMultiChunkIds();

		for (MultiChunkEntry multiChunkEntry : multiChunksEntries) {
			if (dirtyMultiChunkIds.contains(multiChunkEntry.getId())) {
				logger.log(Level.INFO, "- Ignoring multichunk (from dirty database, already uploaded), " + multiChunkEntry.getId() + " ...");
			}
			else {
				File localMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId());
				MultichunkRemoteFile remoteMultiChunkFile = new MultichunkRemoteFile(multiChunkEntry.getId());

				logger.log(Level.INFO, "- Uploading multichunk {0} from {1} to {2} ...", new Object[] { multiChunkEntry.getId(), localMultiChunkFile,
						remoteMultiChunkFile });

				remoteTransaction.upload(localMultiChunkFile, remoteMultiChunkFile);
			}
		}
	}

	private void uploadLocalDatabase(File localDatabaseFile, DatabaseRemoteFile remoteDatabaseFile) throws InterruptedException, StorageException {
		logger.log(Level.INFO, "- Uploading " + localDatabaseFile + " to " + remoteDatabaseFile + " ...");
		remoteTransaction.upload(localDatabaseFile, remoteDatabaseFile);
	}

	private DatabaseVersion index(List<File> localFiles) throws FileNotFoundException, IOException {

		// Index
		Deduper deduper = new Deduper(config.getChunker(), config.getMultiChunker(), config.getTransformer());
		Indexer indexer = new Indexer(config, deduper);

		DatabaseVersion newDatabaseVersion = indexer.index(localFiles);

		VectorClock newVectorClock = findNewVectorClock();
		newDatabaseVersion.setVectorClock(newVectorClock);
		newDatabaseVersion.setTimestamp(new Date());
		newDatabaseVersion.setClient(config.getMachineName());

		return newDatabaseVersion;
	}

	/**
	 * Finds the next vector clock
	 * 
	 * <p>There are two causes for not having a previous vector clock:
	 * <ul>
	 *   <li>This is the initial version
	 *   <li>A cleanup has wiped *all* database versions
	 * </ul>
	 * 
	 * In the latter case, the method looks at the previous database version numbers 
	 * to determine a new vector clock		
	 */
	private VectorClock findNewVectorClock() {
		// Get last vector clock
		DatabaseVersionHeader lastDatabaseVersionHeader = localDatabase.getLastDatabaseVersionHeader();
		VectorClock lastVectorClock = (lastDatabaseVersionHeader != null) ? lastDatabaseVersionHeader.getVectorClock() : new VectorClock();

		logger.log(Level.INFO, "Last vector clock was: " + lastVectorClock);

		boolean noPreviousVectorClock = lastVectorClock.isEmpty();

		if (noPreviousVectorClock) {
			lastVectorClock = localDatabase.getHighestKnownDatabaseFilenameNumbers();
		}

		VectorClock newVectorClock = lastVectorClock.clone();

		Long lastLocalValue = lastVectorClock.getClock(config.getMachineName());
		Long lastDirtyLocalValue = localDatabase.getMaxDirtyVectorClock(config.getMachineName());

		Long newLocalValue = null;

		if (lastDirtyLocalValue != null) {
			newLocalValue = lastDirtyLocalValue + 1;
		}
		else {
			if (lastLocalValue != null) {
				newLocalValue = lastLocalValue + 1;
			}
			else {
				newLocalValue = 1L;
			}
		}

		newVectorClock.setClock(config.getMachineName(), newLocalValue);

		return newVectorClock;
	}

	/**
	 * This method will try to load a local transaction and the corresponding database file.
	 * 
	 * Side-effect: A resumable transaction will be loaded into remoteTransaction, if it exists.
	 * @return the loaded databaseVersion if found. Null otherwise.
	 */
	private DatabaseVersion attemptResumeTransaction() throws Exception {
		File transactionFile = config.getTransactionFile();

		if (!transactionFile.exists()) {
			return null;
		}

		TransactionTO transactionTO = TransactionTO.load(null, transactionFile);

		// Verify if all files needed are in cache.

		for (ActionTO action : transactionTO.getActions()) {
			if (action.getType() == ActionType.UPLOAD) {
				if (action.getStatus() == ActionStatus.UNSTARTED) {
					if (!action.getLocalTempLocation().exists()) {
						// Unstarted upload has no cached local copy, abort
						return null;
					}
				}
			}

		}

		remoteTransaction = new RemoteTransaction(config, transferManager, transactionTO);

		File databaseFile = config.getTransactionDatabaseFile();

		if (!databaseFile.exists()) {
			return null;
		}

		DatabaseXmlSerializer databaseSerializer = new DatabaseXmlSerializer();
		MemoryDatabase memoryDatabase = new MemoryDatabase();
		databaseSerializer.load(memoryDatabase, databaseFile, null, null, DatabaseReadType.FULL);

		if (memoryDatabase.getDatabaseVersions().size() == 0) {
			return null;
		}

		return memoryDatabase.getLastDatabaseVersion();
	}
}
