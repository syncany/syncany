/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com>
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
package org.syncany.operations.cleanup;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Chunk;
import org.syncany.chunk.MultiChunk;
import org.syncany.config.Config;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.database.dao.FileVersionSqlDao;
import org.syncany.operations.AbstractTransferOperation;
import org.syncany.operations.cleanup.CleanupOperationOptions.TimeUnit;
import org.syncany.operations.cleanup.CleanupOperationResult.CleanupResultCode;
import org.syncany.operations.daemon.messages.CleanupEndSyncExternalEvent;
import org.syncany.operations.daemon.messages.CleanupStartCleaningSyncExternalEvent;
import org.syncany.operations.daemon.messages.CleanupStartSyncExternalEvent;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.ls_remote.LsRemoteOperation;
import org.syncany.operations.ls_remote.LsRemoteOperationResult;
import org.syncany.operations.status.StatusOperation;
import org.syncany.operations.status.StatusOperationResult;
import org.syncany.operations.up.BlockingTransfersException;
import org.syncany.operations.up.UpOperation;
import org.syncany.plugins.transfer.RemoteTransaction;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.files.CleanupRemoteFile;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;

/**
 * The purpose of the cleanup operation is to keep the local database and the
 * remote repository clean -- thereby allowing it to be used indefinitely without
 * any performance issues or storage shortage.
 *
 * <p>The responsibilities of the cleanup operations include:
 * <ul>
 *   <li>Remove old {@link FileVersion} and their corresponding database entities.
 *       In particular, it also removes {@link PartialFileHistory}s, {@link FileContent}s,
 *       {@link Chunk}s and {@link MultiChunk}s.</li>
 *   <li>Merge metadata of a single client and remove old database version files
 *       from the remote storage.</li>
 * </ul>
 *
 * <p>High level strategy:
 * <ul>
 *    <ol>Lock repo and start thread that renews the lock every X seconds</ol>
 *    <ol>Find old versions / contents / ... from database</ol>
 *    <ol>Delete these versions and contents locally</ol>
 *    <ol>Delete all remote metadata</ol>
 *    <ol>Obtain consistent database files from local database</ol>
 *    <ol>Upload new database files to repo</ol>
 *    <ol>Remotely delete unused multichunks</ol>
 *    <ol>Stop lock renewal thread and unlock repo</ol>
 * </ul>
 *
 * <p><b>Important issues:</b>
 * All remote operations MUST check if the lock has been recently renewed. If it hasn't, the connection has been lost.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CleanupOperation extends AbstractTransferOperation {
	private static final Logger logger = Logger.getLogger(CleanupOperation.class.getSimpleName());

	public static final String ACTION_ID = "cleanup";
	private static final int BEFORE_DOUBLE_CHECK_TIME = 1200;

	private CleanupOperationOptions options;
	private CleanupOperationResult result;

	private SqlDatabase localDatabase;
	private RemoteTransaction remoteTransaction;

	public CleanupOperation(Config config) {
		this(config, new CleanupOperationOptions());
	}

	public CleanupOperation(Config config, CleanupOperationOptions options) {
		super(config, ACTION_ID);

		this.options = options;
		this.result = new CleanupOperationResult();
		this.localDatabase = new SqlDatabase(config);
	}

	@Override
	public CleanupOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Cleanup' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		// Do initial check out remote repository preconditions
		CleanupResultCode preconditionResult = checkPreconditions();

		fireStartEvent();
		if (preconditionResult != CleanupResultCode.OK) {
			fireEndEvent();
			return new CleanupOperationResult(preconditionResult);
		}

		fireCleanupNeededEvent();

		// At this point, the operation will lock the repository
		startOperation();

		// If there are any, rollback any existing/old transactions.
		// If other clients have unfinished transactions with deletions, do not proceed.
		try {
			transferManager.cleanTransactions();
		}
		catch (BlockingTransfersException ignored) {
			finishOperation();
			fireEndEvent();
			return new CleanupOperationResult(CleanupResultCode.NOK_REPO_BLOCKED);
		}

		// Wait two seconds (conservative cleanup, see #104)
		logger.log(Level.INFO, "Cleanup: Waiting a while to be sure that no other actions are running ...");
		Thread.sleep(BEFORE_DOUBLE_CHECK_TIME);

		// Check again. No other clients should be busy, because we waited BEFORE_DOUBLE_CHECK_TIME
		preconditionResult = checkPreconditions();

		if (preconditionResult != CleanupResultCode.OK) {
			finishOperation();
			fireEndEvent();
			return new CleanupOperationResult(preconditionResult);
		}

		// If we do cleanup, we are no longer allowed to resume a transaction
		transferManager.clearResumableTransactions();
		transferManager.clearPendingTransactions();

		// Now do the actual work!
		logger.log(Level.INFO, "Cleanup: Starting transaction.");
		remoteTransaction = new RemoteTransaction(config, transferManager);

		removeOldVersions();

		if (options.isRemoveUnreferencedTemporaryFiles()) {
			transferManager.removeUnreferencedTemporaryFiles();
		}

		mergeRemoteFiles();

		// We went succesfully through the entire operation and checked everything. Hence we update the last cleanup time.
		updateLastCleanupTime();

		finishOperation();
		fireEndEvent();

		return updateResultCode(result);
	}

	/**
	 * This method checks if we have changed anything and sets the
	 * {@link CleanupResultCode} of the given result accordingly.
	 *
	 * @param result The result so far in this operation.
	 * @return result The original result, with the relevant {@link CleanupResultCode}
	 */
	private CleanupOperationResult updateResultCode(CleanupOperationResult result) {
		if (result.getMergedDatabaseFilesCount() > 0 || result.getRemovedMultiChunksCount() > 0 || result.getRemovedOldVersionsCount() > 0) {
			result.setResultCode(CleanupResultCode.OK);
		}
		else {
			result.setResultCode(CleanupResultCode.OK_NOTHING_DONE);
		}

		return result;
	}

	private void fireStartEvent() {
		eventBus.post(new CleanupStartSyncExternalEvent(config.getLocalDir().getAbsolutePath()));
	}

	private void fireCleanupNeededEvent() {
		eventBus.post(new CleanupStartCleaningSyncExternalEvent(config.getLocalDir().getAbsolutePath()));
	}

	private void fireEndEvent() {
		eventBus.post(new CleanupEndSyncExternalEvent(config.getLocalDir().getAbsolutePath(), result));
	}

	/**
	 * This method inspects the local database and remote repository to
	 * see if cleanup should be performed.
	 *
	 * @return {@link CleanupResultCode.OK} if nothing prevents continuing, another relevant code otherwise.
	 */
	private CleanupResultCode checkPreconditions() throws Exception {
		if (hasDirtyDatabaseVersions()) {
			return CleanupResultCode.NOK_DIRTY_LOCAL;
		}

		if (!options.isForce() && wasCleanedRecently()) {
			return CleanupResultCode.NOK_RECENTLY_CLEANED;
		}

		if (hasLocalChanges()) {
			return CleanupResultCode.NOK_LOCAL_CHANGES;
		}

		if (hasRemoteChanges()) {
			return CleanupResultCode.NOK_REMOTE_CHANGES;
		}

		if (otherRemoteOperationsRunning(CleanupOperation.ACTION_ID, UpOperation.ACTION_ID, DownOperation.ACTION_ID)) {
			return CleanupResultCode.NOK_OTHER_OPERATIONS_RUNNING;
		}

		return CleanupResultCode.OK;
	}

	private boolean hasLocalChanges() throws Exception {
		StatusOperationResult statusOperationResult = new StatusOperation(config, options.getStatusOptions()).execute();
		return statusOperationResult.getChangeSet().hasChanges();
	}

	/**
	 * This method checks if there exist {@link FileVersion}s which are to be deleted because the history they are a part
	 * of is too long. It will collect these, remove them locally and add them to the {@link RemoteTransaction} for deletion.
	 */
	private void removeOldVersions() throws Exception {
		Map<FileHistoryId, List<FileVersion>> purgeFileVersions = new TreeMap<FileHistoryId, List<FileVersion>>();
		Map<FileHistoryId, FileVersion> purgeBeforeFileVersions = new TreeMap<FileHistoryId, FileVersion>();

		if (options.isRemoveVersionsByInterval()) {
			// Get file versions that should be purged according to the settings that are given. Time-based.
			purgeFileVersions = collectPurgableFileVersions();
		}

		if (options.isRemoveOldVersions()) {
			// Get all non-final fileversions and deleted (final) fileversions that we want to fully delete.
			// purgeFileVersions is modified here!
			purgeBeforeFileVersions = collectPurgeBeforeFileVersions(purgeFileVersions);
		}
		if (purgeFileVersions.isEmpty() && purgeBeforeFileVersions.isEmpty()) {
			logger.log(Level.INFO, "- Old version removal: Not necessary.");
			return;
		}

		logger.log(Level.INFO, "- Old version removal: Found {0} file histories and {1} file versions that need cleaning.", new Object[] {
				purgeFileVersions.size(),
				purgeBeforeFileVersions.size() });

		// Local: First, remove file versions that are not longer needed
		localDatabase.removeSmallerOrEqualFileVersions(purgeBeforeFileVersions);
		localDatabase.removeFileVersions(purgeFileVersions);

		// Local: Then, determine what must be changed remotely and remove it locally
		Map<MultiChunkId, MultiChunkEntry> unusedMultiChunks = localDatabase.getUnusedMultiChunks();

		localDatabase.removeUnreferencedDatabaseEntities();
		deleteUnusedRemoteMultiChunks(unusedMultiChunks);

		// Update stats
		long unusedMultiChunkSize = 0;

		for (MultiChunkEntry removedMultiChunk : unusedMultiChunks.values()) {
			unusedMultiChunkSize += removedMultiChunk.getSize();
		}

		result.setRemovedOldVersionsCount(purgeBeforeFileVersions.size() + purgeFileVersions.size());
		result.setRemovedMultiChunksCount(unusedMultiChunks.size());
		result.setRemovedMultiChunksSize(unusedMultiChunkSize);
	}

	private Map<FileHistoryId, FileVersion> collectPurgeBeforeFileVersions(Map<FileHistoryId, List<FileVersion>> purgeFileVersions) {
		long deleteBeforeTimestamp = System.currentTimeMillis() - options.getMinKeepDeletedSeconds() * 1000;
		
		Map<FileHistoryId, FileVersion> deletedFileVersionsBeforeTimestamp = localDatabase.getDeletedFileVersionsBefore(deleteBeforeTimestamp);
		Map<FileHistoryId, List<FileVersion>> selectedPurgeFileVersions = localDatabase.getFileHistoriesToPurgeBefore(deleteBeforeTimestamp);
		
		Map<FileHistoryId, FileVersion> purgeBeforeFileVersions = new HashMap<FileHistoryId, FileVersion>();
		purgeBeforeFileVersions.putAll(deletedFileVersionsBeforeTimestamp);
		putAllFileVersionsInMap(selectedPurgeFileVersions, purgeFileVersions);
		
		return purgeBeforeFileVersions;
	}

	/**
	 * For all time intervals defined in the purge file settings, determine the eligible file
	 * versions to be purged -- namely all but the newest one.
	 * 
	 * @see CleanupOperation 
	 * @see CleanupOperationOptions#getPurgeFileVersionSettings()
	 * @see FileVersionSqlDao#getFileHistoriesToPurgeInInterval(long, long, TimeUnit)
	 */
	private Map<FileHistoryId, List<FileVersion>> collectPurgableFileVersions() {
		Map<FileHistoryId, List<FileVersion>> purgeFileVersions = new HashMap<FileHistoryId, List<FileVersion>>();

		long currentTime = System.currentTimeMillis();
		long previousTruncateIntervalTimeMultiplier = 0;		
		
		for (Map.Entry<Long, TimeUnit> purgeFileVersionSetting : options.getPurgeFileVersionSettings().entrySet()) {
			Long truncateIntervalMultiplier = purgeFileVersionSetting.getKey();
			TimeUnit truncateIntervalTimeUnit = purgeFileVersionSetting.getValue();			
			
			long beginIntervalTimestamp = currentTime - truncateIntervalMultiplier * 1000;
			long endIntervalTimestamp = currentTime - previousTruncateIntervalTimeMultiplier * 1000;
			
			Map<FileHistoryId, List<FileVersion>> newPurgeFileVersions = localDatabase.getFileHistoriesToPurgeInInterval(
					beginIntervalTimestamp, endIntervalTimestamp, truncateIntervalTimeUnit);

			putAllFileVersionsInMap(newPurgeFileVersions, purgeFileVersions);
			previousTruncateIntervalTimeMultiplier = truncateIntervalMultiplier;
		}

		return purgeFileVersions;
	}

	private void putAllFileVersionsInMap(Map<FileHistoryId, List<FileVersion>> newFileVersions,
			Map<FileHistoryId, List<FileVersion>> fileHistoryPurgeFileVersions) {
		
		for (FileHistoryId fileHistoryId : newFileVersions.keySet()) {
			List<FileVersion> purgeFileVersions = fileHistoryPurgeFileVersions.get(fileHistoryId);
			List<FileVersion> newPurgeFileVersions = newFileVersions.get(fileHistoryId);
			
			if (purgeFileVersions != null) {
				purgeFileVersions.addAll(newPurgeFileVersions);
			}
			else {
				fileHistoryPurgeFileVersions.put(fileHistoryId, newPurgeFileVersions);
			}
		}
	}

	/**
	 * This method adds unusedMultiChunks to the @{link RemoteTransaction} for deletion.
	 *
	 * @param unusedMultiChunks which are to be deleted because all references to them are gone.
	 */
	private void deleteUnusedRemoteMultiChunks(Map<MultiChunkId, MultiChunkEntry> unusedMultiChunks) throws StorageException {
		logger.log(Level.INFO, "- Deleting remote multichunks ...");

		for (MultiChunkEntry multiChunkEntry : unusedMultiChunks.values()) {
			logger.log(Level.FINE, "  + Deleting remote multichunk " + multiChunkEntry + " ...");
			remoteTransaction.delete(new MultichunkRemoteFile(multiChunkEntry.getId()));
		}
	}

	private boolean hasDirtyDatabaseVersions() {
		Iterator<DatabaseVersion> dirtyDatabaseVersions = localDatabase.getDirtyDatabaseVersions();
		return dirtyDatabaseVersions.hasNext(); // TODO [low] Is this a resource creeper?
	}

	private boolean hasRemoteChanges() throws Exception {
		LsRemoteOperationResult lsRemoteOperationResult = new LsRemoteOperation(config).execute();
		return lsRemoteOperationResult.getUnknownRemoteDatabases().size() > 0;
	}

	/**
	 * Checks if Cleanup has been performed less then a configurable time ago.
	 */
	private boolean wasCleanedRecently() throws Exception {
		Long lastCleanupTime = localDatabase.getCleanupTime();

		if (lastCleanupTime == null) {
			return false;
		}
		else {
			return lastCleanupTime + options.getMinSecondsBetweenCleanups() > System.currentTimeMillis() / 1000;
		}
	}

	/**
	 * This method deletes all remote database files and writes new ones for each client using the local database.
	 * To make the state clear and prevent issues with replacing files, new database files are given a higher number
	 * than all existing database files.
	 * Both the deletions and the new files added to the current @{link RemoteTransaction}.
	 */
	private void mergeRemoteFiles() throws Exception {
		// Retrieve all database versions
		Map<String, List<DatabaseRemoteFile>> allDatabaseFilesMap = retrieveAllRemoteDatabaseFiles();

		boolean needMerge = needMerge(allDatabaseFilesMap);

		if (!needMerge) {
			logger.log(Level.INFO, "- No purging happened. Number of database files does not exceed threshold. Not merging remote files.");
			return;
		}

		// Now do the merge!
		logger.log(Level.INFO, "- Merge remote files ...");

		List<DatabaseRemoteFile> allToDeleteDatabaseFiles = new ArrayList<DatabaseRemoteFile>();
		Map<File, DatabaseRemoteFile> allMergedDatabaseFiles = new TreeMap<File, DatabaseRemoteFile>();

		for (String client : allDatabaseFilesMap.keySet()) {
			List<DatabaseRemoteFile> clientDatabaseFiles = allDatabaseFilesMap.get(client);
			Collections.sort(clientDatabaseFiles);
			logger.log(Level.INFO, "Databases: " + clientDatabaseFiles);

			// 1. Determine files to delete remotely
			List<DatabaseRemoteFile> toDeleteDatabaseFiles = new ArrayList<DatabaseRemoteFile>(clientDatabaseFiles);
			allToDeleteDatabaseFiles.addAll(toDeleteDatabaseFiles);

			// 2. Write new database file and save it in allMergedDatabaseFiles
			writeMergeFile(client, allMergedDatabaseFiles);

		}

		rememberDatabases(allMergedDatabaseFiles);

		// 3. Prepare transaction

		// Queue old databases for deletion
		for (RemoteFile toDeleteRemoteFile : allToDeleteDatabaseFiles) {
			logger.log(Level.INFO, "   + Deleting remote file " + toDeleteRemoteFile + " ...");
			remoteTransaction.delete(toDeleteRemoteFile);
		}

		// Queue new databases for uploading
		for (File lastLocalMergeDatabaseFile : allMergedDatabaseFiles.keySet()) {
			RemoteFile lastRemoteMergeDatabaseFile = allMergedDatabaseFiles.get(lastLocalMergeDatabaseFile);

			logger.log(Level.INFO, "   + Uploading new file {0} from local file {1} ...", new Object[] { lastRemoteMergeDatabaseFile,
					lastLocalMergeDatabaseFile });

			remoteTransaction.upload(lastLocalMergeDatabaseFile, lastRemoteMergeDatabaseFile);
		}

		finishMerging();

		// Update stats
		result.setMergedDatabaseFilesCount(allToDeleteDatabaseFiles.size());
	}

	/**
	 * This method decides if a merge is needed. Most of the time it will be, since we need to merge every time we remove
	 * any FileVersions to delete them remotely. Another reason for merging is if the number of files exceeds a certain threshold.
	 * This threshold scales linearly with the number of clients that have database files.
	 *
	 * @param allDatabaseFilesMap used to determine if there are too many database files.
	 *
	 * @return true if there are too many database files or we have removed FileVersions, false otherwise.
	 */
	private boolean needMerge(Map<String, List<DatabaseRemoteFile>> allDatabaseFilesMap) {
		int numberOfDatabaseFiles = 0;

		for (String client : allDatabaseFilesMap.keySet()) {
			numberOfDatabaseFiles += allDatabaseFilesMap.get(client).size();
		}

		// A client will merge databases if the number of databases exceeds the maximum number per client times the amount of clients
		int maxDatabaseFiles = options.getMaxDatabaseFiles() * allDatabaseFilesMap.keySet().size();
		boolean tooManyDatabaseFiles = numberOfDatabaseFiles > maxDatabaseFiles;
		boolean removedOldVersions = result.getRemovedOldVersionsCount() > 0;

		return removedOldVersions || tooManyDatabaseFiles || options.isForce();
	}

	/**
	 * This method writes the file with merged databases for a single client and adds it to a Map containing all merged
	 * database files. This is done by querying the local database for all {@link DatabaseVersion}s by this client and
	 * serializing them.
	 *
	 * @param clientName for which we want to write the merged dataabse file.
	 * @param allMergedDatabaseFiles Map where we add the merged file once it is written.
	 */
	private void writeMergeFile(String clientName, Map<File, DatabaseRemoteFile> allMergedDatabaseFiles)
			throws StorageException, IOException {

		// Increment the version by 1, to signal cleanup has occurred

		long lastClientVersion = getNewestDatabaseFileVersion(clientName, localDatabase.getKnownDatabases());
		DatabaseRemoteFile newRemoteMergeDatabaseFile = new DatabaseRemoteFile(clientName, lastClientVersion + 1);

		File newLocalMergeDatabaseFile = config.getCache().getDatabaseFile(newRemoteMergeDatabaseFile.getName());

		logger.log(Level.INFO, "   + Writing new merge file (all files up to {0}) to {1} ...", new Object[] { lastClientVersion,
				newLocalMergeDatabaseFile });

		Iterator<DatabaseVersion> lastNDatabaseVersions = localDatabase.getDatabaseVersionsTo(clientName, lastClientVersion);

		DatabaseXmlSerializer databaseDAO = new DatabaseXmlSerializer(config.getTransformer());
		databaseDAO.save(lastNDatabaseVersions, newLocalMergeDatabaseFile);
		allMergedDatabaseFiles.put(newLocalMergeDatabaseFile, newRemoteMergeDatabaseFile);
	}

	/**
	 * This method locally remembers which databases were newly uploaded, such that they will not be downloaded in
	 * future Downs.
	 */
	private void rememberDatabases(Map<File, DatabaseRemoteFile> allMergedDatabaseFiles) throws SQLException {
		// Remember newly written files as so not to redownload them later.
		List<DatabaseRemoteFile> newRemoteMergeDatabaseFiles = new ArrayList<DatabaseRemoteFile>();
		newRemoteMergeDatabaseFiles.addAll(allMergedDatabaseFiles.values());

		logger.log(Level.INFO, "Writing new known databases table: " + newRemoteMergeDatabaseFiles);

		localDatabase.removeKnownDatabases();
		localDatabase.writeKnownRemoteDatabases(newRemoteMergeDatabaseFiles);
	}

	/**
	 * This method finishes the merging of remote files, by attempting to commit the {@link RemoteTransaction}.
	 * If this fails, it will roll back the local database.
	 */
	private void finishMerging() throws Exception {
		updateCleanupFileInTransaction();

		try {
			logger.log(Level.INFO, "Cleanup: COMMITTING TX ...");

			remoteTransaction.commit();
			localDatabase.commit();
		}
		catch (StorageException e) {
			logger.log(Level.INFO, "Cleanup: FAILED TO COMMIT TX. Rolling back ...");

			localDatabase.rollback();
			throw e;
		}

		logger.log(Level.INFO, "Cleanup: SUCCESS COMMITTING TX.");
	}

	/**
	 * This method obtains a Map with Lists of {@link DatabaseRemoteFile}s as values, by listing them in the remote repo and
	 * collecting the files per client.
	 *
	 * @return a Map with clientNames as keys and lists of corresponding DatabaseRemoteFiles as values.
	 */
	private Map<String, List<DatabaseRemoteFile>> retrieveAllRemoteDatabaseFiles() throws StorageException {
		SortedMap<String, List<DatabaseRemoteFile>> allDatabaseRemoteFilesMap = new TreeMap<String, List<DatabaseRemoteFile>>();
		Map<String, DatabaseRemoteFile> allDatabaseRemoteFiles = transferManager.list(DatabaseRemoteFile.class);

		for (Map.Entry<String, DatabaseRemoteFile> entry : allDatabaseRemoteFiles.entrySet()) {
			String clientName = entry.getValue().getClientName();

			if (allDatabaseRemoteFilesMap.get(clientName) == null) {
				allDatabaseRemoteFilesMap.put(clientName, new ArrayList<DatabaseRemoteFile>());
			}

			allDatabaseRemoteFilesMap.get(clientName).add(entry.getValue());
		}

		return allDatabaseRemoteFilesMap;
	}

	/**
	 * This method checks what the current cleanup number is, increments it by one and adds
	 * a new cleanup file to the transaction, to signify to other clients that Cleanup has occurred.
	 */
	private void updateCleanupFileInTransaction() throws StorageException, IOException {
		if (remoteTransaction.isEmpty()) {
			// No need to bump numbers
			return;
		}
		// Find all existing cleanup files
		Map<String, CleanupRemoteFile> cleanupFiles = transferManager.list(CleanupRemoteFile.class);

		long lastRemoteCleanupNumber = getLastRemoteCleanupNumber(cleanupFiles);

		// Schedule any existing cleanup files for deletion
		for (CleanupRemoteFile cleanupRemoteFile : cleanupFiles.values()) {
			remoteTransaction.delete(cleanupRemoteFile);
		}

		// Upload a new cleanup file that indicates changes
		File newCleanupFile = config.getCache().createTempFile("cleanup");
		long newCleanupNumber = lastRemoteCleanupNumber + 1;

		remoteTransaction.upload(newCleanupFile, new CleanupRemoteFile(newCleanupNumber));
		localDatabase.writeCleanupNumber(newCleanupNumber);
	}

	/**
	 * The cleanup time is used to check if cleanup has been done recently. If it has, we do not need
	 * to clean again.
	 */
	private void updateLastCleanupTime() throws SQLException {
		// Set cleanup number locally
		localDatabase.writeCleanupTime(System.currentTimeMillis() / 1000);
		localDatabase.commit();
	}
}
