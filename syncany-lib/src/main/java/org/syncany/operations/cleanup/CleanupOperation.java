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
package org.syncany.operations.cleanup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.simpleframework.xml.core.Persister;
import org.syncany.chunk.Chunk;
import org.syncany.chunk.MultiChunk;
import org.syncany.config.Config;
import org.syncany.config.to.CleanupTO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.DatabaseVersionHeader.DatabaseVersionType;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.operations.AbstractTransferOperation;
import org.syncany.operations.cleanup.CleanupOperationResult.CleanupResultCode;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.ls_remote.LsRemoteOperation;
import org.syncany.operations.ls_remote.LsRemoteOperation.LsRemoteOperationResult;
import org.syncany.operations.status.StatusOperation;
import org.syncany.operations.status.StatusOperationResult;
import org.syncany.operations.up.UpOperation;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.MultiChunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;

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
public class CleanupOperation extends AbstractTransferOperation {
	private static final Logger logger = Logger.getLogger(CleanupOperation.class.getSimpleName());
	
	public static final String ACTION_ID = "cleanup";
	private static final int BEFORE_DOUBLE_CHECK_TIME = 1200;
	
	private CleanupOperationOptions options;
	private CleanupOperationResult result;

	private SqlDatabase localDatabase;

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

		if (preconditionResult != CleanupResultCode.OK) {
			return new CleanupOperationResult(preconditionResult);
		}

		startOperation();
		
		// Wait two seconds (conservative cleanup, see #104)
		logger.log(Level.INFO, "Cleanup: Waiting a while to be sure that no other actions are running ...");
		Thread.sleep(BEFORE_DOUBLE_CHECK_TIME);
		
		// Check again
		preconditionResult = checkPreconditions();

		if (preconditionResult != CleanupResultCode.OK) {
			finishOperation();
			return new CleanupOperationResult(preconditionResult);
		}
		
		// Now do the actual work!
		
		if (options.isRemoveOldVersions()) {
			removeOldVersions();
		}

		if (options.isMergeRemoteFiles()) {
			mergeRemoteFiles();
		}

		removeLostMultiChunks();

		setLastTimeCleaned(System.currentTimeMillis()/1000);
		finishOperation();
		return updateResultCode(result);
	}

	private void removeLostMultiChunks() throws StorageException {
		Map<String, MultiChunkRemoteFile> remoteMultiChunks = transferManager.list(MultiChunkRemoteFile.class);
		
		Map<MultiChunkId, MultiChunkEntry> locallyKnownMultiChunks = new HashMap<>();
		
		locallyKnownMultiChunks.putAll(localDatabase.getMultiChunks());
		locallyKnownMultiChunks.putAll(localDatabase.getMuddyMultiChunks());
		
		for (MultiChunkRemoteFile remoteMultiChunk : remoteMultiChunks.values()) {
			MultiChunkId remoteMultiChunkId = new MultiChunkId(remoteMultiChunk.getMultiChunkId());
			boolean multiChunkExistsLocally = locallyKnownMultiChunks.containsKey(remoteMultiChunkId);

			if (!multiChunkExistsLocally) {
				logger.log(Level.WARNING, "- Deleting lost/unknown remote multichunk " + remoteMultiChunk + " ...");
				transferManager.delete(remoteMultiChunk);

				result.getRemovedMultiChunks().put(remoteMultiChunkId, new MultiChunkEntry(remoteMultiChunkId, -1));
			}
		}
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
		Map<FileHistoryId, FileVersion> purgeFileVersions = new HashMap<>();
		
		purgeFileVersions.putAll(localDatabase.getFileHistoriesWithMostRecentPurgeVersion(options.getKeepVersionsCount()));
		purgeFileVersions.putAll(localDatabase.getDeletedFileVersions());
		
		boolean purgeDatabaseVersionNecessary = purgeFileVersions.size() > 0;

		if (!purgeDatabaseVersionNecessary) {
			logger.log(Level.INFO, "- Old version removal: Not necessary (no file histories longer than {0} versions found).",
					options.getKeepVersionsCount());
			return;
		}

		logger.log(Level.INFO, "- Old version removal: Found {0} file histories that need cleaning (longer than {1} versions).", new Object[] {
				purgeFileVersions.size(), options.getKeepVersionsCount() });

		// Local: First, remove file versions that are not longer needed
		localDatabase.removeSmallerOrEqualFileVersions(purgeFileVersions);

		// Local: Then, determine what must be changed remotely and remove it locally
		Map<MultiChunkId, MultiChunkEntry> unusedMultiChunks = localDatabase.getUnusedMultiChunks();
		DatabaseVersion purgeDatabaseVersion = createPurgeDatabaseVersion(purgeFileVersions);

		localDatabase.removeUnreferencedDatabaseEntities();

		localDatabase.writeDatabaseVersionHeader(purgeDatabaseVersion.getHeader());
		localDatabase.commit();

		// Remote: serialize purge database version to file and upload
		DatabaseRemoteFile newPurgeRemoteFile = findNewPurgeRemoteFile(purgeDatabaseVersion.getHeader());
		File tempLocalPurgeDatabaseFile = writePurgeFile(purgeDatabaseVersion, newPurgeRemoteFile);

		uploadPurgeFile(tempLocalPurgeDatabaseFile, newPurgeRemoteFile);
		remoteDeleteUnusedMultiChunks(unusedMultiChunks);

		// Update stats
		result.setRemovedOldVersionsCount(purgeFileVersions.size());
		result.setRemovedMultiChunks(unusedMultiChunks);
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

	private void remoteDeleteUnusedMultiChunks(Map<MultiChunkId, MultiChunkEntry> unusedMultiChunks) throws StorageException {
		logger.log(Level.INFO, "- Deleting remote multichunks ...");

		for (MultiChunkEntry multiChunkEntry : unusedMultiChunks.values()) {
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
	
	private boolean wasCleanedRecently() {
		return getLastTimeCleaned() + options.getMinSecondsBetweenCleanups() > System.currentTimeMillis()/1000;
	}

	private void mergeRemoteFiles() throws IOException, StorageException {
		

		
		// Retrieve all database versions
		Map<String, List<DatabaseRemoteFile>> allDatabaseFilesMap = retrieveAllRemoteDatabaseFiles();
		
		List<DatabaseRemoteFile> allToDeleteDatabaseFiles = new ArrayList<DatabaseRemoteFile>();
		Map<File, DatabaseRemoteFile> allMergedDatabaseFiles = new TreeMap<File, DatabaseRemoteFile>();
		
		int numberOfDatabaseFiles = 0;
		
		for (String client : allDatabaseFilesMap.keySet()) {
			numberOfDatabaseFiles += allDatabaseFilesMap.get(client).size();
		}
		
		// A client will merge databases if the number of databases exceeds the maximum number per client times the amount of clients
		boolean notTooManyDatabaseFiles = numberOfDatabaseFiles <= options.getMaxDatabaseFiles()*allDatabaseFilesMap.keySet().size();
		
		if (!options.isForce() && notTooManyDatabaseFiles) {
			logger.log(Level.INFO, "- Merge remote files: Not necessary ({0} database files, max. {1})", new Object[] {
					numberOfDatabaseFiles, options.getMaxDatabaseFiles()*allDatabaseFilesMap.keySet().size() });
			return;
		}
		
		for (String client : allDatabaseFilesMap.keySet()) {
			List<DatabaseRemoteFile> clientDatabaseFiles = allDatabaseFilesMap.get(client);
			Collections.sort(clientDatabaseFiles);
			logger.log(Level.INFO, "Databases: " + clientDatabaseFiles);
			
			// Now do the merge!
			logger.log(Level.INFO, "- Merge remote files: Merging necessary ({0} database files, max. {1}) ...",
					new Object[] { clientDatabaseFiles.size(), options.getMaxDatabaseFiles() });
	
			// 1. Determine files to delete remotely
			List<DatabaseRemoteFile> toDeleteDatabaseFiles = new ArrayList<DatabaseRemoteFile>(clientDatabaseFiles);

			// 2. Write merge file
			DatabaseRemoteFile lastRemoteMergeDatabaseFile = toDeleteDatabaseFiles.get(toDeleteDatabaseFiles.size() - 1);
			File lastLocalMergeDatabaseFile = config.getCache().getDatabaseFile(lastRemoteMergeDatabaseFile.getName());

			logger.log(Level.INFO, "   + Writing new merge file (from {0}, to {1}) to {2} ...", new Object[] {
					toDeleteDatabaseFiles.get(0).getClientVersion(), lastRemoteMergeDatabaseFile.getClientVersion(), lastLocalMergeDatabaseFile });

			long lastLocalClientVersion = lastRemoteMergeDatabaseFile.getClientVersion();
			Iterator<DatabaseVersion> lastNDatabaseVersions = localDatabase.getDatabaseVersionsTo(client, lastLocalClientVersion);

			DatabaseXmlSerializer databaseDAO = new DatabaseXmlSerializer(config.getTransformer());
			databaseDAO.save(lastNDatabaseVersions, lastLocalMergeDatabaseFile);
			
			// Queue files for uploading and deletion
			allToDeleteDatabaseFiles.addAll(toDeleteDatabaseFiles);
			allMergedDatabaseFiles.put(lastLocalMergeDatabaseFile, lastRemoteMergeDatabaseFile);

		}
		
		// 3. Uploading merge file

		// And delete others
		for (RemoteFile toDeleteRemoteFile : allToDeleteDatabaseFiles) {
			logger.log(Level.INFO, "   + Deleting remote file " + toDeleteRemoteFile + " ...");
			transferManager.delete(toDeleteRemoteFile);
		}
		
		for (File lastLocalMergeDatabaseFile : allMergedDatabaseFiles.keySet()) {
			// TODO [high] Issue #64: TM cannot overwrite, might lead to chaos if operation does not finish, uploading the new merge file, this might
			// happen often if new file is bigger!
			
			RemoteFile lastRemoteMergeDatabaseFile = allMergedDatabaseFiles.get(lastLocalMergeDatabaseFile);
	
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
		}

		// Update stats
		result.setMergedDatabaseFilesCount(allToDeleteDatabaseFiles.size());
	}

	/**
	 * retrieveAllRemoteDatabaseFiles returns a Map with clientNames as keys and
	 * lists of corresponding DatabaseRemoteFiles as values.
	 */
	private Map<String, List<DatabaseRemoteFile>> retrieveAllRemoteDatabaseFiles() throws StorageException {
		TreeMap<String, List<DatabaseRemoteFile>> allDatabaseRemoteFilesMap = new TreeMap<String, List<DatabaseRemoteFile>>();
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

	private long getLastTimeCleaned() {
		try {
			CleanupTO cleanupTO = (new Persister()).read(CleanupTO.class, config.getCleanupFile());
			return cleanupTO.getLastTimeCleaned();
		}
		catch (Exception e) {
			logger.log(Level.INFO, "Something went wrong with reading cleanup.xml, assuming never cleaned." + e.getMessage());
			return 0;
		}
	}
	
	private void setLastTimeCleaned(long lastTimeCleaned) {
		CleanupTO cleanupTO = new CleanupTO();
		cleanupTO.setLastTimeCleaned(lastTimeCleaned);
		
		try {
			logger.log(Level.INFO, "Writing cleanup.xml");
			(new Persister()).write(cleanupTO, config.getCleanupFile());
		}
		catch (Exception e) {
			// Not doing anything else, because the worst that could happen is that cleanup is run an extra time
			logger.log(Level.INFO, "Something went wrong with writing cleanup.xml." + e.getMessage());
		}
		
	}
}
