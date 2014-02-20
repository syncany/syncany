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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.ChunkEntry;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.SqlDatabase;
import org.syncany.database.dao.XmlDatabaseSerializer;

public class CleanupOperation extends Operation {
	private static final Logger logger = Logger.getLogger(CleanupOperation.class.getSimpleName());

	public static final int MIN_KEEP_DATABASE_VERSIONS = 5;
	public static final int MAX_KEEP_DATABASE_VERSIONS = 15;

	private CleanupOperationOptions options;
	private TransferManager transferManager;
	private SqlDatabase localDatabase;

	public CleanupOperation(Config config) {
		this(config, new CleanupOperationOptions());
	}
	
	public CleanupOperation(Config config, CleanupOperationOptions options) {
		super(config);
		
		this.options = options;
		this.transferManager = config.getConnection().createTransferManager();
		this.localDatabase = new SqlDatabase(config);
	}

	@Override
	public CleanupOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Cleanup' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");
	
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
		
		return null;
	}	

	/**
	 * <b>WARNING: Syntax errors; still DRAFTING / BRAIN STORMING here ...</b> 
	 * 
	 * High level strategy:
	 * 1. Lock repo and start thread that renews the lock every X seconds
	 * 2. Find old versions / contents / ... from database
	 * 3. Write and upload old versions to PRUNE file
	 * 4. Remotely delete unused multichunks 
	 * 5. Stop lock renewal thread and unlock repo
	 * 
	 * Important issues:
	 *  - All remote operations MUST be performed atomically. How to achieve this? 
	 *    How to react if one operation works and the other one fails?
	 *  - All remote operations MUST check if the lock has been recently renewed. If it hasn't, the connection has been lost.
	 *  
	 * @throws Exception 
	 */
	private void removeOldVersions() throws Exception {
		if (hasDirtyDatabaseVersions()) {
			throw new Exception("Cannot cleanup database if local repository is in a dirty state; Call 'up' first.");
		}
		
		lockRemoteRepository(); // Write-lock sufficient?
		// startLockRenewalThread(); TODO [medium] Implement lock renewal thread
						
		List<PartialFileHistory> oldVersions = findOldVersions(options.getKeepVersionsCount());
		List<ChunkEntry> unusedChunks = findUnusedChunks(options.getKeepVersionsCount());
		List<FileContent> unusedFileContents = findUnusedFileContents(options.getKeepVersionsCount());
		List<MultiChunkEntry> unusedMultiChunks = findUnusedMultiChunks(options.getKeepVersionsCount());
		
		// Local
		File tempPruneFile = writePruneFile(oldVersions, unusedChunks, unusedFileContents, unusedMultiChunks);
		PruneRemoteFile newPruneRemoteFile = findNewPruneRemoteFile();
		
		deleteLocalUnusedChunks(unusedChunks);
		deleteLocalUnusedFileContents(unusedFileContents);
		deleteLocalUnusedMultiChunks(unusedMultiChunks);
		deleteLocalOldVersions(oldVersions);
						
		// Remote
		uploadPruneFile(tempPruneFile, newPruneRemoteFile); // prune-A-0000001    OR     db-PRUNE-0000001  ??
		remoteDeleteUnusedMultiChunks(unusedMultiChunks);		
								
		// stopLockRenewalThread();
		unlockRemoteRepository();
	}

	private void uploadPruneFile(File tempPruneFile, PruneRemoteFile newPruneRemoteFile) {
		// TODO Auto-generated method stub
		
	}

	private class PruneRemoteFile extends RemoteFile {
		public PruneRemoteFile(String name) throws StorageException {
			super(name);
		}		
	}
	
	private void deleteLocalUnusedChunks(List<ChunkEntry> unusedChunks) {
		// TODO Auto-generated method stub
		
	}

	private List<ChunkEntry> findUnusedChunks(int keepVersionsCount) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<FileContent> findUnusedFileContents(int keepVersionsCount) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<MultiChunkEntry> findUnusedMultiChunks(int keepVersionsCount) {
		// TODO Auto-generated method stub
		return null;
	}

	private File writePruneFile(List<PartialFileHistory> oldVersions, List<ChunkEntry> unusedChunks, List<FileContent> unusedFileContents,
			List<MultiChunkEntry> unusedMultiChunks) {
		
		DatabaseVersion pruneDatabaseVersion = new DatabaseVersion();
		
		for (PartialFileHistory partialFileHistory : oldVersions) {
			pruneDatabaseVersion.addFileHistory(partialFileHistory);
			
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, "- Pruning partial file history "+partialFileHistory.getFileId()+ "...");
				
				for (FileVersion fileVersion : partialFileHistory.getFileVersions().values()) {
					logger.log(Level.FINE, "  + "+fileVersion);
				}
			}
		}
		
		return null;
	}

	private PruneRemoteFile findNewPruneRemoteFile() {
		// TODO Auto-generated method stub
		return null;
	}

	private void deleteLocalUnusedFileContents(List<FileContent> unusedFileContents) {
		// TODO Auto-generated method stub
		
	}

	private void deleteLocalUnusedMultiChunks(List<MultiChunkEntry> unusedMultiChunks) {
		// TODO Auto-generated method stub
		
	}

	private void deleteLocalOldVersions(List<PartialFileHistory> oldVersions) {
		// TODO Auto-generated method stub
		
	}

	private void remoteDeleteUnusedMultiChunks(List<MultiChunkEntry> unusedMultiChunks) {
		// TODO Auto-generated method stub
		
	}

	private boolean hasDirtyDatabaseVersions() {
		Iterator<DatabaseVersion> dirtyDatabaseVersions = localDatabase.getDirtyDatabaseVersions();		
		return dirtyDatabaseVersions.hasNext(); // TODO [low] Is this a resource creeper?		
	}

	private List<PartialFileHistory> findOldVersions(int keepVersionsCount) {		
		return localDatabase.getFileHistoriesWithPurgeVersions(keepVersionsCount);	
	}

	private void lockRemoteRepository() throws StorageException, IOException {
		File tempLockFile = File.createTempFile("syncany", "lock");
		tempLockFile.deleteOnExit();
		
		transferManager.upload(tempLockFile, new DatabaseRemoteFile("lock", System.currentTimeMillis()));
		tempLockFile.delete();
	}

	private void unlockRemoteRepository() throws StorageException {
		transferManager.delete(new DatabaseRemoteFile("lock", 0));
	}
	

	/*private void repackageMultiChunks() {
		List<Map<MultiChunkEntry, ChunkEntry>> partiallyUnusedMultiChunks = findPartiallyUnusedMultiChunks(options.getKeepVersionsCount(), options.getRepackageUnusedThreshold());			
		Map<MultiChunkEntry, File> newRepackagedMultiChunks = repackagePartiallyUnusedMultiChunks(partiallyUnusedMultiChunks);
					
		// Remote changes 
		uploadRepackagedMultiChunks(repackagedMultiChunks);
		remoteDeletePartiallyUnusedMultiChunks(partiallyUnusedMultiChunks);
		
		// Local changes
		removeLocalPartiallyUnusedMultiChunks(partiallyUnusedMultiChunks);
		insertLocalNewRepackagedMultiChunks(newRepackagedMultiChunks);
	}*/

	private void mergeRemoteFiles() throws IOException, StorageException {
		// Retrieve and sort machine's database versions
		TreeMap<String, DatabaseRemoteFile> ownDatabaseFilesMap = retrieveOwnRemoteDatabaseFiles();
		
		if (ownDatabaseFilesMap.size() <= MAX_KEEP_DATABASE_VERSIONS) {
			logger.log(Level.INFO, "- Merge remote files: Not necessary (" + ownDatabaseFilesMap.size() + " database files, max. " + MAX_KEEP_DATABASE_VERSIONS + ")");
			return;
		}

		//// Now do the merge!
		logger.log(Level.INFO, "- Merge remote files: Merging necessary (" + ownDatabaseFilesMap.size() + " database files, max. " + MAX_KEEP_DATABASE_VERSIONS + ") ...");

		// 1. Determine files to delete remotely		
		List<DatabaseRemoteFile> toDeleteDatabaseFiles = new ArrayList<DatabaseRemoteFile>();
		int numOfDatabaseFilesToDelete = ownDatabaseFilesMap.size() - MIN_KEEP_DATABASE_VERSIONS;
		
		for (DatabaseRemoteFile ownDatabaseFile : ownDatabaseFilesMap.values()) {
			if (toDeleteDatabaseFiles.size() < numOfDatabaseFilesToDelete) {
				toDeleteDatabaseFiles.add(ownDatabaseFile);
			}			
		}				
		
		// 2. Write merge file
		DatabaseRemoteFile lastRemoteMergeDatabaseFile = toDeleteDatabaseFiles.get(toDeleteDatabaseFiles.size()-1);				
		File lastLocalMergeDatabaseFile = config.getCache().getDatabaseFile(lastRemoteMergeDatabaseFile.getName());

		logger.log(Level.INFO, "   + Writing new merge file (from {0}, to {1}) to {2} ...", new Object[] {
				toDeleteDatabaseFiles.get(0).getClientVersion(), lastRemoteMergeDatabaseFile.getClientVersion(), lastLocalMergeDatabaseFile });
		
		long lastLocalClientVersion = lastRemoteMergeDatabaseFile.getClientVersion();
		Iterator<DatabaseVersion> lastNDatabaseVersions = localDatabase.getDatabaseVersionsTo(config.getMachineName(), lastLocalClientVersion);
		
		XmlDatabaseSerializer databaseDAO = new XmlDatabaseSerializer(config.getTransformer());
		databaseDAO.save(lastNDatabaseVersions, lastLocalMergeDatabaseFile);
		
		// 3. Uploading merge file		

		// And delete others
		for (RemoteFile toDeleteRemoteFile : toDeleteDatabaseFiles) {
			logger.log(Level.INFO, "   + Deleting remote file " + toDeleteRemoteFile + " ...");
			transferManager.delete(toDeleteRemoteFile);
		}
		
		// TODO [high] TM cannot overwrite, might lead to chaos if operation does not finish, uploading the new merge file, this might happen often if new file is bigger!
		
		logger.log(Level.INFO, "   + Uploading new file {0} from local file {1} ...", new Object[] { lastRemoteMergeDatabaseFile, lastLocalMergeDatabaseFile });
		transferManager.delete(lastRemoteMergeDatabaseFile); 															
		transferManager.upload(lastLocalMergeDatabaseFile, lastRemoteMergeDatabaseFile);		
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
		private boolean mergeRemoteFiles = true;
		private boolean removeOldVersions = false;
		private int keepVersionsCount = 5;
		private boolean repackageMultiChunks = true;
		private double repackageUnusedThreshold = 0.7;

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
	
	public static class CleanupOperationResult implements OperationResult {
		// Nothing
	}
}
