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
package org.syncany.operations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Chunk;
import org.syncany.chunk.MultiChunk;
import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.DatabaseVersionHeader.DatabaseVersionType;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.operations.LsRemoteOperation.LsRemoteOperationResult;
import org.syncany.operations.StatusOperation.StatusOperationOptions;
import org.syncany.operations.StatusOperation.StatusOperationResult;

import com.google.common.collect.Lists;

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
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class CleanupOperation extends Operation {
	private static final Logger logger = Logger.getLogger(CleanupOperation.class.getSimpleName());
	private static final String LOCK_CLIENT_NAME = "lock";

	public static final int MIN_KEEP_DATABASE_VERSIONS = 5;
	public static final int MAX_KEEP_DATABASE_VERSIONS = 15;

	private CleanupOperationOptions options;
	private CleanupOperationResult result;

	private TransferManager transferManager;
	private SqlDatabase localDatabase;

	private DatabaseRemoteFile lockFile;

	public CleanupOperation(Config config) {
		this(config, new CleanupOperationOptions());
	}

	public CleanupOperation(Config config, CleanupOperationOptions options) {
		super(config);

		this.options = options;
		this.result = new CleanupOperationResult();

		this.transferManager = config.getPlugin().createTransferManager(config.getConnection());
		this.localDatabase = new SqlDatabase(config);
		
		this.lockFile = null;
	}

	@Override
	public CleanupOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Cleanup' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		 CleanupResultCode preconditionResult = checkPreconditions();
		
		if (preconditionResult != CleanupResultCode.OK) {
			return new CleanupOperationResult(preconditionResult);
		}
		
		if (options.isMergeRemoteFiles()) {
			mergeRemoteFiles();
		}

		if (options.isRemoveOldVersions()) {
			removeOldVersions();
		}

		if (options.isRepackageMultiChunks()) {
			// To be done at a later time
			// repackageMultiChunks();
		}

		return updateResultCode(result);
	}

	private CleanupOperationResult updateResultCode(CleanupOperationResult result) {
		if (result.getMergedDatabaseFilesCount() > 0 || result.getRemovedMultiChunks().size() > 0 || result.getRemovedOldVersionsCount() > 0) {
			result.setResultCode(CleanupResultCode.OK);
		}
		else {
			result.setResultCode(CleanupResultCode.OK_NOTHING_DONE);
		}
		
		return result;
	}

	private CleanupResultCode checkPreconditions() throws Exception {
		if (hasDirtyDatabaseVersions()) {
			return CleanupResultCode.NOK_DIRTY_LOCAL;
		}
		
		if (hasLocalChanges()) {
			return CleanupResultCode.NOK_LOCAL_CHANGES;
		}
		
		if (hasRemoteChanges()) {
			return CleanupResultCode.NOK_REMOTE_CHANGES;
		}
		
		return CleanupResultCode.OK;
	}

	private boolean hasLocalChanges() throws Exception {
		StatusOperationResult statusOperationResult = new StatusOperation(config, options.getStatusOptions()).execute();
		return statusOperationResult.getChangeSet().hasChanges();
	}

	/**
	 * High level strategy:
	 * 1. Lock repo and start thread that renews the lock every X seconds
	 * 2. Find old versions / contents / ... from database
	 * 3. Write and upload old versions to PRUNE file
	 * 4. Remotely delete unused multichunks 
	 * 5. Stop lock renewal thread and unlock repo
	 * 
	 * Important issues:
	 *  - TODO [high] Issue #64: All remote operations MUST be performed atomically. How to achieve this? 
	 *    How to react if one operation works and the other one fails?
	 *  - All remote operations MUST check if the lock has been recently renewed. If it hasn't, the connection has been lost.
	 *  
	 * @throws Exception 
	 */
	private void removeOldVersions() throws Exception {
		Map<FileHistoryId, FileVersion> mostRecentPurgeFileVersions = localDatabase.getFileHistoriesWithMostRecentPurgeVersion(options.getKeepVersionsCount());
		boolean purgeDatabaseVersionNecessary = mostRecentPurgeFileVersions.size() > 0;
		
		if (!purgeDatabaseVersionNecessary) {
			logger.log(Level.INFO, "- Old version removal: Not necessary (no file histories longer than {0} versions found).", options.getKeepVersionsCount());
			return;
		}		
		
		logger.log(Level.INFO, "- Old version removal: Found {0} file histories that need cleaning (longer than {1} versions).", new Object[] {
				mostRecentPurgeFileVersions.size(), options.getKeepVersionsCount() });
		
		lockRemoteRepository(); // Write-lock sufficient?
		// startLockRenewalThread(); TODO [medium] Implement lock renewal thread

		// Local: First, remove file versions that are not longer needed
		localDatabase.removeSmallerOrEqualFileVersions(mostRecentPurgeFileVersions);
		localDatabase.removeDeletedFileVersions();

		// Local: Then, determine what must be changed remotely and remove it locally
		List<MultiChunkEntry> unusedMultiChunks = localDatabase.getUnusedMultiChunks();
		DatabaseVersion purgeDatabaseVersion = createPurgeDatabaseVersion(mostRecentPurgeFileVersions);
		
		localDatabase.removeUnreferencedDatabaseEntities();		
			
		localDatabase.writeDatabaseVersionHeader(purgeDatabaseVersion.getHeader());
		localDatabase.commit();
		
		// Remote: serialize purge database version to file and upload
		DatabaseRemoteFile newPurgeRemoteFile = findNewPurgeRemoteFile(purgeDatabaseVersion.getHeader());
		File tempLocalPurgeDatabaseFile = writePurgeFile(purgeDatabaseVersion, newPurgeRemoteFile);

		uploadPurgeFile(tempLocalPurgeDatabaseFile, newPurgeRemoteFile);
		remoteDeleteUnusedMultiChunks(unusedMultiChunks);
		
		// Update stats
		result.setRemovedOldVersionsCount(mostRecentPurgeFileVersions.size());
		result.setRemovedMultiChunks(unusedMultiChunks);

		// stopLockRenewalThread();
		unlockRemoteRepository();		
	}

	private void uploadPurgeFile(File tempPurgeFile, DatabaseRemoteFile newPurgeRemoteFile) throws StorageException {
		logger.log(Level.INFO, "- Uploading PURGE database file " + newPurgeRemoteFile + " ...");
		transferManager.upload(tempPurgeFile, newPurgeRemoteFile);
	}

	private DatabaseVersion createPurgeDatabaseVersion(Map<FileHistoryId, FileVersion> mostRecentPurgeFileVersions) {
		DatabaseVersionHeader lastDatabaseVersionHeader = localDatabase.getLastDatabaseVersionHeader();
		VectorClock lastVectorClock = lastDatabaseVersionHeader.getVectorClock();
		
		VectorClock purgeVectorClock = lastVectorClock.clone();
		purgeVectorClock.incrementClock(config.getMachineName());		
		
		DatabaseVersionHeader purgeDatabaseVersionHeader = new DatabaseVersionHeader();
		purgeDatabaseVersionHeader.setType(DatabaseVersionType.PURGE);
		purgeDatabaseVersionHeader.setDate(new Date());
		purgeDatabaseVersionHeader.setClient(config.getMachineName());
		purgeDatabaseVersionHeader.setVectorClock(purgeVectorClock);
		
		DatabaseVersion purgeDatabaseVersion = new DatabaseVersion();
		purgeDatabaseVersion.setHeader(purgeDatabaseVersionHeader);	

		for (Entry<FileHistoryId, FileVersion> fileHistoryEntry : mostRecentPurgeFileVersions.entrySet()) {
			PartialFileHistory purgeFileHistory = new PartialFileHistory(fileHistoryEntry.getKey());
			
			purgeFileHistory.addFileVersion(fileHistoryEntry.getValue());			
			purgeDatabaseVersion.addFileHistory(purgeFileHistory);
						
			logger.log(Level.FINE, "- Pruning file history " + fileHistoryEntry.getKey() + " versions <= " + fileHistoryEntry.getValue() + " ...");
		}
		
		return purgeDatabaseVersion;		
	}
	
	private File writePurgeFile(DatabaseVersion purgeDatabaseVersion, DatabaseRemoteFile newPurgeDatabaseFile) throws IOException {		
		File localPurgeDatabaseFile = config.getCache().getDatabaseFile(newPurgeDatabaseFile.getName());
		
		DatabaseXmlSerializer xmlSerializer = new DatabaseXmlSerializer(config.getTransformer());
		xmlSerializer.save(Lists.newArrayList(purgeDatabaseVersion), localPurgeDatabaseFile);
		
		return localPurgeDatabaseFile;
	}

	private DatabaseRemoteFile findNewPurgeRemoteFile(DatabaseVersionHeader purgeDatabaseVersionHeader) throws StorageException {
		Long localMachineVersion = purgeDatabaseVersionHeader.getVectorClock().getClock(config.getMachineName());
		return new DatabaseRemoteFile(config.getMachineName(), localMachineVersion);
	}

	private void remoteDeleteUnusedMultiChunks(List<MultiChunkEntry> unusedMultiChunks) throws StorageException {
		logger.log(Level.INFO, "- Deleting remote multichunks ...");
		
		for (MultiChunkEntry multiChunkEntry : unusedMultiChunks) {
			logger.log(Level.FINE, "  + Deleting remote multichunk " + multiChunkEntry + " ...");
			transferManager.delete(new MultiChunkRemoteFile(multiChunkEntry.getId()));
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
	
	private void lockRemoteRepository() throws StorageException, IOException {
		File tempLockFile = File.createTempFile("syncany", "lock");
		tempLockFile.deleteOnExit();

		lockFile = new DatabaseRemoteFile(LOCK_CLIENT_NAME, System.currentTimeMillis());

		logger.log(Level.INFO, "- Setting repository write lock: uploading {0} ...", lockFile);
		transferManager.upload(tempLockFile, lockFile);

		tempLockFile.delete();
	}

	private void unlockRemoteRepository() throws StorageException {
		logger.log(Level.INFO, "- Removing repository lock: deleting {0} ...", lockFile);
		transferManager.delete(lockFile);
	}

	private void mergeRemoteFiles() throws IOException, StorageException {
		// Retrieve and sort machine's database versions
		TreeMap<String, DatabaseRemoteFile> ownDatabaseFilesMap = retrieveOwnRemoteDatabaseFiles();

		if (ownDatabaseFilesMap.size() <= MAX_KEEP_DATABASE_VERSIONS) {
			logger.log(Level.INFO, "- Merge remote files: Not necessary ({0} database files, max. {1})", new Object[] { ownDatabaseFilesMap.size(),
					MAX_KEEP_DATABASE_VERSIONS });
			
			return;
		}

		// // Now do the merge!
		logger.log(Level.INFO, "- Merge remote files: Merging necessary ({0} database files, max. {1}) ...",
				new Object[] { ownDatabaseFilesMap.size(), MAX_KEEP_DATABASE_VERSIONS });

		// 1. Determine files to delete remotely
		List<DatabaseRemoteFile> toDeleteDatabaseFiles = new ArrayList<DatabaseRemoteFile>();
		int numOfDatabaseFilesToDelete = ownDatabaseFilesMap.size() - MIN_KEEP_DATABASE_VERSIONS;

		for (DatabaseRemoteFile ownDatabaseFile : ownDatabaseFilesMap.values()) {
			if (toDeleteDatabaseFiles.size() < numOfDatabaseFilesToDelete) {
				toDeleteDatabaseFiles.add(ownDatabaseFile);
			}
		}

		// 2. Write merge file
		DatabaseRemoteFile lastRemoteMergeDatabaseFile = toDeleteDatabaseFiles.get(toDeleteDatabaseFiles.size() - 1);
		File lastLocalMergeDatabaseFile = config.getCache().getDatabaseFile(lastRemoteMergeDatabaseFile.getName());

		logger.log(Level.INFO, "   + Writing new merge file (from {0}, to {1}) to {2} ...", new Object[] {
				toDeleteDatabaseFiles.get(0).getClientVersion(), lastRemoteMergeDatabaseFile.getClientVersion(), lastLocalMergeDatabaseFile });

		long lastLocalClientVersion = lastRemoteMergeDatabaseFile.getClientVersion();
		Iterator<DatabaseVersion> lastNDatabaseVersions = localDatabase.getDatabaseVersionsTo(config.getMachineName(), lastLocalClientVersion);

		DatabaseXmlSerializer databaseDAO = new DatabaseXmlSerializer(config.getTransformer());
		databaseDAO.save(lastNDatabaseVersions, lastLocalMergeDatabaseFile);

		// 3. Uploading merge file

		// And delete others
		for (RemoteFile toDeleteRemoteFile : toDeleteDatabaseFiles) {
			logger.log(Level.INFO, "   + Deleting remote file " + toDeleteRemoteFile + " ...");
			transferManager.delete(toDeleteRemoteFile);
		}

		// TODO [high] Issue #64: TM cannot overwrite, might lead to chaos if operation does not finish, uploading the new merge file, this might happen often if
		// new file is bigger!

		logger.log(Level.INFO, "   + Uploading new file {0} from local file {1} ...", new Object[] { lastRemoteMergeDatabaseFile,
				lastLocalMergeDatabaseFile });
		
		try {
			// Make sure it's deleted
			transferManager.delete(lastRemoteMergeDatabaseFile);
		}
		catch (StorageException e) {
			// Don't care!
		}
		
		transferManager.upload(lastLocalMergeDatabaseFile, lastRemoteMergeDatabaseFile);
		
		// Update stats
		result.setMergedDatabaseFilesCount(toDeleteDatabaseFiles.size());
	}

	private TreeMap<String, DatabaseRemoteFile> retrieveOwnRemoteDatabaseFiles() throws StorageException {
		TreeMap<String, DatabaseRemoteFile> ownDatabaseRemoteFiles = new TreeMap<String, DatabaseRemoteFile>();
		Map<String, DatabaseRemoteFile> allDatabaseRemoteFiles = transferManager.list(DatabaseRemoteFile.class);

		for (Map.Entry<String, DatabaseRemoteFile> entry : allDatabaseRemoteFiles.entrySet()) {
			if (config.getMachineName().equals(entry.getValue().getClientName())) {
				ownDatabaseRemoteFiles.put(entry.getKey(), entry.getValue());
			}
		}

		return ownDatabaseRemoteFiles;
	}

	public static class CleanupOperationOptions implements OperationOptions {
		private StatusOperationOptions statusOptions = new StatusOperationOptions();
		private boolean mergeRemoteFiles = true;
		private boolean removeOldVersions = true;
		private int keepVersionsCount = 5;
		private boolean repackageMultiChunks = true;
		private double repackageUnusedThreshold = 0.7;
		
		public StatusOperationOptions getStatusOptions() {
			return statusOptions;
		}

		public void setStatusOptions(StatusOperationOptions statusOptions) {
			this.statusOptions = statusOptions;
		}

		public boolean isMergeRemoteFiles() {
			return mergeRemoteFiles;
		}

		public boolean isRemoveOldVersions() {
			return removeOldVersions;
		}

		public double getRepackageUnusedThreshold() {
			return repackageUnusedThreshold;
		}

		public int getKeepVersionsCount() {
			return keepVersionsCount;
		}

		public boolean isRepackageMultiChunks() {
			return repackageMultiChunks;
		}

		public void setMergeRemoteFiles(boolean mergeRemoteFiles) {
			this.mergeRemoteFiles = mergeRemoteFiles;
		}

		public void setRemoveOldVersions(boolean removeOldVersions) {
			this.removeOldVersions = removeOldVersions;
		}

		public void setKeepVersionsCount(int keepVersionsCount) {
			this.keepVersionsCount = keepVersionsCount;
		}

		public void setRepackageMultiChunks(boolean repackageMultiChunks) {
			this.repackageMultiChunks = repackageMultiChunks;
		}

		public void setRepackageUnusedThreshold(double repackageUnusedThreshold) {
			this.repackageUnusedThreshold = repackageUnusedThreshold;
		}
	}

	public enum CleanupResultCode {
		OK, OK_NOTHING_DONE, NOK_REMOTE_CHANGES, NOK_LOCAL_CHANGES, NOK_DIRTY_LOCAL, NOK_ERROR
	}
	
	public static class CleanupOperationResult implements OperationResult {
		private CleanupResultCode resultCode = CleanupResultCode.OK_NOTHING_DONE;
		private int mergedDatabaseFilesCount = 0;
		private int removedOldVersionsCount = 0;
		private List<MultiChunkEntry> removedMultiChunks = new ArrayList<MultiChunkEntry>();

		public CleanupOperationResult() {
			// Nothing.
		}

		public CleanupOperationResult(CleanupResultCode resultCode) {
			this.resultCode = resultCode;
		}
		
		public void setResultCode(CleanupResultCode resultCode) {
			this.resultCode = resultCode;
		}
		
		public CleanupResultCode getResultCode() {
			return resultCode;
		}

		public int getMergedDatabaseFilesCount() {
			return mergedDatabaseFilesCount;
		}

		public void setMergedDatabaseFilesCount(int mergedDatabaseFilesCount) {
			this.mergedDatabaseFilesCount = mergedDatabaseFilesCount;
		}

		public int getRemovedOldVersionsCount() {
			return removedOldVersionsCount;
		}

		public void setRemovedOldVersionsCount(int removedOldVersionsCount) {
			this.removedOldVersionsCount = removedOldVersionsCount;
		}

		public List<MultiChunkEntry> getRemovedMultiChunks() {
			return removedMultiChunks;
		}

		public void setRemovedMultiChunks(List<MultiChunkEntry> removedMultiChunks) {
			this.removedMultiChunks = removedMultiChunks;
		}			
	}
}
