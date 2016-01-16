/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com>
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;
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
 * Besides the normal behavior of creating transactions from local changes and uploading these,
 * this class is also able to upload existing transactions that have been interrupted during a previous upload attempt.
 * The up operation analyzes maps the local changes over a number of transactions. The size of these transactions are based
 * on the settings in {@link UpOperationOptions}.
 * If a sequence of transactions is interrupted, all queued transactions are written to disk to be resumed later.
 * The next up operation then reads these transactions and resumes them in the same order as they were queued before the interruption.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class UpOperation extends AbstractTransferOperation {
	private static final Logger logger = Logger.getLogger(UpOperation.class.getSimpleName());

	public static final String ACTION_ID = "up";

	private UpOperationOptions options;
	private UpOperationResult result;

	private SqlDatabase localDatabase;
	
	private boolean resuming;
	private TransactionRemoteFile transactionRemoteFileToResume;
	private Collection<RemoteTransaction> remoteTransactionsToResume;
	private BlockingQueue<DatabaseVersion> databaseVersionQueue;

	public UpOperation(Config config) {
		this(config, new UpOperationOptions());
	}

	public UpOperation(Config config, UpOperationOptions options) {
		super(config, ACTION_ID);

		this.options = options;
		this.result = new UpOperationResult();
		this.localDatabase = new SqlDatabase(config);
		
		this.resuming = false;
		this.transactionRemoteFileToResume = null;
		this.remoteTransactionsToResume = null;
		this.databaseVersionQueue = new LinkedBlockingQueue<>();
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

		try {
			if (options.isResume()) {
				prepareResume();			
			}

			if (!resuming) {
				startIndexerThread(databaseVersionQueue);			
			}

			// If we are not resuming from a remote transaction, we need to clean transactions.
			if (transactionRemoteFileToResume == null) {
				transferManager.cleanTransactions();
			}			
		}
		catch (BlockingTransfersException e) {
			stopBecauseOfBlockingTransactions();
			return result;
		}
		
		// Go wild
		int numberOfPerformedTransactions = executeTransactions();
		updateResult(numberOfPerformedTransactions);		

		// Close database connection
		localDatabase.finalize();

		// Finish 'up' before 'cleanup' starts
		finishOperation();
		fireEndEvent();
		
		return result;
	}

	private void updateResult(int numberOfPerformedTransactions) {
		if (numberOfPerformedTransactions == 0) {
			logger.log(Level.INFO, "Local database is up-to-date. NOTHING TO DO!");
			result.setResultCode(UpResultCode.OK_NO_CHANGES);
		}
		else {
			logger.log(Level.INFO, "Sync up done.");
			result.setResultCode(UpResultCode.OK_CHANGES_UPLOADED);
		}
	}

	private void startIndexerThread(BlockingQueue<DatabaseVersion> databaseVersionQueue) {
		// Get a list of files that have been updated
		ChangeSet localChanges = result.getStatusResult().getChangeSet();
		List<File> locallyUpdatedFiles = extractLocallyUpdatedFiles(localChanges);
		List<File> locallyDeletedFiles = extractLocallyDeletedFiles(localChanges);
		
		// Iterate over the changes, deduplicate, and feed DatabaseVersions into an iterator
		Deduper deduper = new Deduper(config.getChunker(), config.getMultiChunker(), config.getTransformer(), options.getTransactionSizeLimit(),
				options.getTransactionFileLimit());
		
		AsyncIndexer asyncIndexer = new AsyncIndexer(config, deduper, locallyUpdatedFiles, locallyDeletedFiles, databaseVersionQueue);
		new Thread(asyncIndexer, "AsyncI/" + config.getLocalDir().getName()).start();
	}

	private void prepareResume() throws Exception {	
		Collection<Long> versionsToResume = transferManager.loadPendingTransactionList();
		boolean hasVersionsToResume = versionsToResume != null && versionsToResume.size() > 0;

		if (hasVersionsToResume) {
			logger.log(Level.INFO, "Found local transaction to resume.");
			logger.log(Level.INFO, "Attempting to find transactionRemoteFile");

			remoteTransactionsToResume = attemptResumeTransactions(versionsToResume);
			Collection<DatabaseVersion> remoteDatabaseVersionsToResume = attemptResumeDatabaseVersions(versionsToResume);

			resuming = remoteDatabaseVersionsToResume != null && remoteTransactionsToResume != null &&
					remoteDatabaseVersionsToResume.size() == remoteTransactionsToResume.size();
			
			if (resuming) {
				databaseVersionQueue.addAll(remoteDatabaseVersionsToResume);				
				databaseVersionQueue.add(new DatabaseVersion()); // Empty database version is the stopping marker			
				
				transactionRemoteFileToResume = attemptResumeTransactionRemoteFile();			
			} 
			else {
				transferManager.clearResumableTransactions();
			}
		}
		else {
			transferManager.clearResumableTransactions();
		}
	}

	/**
	 *	Transfers the given {@link DatabaseVersion} objects to the remote.
	 *	Each {@link DatabaseVersion} will be transferred in its own {@link RemoteTransaction} object.
	 *	
	 *	This method resumes an interrupted sequence of earlier transactions.
	 *	It expects the {@link DatabaseVersion} and {@link RemoteTransaction} files to be in the same order as they were originally generated.
	 *	The first {@link DatabaseVersion} and {@link RemoteTransaction} objects should match the interrupted transaction.
	 *
	 *	The assumption is that the given {@link RemoteTransaction} objects match the given {@link DatabaseVersion} objects.
	 *	The given {@link TransactionRemoteFile} corresponds to the file on the remote from the interrupted transaction.
	 *
	 *	@param databaseVersionQueue The {@link DatabaseVersion} objects to send to the remote.
	 *	@param remoteTransactionsToResume {@link RemoteTransaction} objects that correspond to the given {@link DatabaseVersion} objects.
	 *	@param transactionRemoteFileToResume The file on the remote that was used for the specific transaction that was interrupted.
	 */
	private int executeTransactions() throws Exception {
		Iterator<RemoteTransaction> remoteTransactionsToResumeIterator = (resuming) ? remoteTransactionsToResume.iterator() : null;
		
		// At this point, if a failure occurs from which we can resume, new transaction files will be written
		// Delete any old transaction files
		transferManager.clearPendingTransactions();

		boolean detectedFailure = false;
		Exception caughtFailure = null;
		List<RemoteTransaction> remainingRemoteTransactions = new ArrayList<>();
		List<DatabaseVersion> remainingDatabaseVersions = new ArrayList<>();
		
		DatabaseVersion databaseVersion = databaseVersionQueue.take();
		boolean noDatabaseVersions = databaseVersion.isEmpty();
		
		// Add dirty data to first database
		addDirtyData(databaseVersion);

		//
		while (!databaseVersion.isEmpty()) {
			RemoteTransaction remoteTransaction = null;
			
			if (!resuming) {
				VectorClock newVectorClock = findNewVectorClock();
				
				databaseVersion.setVectorClock(newVectorClock);
				databaseVersion.setTimestamp(new Date());
				databaseVersion.setClient(config.getMachineName());

				remoteTransaction = new RemoteTransaction(config, transferManager);

				// Add multichunks to transaction
				logger.log(Level.INFO, "Uploading new multichunks ...");
				
				// This call adds newly changed chunks to a "RemoteTransaction", so they can be uploaded later.
				addMultiChunksToTransaction(remoteTransaction, databaseVersion.getMultiChunks());
			}
			else {
				remoteTransaction = remoteTransactionsToResumeIterator.next();
			}

			logger.log(Level.INFO, "Uploading database: " + databaseVersion);

			// Create delta database and commit transaction
			// The information about file changes is written to disk to locally "commit" the transaction. This
			// enables Syncany to later resume the transaction if it is interrupted before completion.
			writeAndAddDeltaDatabase(remoteTransaction, databaseVersion, resuming);

			// This thread is to be run when the transaction is interrupted for connectivity reasons. It will serialize
			// the transaction and metadata in memory such that the transaction can be resumed later.
			Thread writeResumeFilesShutDownHook = createAndAddShutdownHook(remoteTransaction, databaseVersion);

			// This performs the actual sync to the remote. It is executed synchronously. Only after the changes
			// are confirmed to have been safely pushed to the remote, will the transaction be marked as complete.
			if (!detectedFailure) {
				boolean committingFailed = true;
				try {
					if (transactionRemoteFileToResume == null) {
						remoteTransaction.commit();
					}
					else {
						remoteTransaction.commit(config.getTransactionFile(), transactionRemoteFileToResume);
						transactionRemoteFileToResume = null;
					}

					logger.log(Level.INFO, "Persisting local SQL database (new database version {0}) ...", databaseVersion.getHeader().toString());
					long newDatabaseVersionId = localDatabase.writeDatabaseVersion(databaseVersion);

					logger.log(Level.INFO, "Removing DIRTY database versions from database ...");
					localDatabase.removeDirtyDatabaseVersions(newDatabaseVersionId);

					logger.log(Level.INFO, "Adding database version to result changes:" + databaseVersion);
					addNewDatabaseChangesToResultChanges(databaseVersion, result.getChangeSet());

					result.incrementTransactionsCompleted();


					logger.log(Level.INFO, "Committing local database.");
					localDatabase.commit();

					committingFailed = false;
				}
				catch (Exception e) {
					detectedFailure = true;
					caughtFailure = e;
				}
				finally {
					// The JVM has not shut down, so we can remove the shutdown hook.
					// If it turns out that committing has failed, we run it explicitly.
					removeShutdownHook(writeResumeFilesShutDownHook);

					if (committingFailed) {
						remainingRemoteTransactions.add(remoteTransaction);
						remainingDatabaseVersions.add(databaseVersion);
					}
				}
			}
			else {
				remainingRemoteTransactions.add(remoteTransaction);
				remainingDatabaseVersions.add(databaseVersion);
			}
			
			if (!noDatabaseVersions) {
				logger.log(Level.FINE, "Waiting for new database version.");
				databaseVersion = databaseVersionQueue.take();
				logger.log(Level.FINE, "Took new database version: " + databaseVersion);
			}
			else {
				logger.log(Level.FINE, "Not waiting for new database version, last one has been taken.");
				break;
			}

		}

		if (detectedFailure) {
			localDatabase.rollback();
			serializeRemoteTransactionsAndMetadata(remainingRemoteTransactions, remainingDatabaseVersions);
			throw caughtFailure;
		}

		return (int) result.getTransactionsCompleted();
	}

	private TransactionRemoteFile attemptResumeTransactionRemoteFile() throws StorageException, BlockingTransfersException {
		TransactionRemoteFile transactionRemoteFile = null;

		// They look for the matching transaction on the remote.
		List<TransactionRemoteFile> transactions = transferManager.getTransactionsByClient(config.getMachineName());

		// If there are blocking transactions, they stop completely.
		// Not sure yet what these blocking structures are.
		if (transactions == null) {
			// We have blocking transactions
			stopBecauseOfBlockingTransactions();
			throw new BlockingTransfersException();
		}

		// There is no sign of the transaction on the remote. Clean up the local transaction.
		if (transactions.size() != 1) {
			logger.log(Level.INFO, "Unable to find (unique) transactionRemoteFile. Not resuming.");
			transferManager.clearResumableTransactions();
		}
		// Remote transaction file found.
		else {
			transactionRemoteFile = transactions.get(0);
		}
		
		return transactionRemoteFile;
	}

	/**
	 * This method creates a Thread, which serializes the {@link remoteTransaction} in the state at the time the thread is run,
	 * as well as the {@link DatabaseVersion} that contains the metadata about what is uploaded in this transaction.
	 *
	 * @param newDatabaseVersion DatabaseVersion that contains everything that should be locally saved when current transaction is resumed.
	 *
	 * @return Thread which is attached as a shutdownHook.
	 */
	private Thread createAndAddShutdownHook(final RemoteTransaction remoteTransaction, final DatabaseVersion newDatabaseVersion) {
		Thread writeResumeFilesShutDownHook = new Thread(new Runnable() {
			@Override
			public void run() {
				serializeRemoteTransactionsAndMetadata(Arrays.asList(remoteTransaction), Arrays.asList(newDatabaseVersion));
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

	/**
	 * This method sets the correct {@link UpResultCode} when another client has a transaction in progress with deletions.
	 */
	private void stopBecauseOfBlockingTransactions() throws StorageException {
		logger.log(Level.INFO, "Another client is blocking the repo with unfinished cleanup.");
		result.setResultCode(UpResultCode.NOK_REPO_BLOCKED);

		finishOperation();
		fireEndEvent();
	}

	/**
	 * This method checks if:
	 *
	 * <ul>
	 * 	<li>If there are local changes => No need for Up.</li>
	 *  <li>If another clients is running Cleanup => Not allowed to upload.</li>
	 *  <li>If remote changes exist => Should Down first.</li>
	 * </ul>
	 *
	 * @returns boolean true if Up can and should be done, false otherwise.
	 */
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

	/**
	 * This method takes the metadata that is to be uploaded, loads it into a {@link MemoryDatabase} and serializes
	 * it to a file. If this is not a resumption of a previous transaction, this file is added to the transaction.
	 * Finally, databaseversions that are uploaded are remembered as known, such that they are not downloaded in future Downs.
	 *
	 * @param newDatabaseVersion {@link DatabaseVersion} containing all metadata that would be locally persisted if the transaction succeeds.
	 * @param resuming boolean indicating if the current transaction is in the process of being resumed.
	 */
	private void writeAndAddDeltaDatabase(RemoteTransaction remoteTransaction, DatabaseVersion newDatabaseVersion, boolean resuming)
			throws InterruptedException, StorageException,
			IOException,
			SQLException {
		// Clone database version (necessary, because the original must not be touched)
		DatabaseVersion deltaDatabaseVersion = newDatabaseVersion.clone();

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
			addLocalDatabaseToTransaction(remoteTransaction, localDeltaDatabaseFile, remoteDeltaDatabaseFile);
		}
		// Remember uploaded database as known.
		List<DatabaseRemoteFile> newDatabaseRemoteFiles = new ArrayList<DatabaseRemoteFile>();
		newDatabaseRemoteFiles.add(remoteDeltaDatabaseFile);
		localDatabase.writeKnownRemoteDatabases(newDatabaseRemoteFiles);
	}

	/**
	 * Serializes a {@link MemoryDatabase} to a file, using the configured transformer.
	 */
	protected void saveDeltaDatabase(MemoryDatabase db, File localDatabaseFile) throws IOException {
		logger.log(Level.INFO, "- Saving database to " + localDatabaseFile + " ...");

		DatabaseXmlSerializer dao = new DatabaseXmlSerializer(config.getTransformer());
		dao.save(db.getDatabaseVersions(), localDatabaseFile);
	}

	/**
	 * This methods iterates over all {@link DatabaseVersion}s that are dirty. Dirty means that they are not in the winning
	 * branch. All data which is contained in these dirty DatabaseVersions is added to the newDatabaseVersion, so that it
	 * is included in the new Up. Note that only metadata is reuploaded, the actual multichunks are still in the repository.
	 *
	 * @param newDatabaseVersion {@link DatabaseVersion} to which dirty data should be added.
	 */
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

	/**
	 * This method extracts the files that are new or changed from a {@link ChangeSet} of local changes.
	 *
	 * @param localChanges {@link ChangeSet} that was the result from a StatusOperation.
	 *
	 * @return a list of Files that are new or have been changed.
	 */
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

	private List<File> extractLocallyDeletedFiles(ChangeSet localChanges) {
		List<File> locallyDeletedFiles = new ArrayList<File>();

		for (String relativeFilePath : localChanges.getDeletedFiles()) {
			locallyDeletedFiles.add(new File(config.getLocalDir() + File.separator + relativeFilePath));
		}

		return locallyDeletedFiles;
	}

	/**
	 * This method fills a {@link ChangeSet} with the files and changes that are uploaded, to include in
	 * the {@link UpOperationResult}.
	 *
	 * @param newDatabaseVersion {@link DatabaseVersion} that contains the changes.
	 * @param resultChanges {@ChangeSet} to which these changes are to be added.
	 */
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

	/**
	 * This methods adds the multichunks that are not yet present in the remote repo to the {@link RemoteTransaction} for
	 * uploading. Multichunks are not uploaded if they are dirty.
	 *
	 * @param multiChunkEntries Collection of multiChunkEntries that are included in the new {@link DatabaseVersion}
	 */
	private void addMultiChunksToTransaction(RemoteTransaction remoteTransaction, Collection<MultiChunkEntry> multiChunksEntries)
			throws InterruptedException, StorageException {
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

	private void addLocalDatabaseToTransaction(RemoteTransaction remoteTransaction, File localDatabaseFile, DatabaseRemoteFile remoteDatabaseFile)
			throws InterruptedException,
			StorageException {
		
		logger.log(Level.INFO, "- Uploading " + localDatabaseFile + " to " + remoteDatabaseFile + " ...");
		remoteTransaction.upload(localDatabaseFile, remoteDatabaseFile);
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

	private Collection<RemoteTransaction> attemptResumeTransactions(Collection<Long> versions) {
		try {
			Collection<RemoteTransaction> remoteTransactions = new ArrayList<>();

			for (Long version : versions) {
				File transactionFile = config.getTransactionFile(version);

				// If a single transaction file is missing, we should restart
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

				remoteTransactions.add(new RemoteTransaction(config, transferManager, transactionTO));
			}
			
			return remoteTransactions;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Invalid transaction file. Cannot resume!");
			return null;
		}
	}

	private Collection<DatabaseVersion> attemptResumeDatabaseVersions(Collection<Long> versions) throws Exception {
		try {
			Collection<DatabaseVersion> databaseVersions = new ArrayList<>();
			
			for (Long version : versions) {
				File databaseFile = config.getTransactionDatabaseFile(version);

				// If a single database file is missing, we should restart
				if (!databaseFile.exists()) {
					return null;
				}

				DatabaseXmlSerializer databaseSerializer = new DatabaseXmlSerializer();
				MemoryDatabase memoryDatabase = new MemoryDatabase();
				databaseSerializer.load(memoryDatabase, databaseFile, null, null, DatabaseReadType.FULL);

				if (memoryDatabase.getDatabaseVersions().size() == 0) {
					return null;
				}

				databaseVersions.add(memoryDatabase.getLastDatabaseVersion());
			}
			
			return databaseVersions;			
		} catch (Exception e) {
			logger.log(Level.WARNING, "Cannot load database versions from 'state'. Cannot resume.");
			return null;
		}
	}

	/**
	 * Serializes both the remote transaction and the current database version
	 * that would be added if Up was successful.
	 * @param newDatabaseVersion the current metadata
	 */
	private void serializeRemoteTransactionsAndMetadata(List<RemoteTransaction> remoteTransactions, List<DatabaseVersion> newDatabaseVersions) {
		try {
			logger.log(Level.INFO, "Persisting status of UpOperation to " + config.getStateDir() + " ...");

			// Collect a list of all database version numbers that will be saved
			List<Long> databaseVersionClocks = new ArrayList<>();
			for (int i = 0; i < remoteTransactions.size(); i++) {
				DatabaseVersion databaseVersion = newDatabaseVersions.get(i);
				long databaseVersionClock = databaseVersion.getVectorClock().getClock(config.getMachineName());
				databaseVersionClocks.add(databaseVersionClock);
			}

			// Write the list of version number to a file, before serializing any transactions!
			// This ensures that no transaction files can exist without a "reference" to them.
			File transactionListFile = config.getTransactionListFile();
			PrintWriter transactionListWriter = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(transactionListFile), "UTF-8"));
			for (Long databaseVersion : databaseVersionClocks) {
				transactionListWriter.println(databaseVersion);
			}
			transactionListWriter.close();

			// For each database version write the transaction and database files
			for (int i = 0; i < remoteTransactions.size(); i++) {
				DatabaseVersion databaseVersion = newDatabaseVersions.get(i);
				long databaseVersionClock = databaseVersionClocks.get(i);

				// Writing transaction file to state dir
				remoteTransactions.get(i).writeToFile(null, config.getTransactionFile(databaseVersionClock));

				// Writing database representation of new database version to state dir
				MemoryDatabase memoryDatabase = new MemoryDatabase();
				memoryDatabase.addDatabaseVersion(databaseVersion);

				DatabaseXmlSerializer dao = new DatabaseXmlSerializer();
				dao.save(memoryDatabase.getDatabaseVersions(), config.getTransactionDatabaseFile(databaseVersionClock));
			}

			// The first transaction may be resumable, so write it to the default transaction file
			remoteTransactions.get(0).writeToFile(null, config.getTransactionFile());
		}
		catch (Exception e) {
			logger.log(Level.WARNING, "Failure when persisting status of Up: ", e);
		}
	}
}
