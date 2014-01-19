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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersion.DatabaseVersionStatus;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.SqlDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.XmlDatabaseDao;
import org.syncany.operations.actions.FileCreatingFileSystemAction;
import org.syncany.operations.actions.FileSystemAction;
import org.syncany.operations.actions.FileSystemAction.InconsistentFileSystemException;
import org.syncany.util.FileUtil;

/**
 * The down operation implements a central part of Syncany's business logic. It determines
 * whether other clients have uploaded new changes, downloads and compares these changes to
 * the local database, and applies them locally. The down operation is the complement to the
 * {@link UpOperation}.
 * 
 * <p>The general operation flow is as follows:
 * <ol>
 *  <li>List all database versions on the remote storage using the {@link LsRemoteOperation}
 *      (implemented in {@link #listUnknownRemoteDatabases(MemoryDatabase, TransferManager) listUnknownRemoteDatabases()}</li>
 *  <li>Download unknown databases using a {@link TransferManager} (if any), skip the rest down otherwise
 *      (implemented in {@link #downloadUnknownRemoteDatabases(TransferManager, List) downloadUnknownRemoteDatabases()}</li>
 *  <li>Load remote database headers (branches) and compare them to the local database to determine a winner
 *      using several methods of the {@link DatabaseReconciliator}</li>
 *  <li>Determine whether the local branch conflicts with the winner branch; if so, prune conflicting
 *      local database versions (using {@link DatabaseReconciliator#findLosersPruneBranch(DatabaseBranch, DatabaseBranch)
 *      findLosersPruneBranch()})</li>
 *  <li>Determine whether the local branch needs to be updated (new database versions); if so, determine
 *      local {@link FileSystemAction}s</li>
 *  <li>Determine, download and decrypt required multi chunks from remote storage from file actions
 *      (implemented in {@link #determineMultiChunksToDownload(FileVersion, MemoryDatabase, MemoryDatabase) determineMultiChunksToDownload()},
 *      and {@link #downloadAndDecryptMultiChunks(Set) downloadAndDecryptMultiChunks()})</li>
 *  <li>Apply file system actions locally, creating conflict files where necessary if local file does
 *      not match the expected file (implemented in {@link #applyFileSystemActions(List) applyFileSystemActions()} </li>
 *  <li>Save local database and update known database list (database files that do not need to be 
 *      downloaded anymore</li>  
 * </ol>
 *     
 * @see DatabaseReconciliator
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DownOperation extends Operation {
	private static final Logger logger = Logger.getLogger(DownOperation.class.getSimpleName());
	
	private DownOperationOptions options;
	private DownOperationResult result;

	private SqlDatabase localDatabase;
	private DatabaseBranch localBranch;
	private TransferManager transferManager;
	private DatabaseReconciliator databaseReconciliator;

	public DownOperation(Config config) {
		this(config, new DownOperationOptions());
	}

	public DownOperation(Config config, DownOperationOptions options) {
		super(config);

		this.options = options;
		this.result = new DownOperationResult();
		this.localDatabase = new SqlDatabase(config);
		this.transferManager = config.getConnection().createTransferManager();
		this.databaseReconciliator = new DatabaseReconciliator();

	}

	@Override
	public DownOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync down' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		// Check strategies
		if (options.getConflictStrategy() != DownConflictStrategy.AUTO_RENAME) {
			logger.log(Level.INFO, "Conflict strategy "+options.getConflictStrategy()+" not yet implemented.");
			result.setResultCode(DownResultCode.NOK);
			
			return result;
		}
		
		// 0. Load database and create TM
		localBranch = localDatabase.getLocalDatabaseBranch();

		// 1. Check which remote databases to download based on the last local vector clock
		List<DatabaseRemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(transferManager);

		if (unknownRemoteDatabases.isEmpty()) {
			logger.log(Level.INFO, "* Nothing new. Skipping down operation.");
			result.setResultCode(DownResultCode.OK_NO_REMOTE_CHANGES);

			disconnectTransferManager();

			return result;
		}

		// 2. Download the remote databases to the local cache folder
		List<File> unknownRemoteDatabasesInCache = downloadUnknownRemoteDatabases(transferManager, unknownRemoteDatabases);

		// 3. Read version headers (vector clocks)
		DatabaseBranches unknownRemoteBranches = readUnknownDatabaseVersionHeaders(unknownRemoteDatabasesInCache);

		// 4. Determine winner branch
		DatabaseBranch winnersBranch = determineWinnerBranch(unknownRemoteBranches);
		logger.log(Level.INFO, "We have a winner! Now determine what to do locally ...");

		// 5. Prune local stuff (if local conflicts exist)
		pruneConflictingLocalBranch(winnersBranch);

		// 6. Apply winner's branch
		applyWinnersBranch(winnersBranch, unknownRemoteDatabasesInCache);

		// 7. Write names of newly analyzed remote databases (so we don't download them again)
		localDatabase.persistNewKnownRemoteDatabases(unknownRemoteDatabases);

		disconnectTransferManager();

		logger.log(Level.INFO, "Sync down done.");
		return result;
	}

	private void applyWinnersBranch(DatabaseBranch winnersBranch, List<File> unknownRemoteDatabasesInCache) throws Exception {
		DatabaseBranch winnersApplyBranch = databaseReconciliator.findWinnersApplyBranch(localBranch, winnersBranch);
		logger.log(Level.INFO, "- Database versions to APPLY locally: " + winnersApplyBranch);

		if (winnersApplyBranch.size() == 0) {
			logger.log(Level.WARNING, "  + Nothing to update. Nice!");
			result.setResultCode(DownResultCode.OK_NO_REMOTE_CHANGES);
		}
		else {
			logger.log(Level.INFO, "- Loading winners database ...");
			MemoryDatabase winnersDatabase = readWinnersDatabase(winnersApplyBranch, unknownRemoteDatabasesInCache);

			FileSystemActionReconciliator actionReconciliator = new FileSystemActionReconciliator(config, result);
			List<FileSystemAction> actions = actionReconciliator.determineFileSystemActions(winnersDatabase);

			Set<MultiChunkEntry> unknownMultiChunks = determineRequiredMultiChunks(actions, winnersDatabase);
			downloadAndDecryptMultiChunks(unknownMultiChunks);

			applyFileSystemActions(actions);

			// Add winners database to local database
			// Note: This must happen AFTER the file system stuff, because we compare the winners database with the local database!			
			logger.log(Level.INFO, "   Adding database versions to SQL database ...");
			
			for (DatabaseVersionHeader applyDatabaseVersionHeader : winnersApplyBranch.getAll()) {
				logger.log(Level.INFO, "   + Applying database version " + applyDatabaseVersionHeader.getVectorClock());

				DatabaseVersion applyDatabaseVersion = winnersDatabase.getDatabaseVersion(applyDatabaseVersionHeader.getVectorClock());				
				localDatabase.persistDatabaseVersion(applyDatabaseVersion);
			}

			result.setResultCode(DownResultCode.OK_WITH_REMOTE_CHANGES);
		}
	}

	private void pruneConflictingLocalBranch(DatabaseBranch winnersBranch) throws Exception {
		DatabaseBranch localPruneBranch = databaseReconciliator.findLosersPruneBranch(localBranch, winnersBranch);
		logger.log(Level.INFO, "- Database versions to REMOVE locally: " + localPruneBranch);

		if (localPruneBranch.size() == 0) {
			logger.log(Level.INFO, "  + Nothing to prune locally. No conflicts. Only updates. Nice!");
		}
		else {
			// Load dirty database (if existent)
			logger.log(Level.INFO, "  + Marking databases as DIRTY locally ...");

			for (DatabaseVersionHeader databaseVersionHeader : localPruneBranch.getAll()) {
				logger.log(Level.INFO, "    * MASTER->DIRTY: "+databaseVersionHeader);
				localDatabase.markDatabaseVersion(databaseVersionHeader, DatabaseVersionStatus.DIRTY);
			
				String remoteFileToPruneClientName = config.getMachineName();
				long remoteFileToPruneVersion = databaseVersionHeader.getVectorClock().getClock(config.getMachineName());
				DatabaseRemoteFile remoteFileToPrune = new DatabaseRemoteFile(remoteFileToPruneClientName, remoteFileToPruneVersion);

				logger.log(Level.INFO, "    * Deleting remote database file " + remoteFileToPrune + " ...");
				transferManager.delete(remoteFileToPrune);
			}
		}
	}

	/**
	 * This method uses the {@link DatabaseReconciliator} to compare the local database with the 
	 * downloaded remote databases, in order to determine a winner. The winner's database versions
	 * will be applied locally.
	 * 
	 * <p>For the comparison, the {@link DatabaseVersionHeader}s (mainly the {@link VectorClock}) of each
	 * database version are compared. Using these vector clocks, the underlying algorithms determine  
	 * potential conflicts (between database versions, = simultaneous vector clocks), and resolve these 
	 * conflicts by comparing local timestamps. 
	 * 
	 * <p>The detailed algorithm is described in the {@link DatabaseReconciliator}.
	 * 
	 * @param localDatabase The local database (to be compared with the remote databases)
	 * @param unknownRemoteBranches The newly downloaded remote database version headers (= branches)
	 * @return Returns the branch of the winner 
	 * @throws Exception If any kind of error occurs (...)
	 */
	private DatabaseBranch determineWinnerBranch(DatabaseBranches unknownRemoteBranches) throws Exception {
		logger.log(Level.INFO, "Detect updates and conflicts ...");
		DatabaseReconciliator databaseReconciliator = new DatabaseReconciliator();

		logger.log(Level.INFO, "- Stitching branches ...");
		DatabaseBranches allStitchedBranches = databaseReconciliator.stitchBranches(unknownRemoteBranches, config.getMachineName(), localBranch);

		DatabaseVersionHeader lastCommonHeader = databaseReconciliator.findLastCommonDatabaseVersionHeader(localBranch, allStitchedBranches);
		TreeMap<String, DatabaseVersionHeader> firstConflictingHeaders = databaseReconciliator.findFirstConflictingDatabaseVersionHeader(
				lastCommonHeader, allStitchedBranches);
		TreeMap<String, DatabaseVersionHeader> winningFirstConflictingHeaders = databaseReconciliator
				.findWinningFirstConflictingDatabaseVersionHeaders(firstConflictingHeaders);
		Entry<String, DatabaseVersionHeader> winnersWinnersLastDatabaseVersionHeader = databaseReconciliator
				.findWinnersWinnersLastDatabaseVersionHeader(winningFirstConflictingHeaders, allStitchedBranches);

		if (logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "- Database reconciliation results:");
			logger.log(Level.FINEST, "  + localBranch: " + localBranch);
			logger.log(Level.FINEST, "  + unknownRemoteBranches: " + unknownRemoteBranches);
			// logger.log(Level.FINEST, "  + allStitchedBranches: "+allStitchedBranches);
			logger.log(Level.FINEST, "  + lastCommonHeader: " + lastCommonHeader);
			logger.log(Level.FINEST, "  + firstConflictingHeaders: " + firstConflictingHeaders);
			logger.log(Level.FINEST, "  + winningFirstConflictingHeaders: " + winningFirstConflictingHeaders);
			logger.log(Level.FINEST, "  + winnersWinnersLastDatabaseVersionHeader: " + winnersWinnersLastDatabaseVersionHeader);
		}

		String winnersName = winnersWinnersLastDatabaseVersionHeader.getKey();
		DatabaseBranch winnersBranch = allStitchedBranches.getBranch(winnersName);

		logger.log(Level.INFO, "- Compared branches: " + allStitchedBranches);
		logger.log(Level.INFO, "- Winner is " + winnersName + " with branch " + winnersBranch);

		return winnersBranch;
	}

	private Set<MultiChunkEntry> determineRequiredMultiChunks(List<FileSystemAction> actions, MemoryDatabase winnersDatabase) {
		Set<MultiChunkEntry> multiChunksToDownload = new HashSet<MultiChunkEntry>();

		for (FileSystemAction action : actions) {
			if (action instanceof FileCreatingFileSystemAction) { // TODO [low] This adds ALL multichunks even though some might be available locally
				multiChunksToDownload.addAll(determineMultiChunksToDownload(action.getFile2(), winnersDatabase));
			}
		}

		return multiChunksToDownload;
	}

	private Collection<MultiChunkEntry> determineMultiChunksToDownload(FileVersion fileVersion, MemoryDatabase winnersDatabase) {
		Set<MultiChunkEntry> multiChunksToDownload = new HashSet<MultiChunkEntry>();

		// First: Check if we know this file locally!
		List<MultiChunkEntry> multiChunkEntries = localDatabase.getMultiChunksWithoutChunkChecksums(fileVersion.getChecksum());
		
		if (multiChunkEntries.size() > 0) {
			multiChunksToDownload.addAll(multiChunkEntries);
		}
		else {
			// Second: We don't know it locally; must be from the winners database
			FileContent winningFileContent = winnersDatabase.getContent(fileVersion.getChecksum());			
			boolean winningFileHasContent = winningFileContent != null;

			if (winningFileHasContent) { // File can be empty!
				Collection<ChunkChecksum> fileChunks = winningFileContent.getChunks(); 
				
				// TODO [medium] Instead of just looking for multichunks to download here, we should look for chunks in local files as well
				// and return the chunk positions in the local files ChunkPosition (chunk123 at file12, offset 200, size 250)

				for (ChunkChecksum chunkChecksum : fileChunks) {
					MultiChunkEntry multiChunkForChunk = localDatabase.getMultiChunkWithoutChunkChecksums(chunkChecksum);
					// TODO [high] Performance: This queries the database for every chunk, SLOWWW!
					
					if (multiChunkForChunk == null) {
						multiChunkForChunk = winnersDatabase.getMultiChunkForChunk(chunkChecksum);
					}

					// Check consistency!
					if (multiChunkForChunk == null) {
						throw new RuntimeException("Cannot find multichunk for chunk "+chunkChecksum);
					}
					
					if (!multiChunksToDownload.contains(multiChunkForChunk)) {
						logger.log(Level.INFO, "  + Adding multichunk " + multiChunkForChunk.getId() + " to download list ...");
						multiChunksToDownload.add(multiChunkForChunk);
					}
				}
			}
		}
		
		return multiChunksToDownload;
	}

	private List<FileSystemAction> sortFileSystemActions(List<FileSystemAction> actions) {
		FileSystemActionComparator actionComparator = new FileSystemActionComparator();
		actionComparator.sort(actions);

		return actions;
	}

	private void applyFileSystemActions(List<FileSystemAction> actions) throws Exception {
		// Sort
		actions = sortFileSystemActions(actions);

		logger.log(Level.FINER, "- Applying file system actions (sorted!) ...");

		// Apply
		for (FileSystemAction action : actions) {
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "   +  {0}", action);
			}

			try {
				action.execute();
			}
			catch (InconsistentFileSystemException e) {
				logger.log(Level.FINER, "     --> Inconsistent file system exception thrown. Ignoring for this file.", e);
			}
		}
	}

	private void downloadAndDecryptMultiChunks(Set<MultiChunkEntry> unknownMultiChunks) throws StorageException, IOException {
		logger.log(Level.INFO, "- Downloading and extracting multichunks ...");

		// TODO [medium] Check existing files by checksum and do NOT download them if they exist locally, or copy them

		for (MultiChunkEntry multiChunkEntry : unknownMultiChunks) {
			File localEncryptedMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId().getRaw());
			File localDecryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkEntry.getId().getRaw());
			MultiChunkRemoteFile remoteMultiChunkFile = new MultiChunkRemoteFile(multiChunkEntry.getId().getRaw());

			logger.log(Level.INFO, "  + Downloading multichunk " + multiChunkEntry.getId() + " ...");
			transferManager.download(remoteMultiChunkFile, localEncryptedMultiChunkFile);
			result.downloadedMultiChunks.add(multiChunkEntry);

			logger.log(Level.INFO, "  + Decrypting multichunk " + multiChunkEntry.getId() + " ...");
			InputStream multiChunkInputStream = config.getTransformer().createInputStream(new FileInputStream(localEncryptedMultiChunkFile));
			OutputStream decryptedMultiChunkOutputStream = new FileOutputStream(localDecryptedMultiChunkFile);

			// TODO [medium] Calculate checksum while writing file, to verify correct content
			FileUtil.appendToOutputStream(multiChunkInputStream, decryptedMultiChunkOutputStream);

			decryptedMultiChunkOutputStream.close();
			multiChunkInputStream.close();

			logger.log(Level.FINE, "  + Locally deleting multichunk " + multiChunkEntry.getId() + " ...");
			localEncryptedMultiChunkFile.delete();
		}

		transferManager.disconnect();
	}

	private MemoryDatabase readWinnersDatabase(DatabaseBranch winnersApplyBranch, List<File> remoteDatabases) throws IOException, StorageException {
		// Make map 'short filename' -> 'full filename'
		Map<String, File> shortFilenameToFileMap = new HashMap<String, File>();

		for (File remoteDatabase : remoteDatabases) {
			shortFilenameToFileMap.put(remoteDatabase.getName(), remoteDatabase);
		}

		// Load individual databases for branch ranges
		XmlDatabaseDao databaseDAO = new XmlDatabaseDao(config.getTransformer());
		MemoryDatabase winnerBranchDatabase = new MemoryDatabase(); // Database cannot be reused, since these might be different clients

		String clientName = null;
		VectorClock clientVersionFrom = null;
		VectorClock clientVersionTo = null;

		for (DatabaseVersionHeader databaseVersionHeader : winnersApplyBranch.getAll()) {
			// First of range for this client
			if (clientName == null || !clientName.equals(databaseVersionHeader.getClient())) {
				clientName = databaseVersionHeader.getClient();
				clientVersionFrom = databaseVersionHeader.getVectorClock();
				clientVersionTo = databaseVersionHeader.getVectorClock();
			}

			// Still in range for this client
			else if (clientName.equals(databaseVersionHeader.getClient())) {
				clientVersionTo = databaseVersionHeader.getVectorClock();
			}

			DatabaseRemoteFile potentialDatabaseRemoteFileForRange = new DatabaseRemoteFile(clientName, clientVersionTo.getClock(clientName));
			File databaseFileForRange = shortFilenameToFileMap.get(potentialDatabaseRemoteFileForRange.getName());

			if (databaseFileForRange != null) {
				// Load database
				logger.log(Level.INFO, "- Loading " + databaseFileForRange + " (from " + clientVersionFrom + ", to " + clientVersionTo + ") ...");
				databaseDAO.load(winnerBranchDatabase, databaseFileForRange, clientVersionFrom, clientVersionTo);

				// Reset range
				clientName = null;
				clientVersionFrom = null;
				clientVersionTo = null;
			}
		}

		return winnerBranchDatabase;
	}

	private DatabaseBranches readUnknownDatabaseVersionHeaders(List<File> remoteDatabases) throws IOException, StorageException {
		logger.log(Level.INFO, "Loading database headers, creating branches ...");

		// Sort files (db-a-1 must be before db-a-2 !)
		Collections.sort(remoteDatabases);

		// Read database files
		DatabaseBranches unknownRemoteBranches = new DatabaseBranches();
		XmlDatabaseDao dbDAO = new XmlDatabaseDao(config.getTransformer());

		for (File remoteDatabaseFileInCache : remoteDatabases) {
			MemoryDatabase remoteDatabase = new MemoryDatabase(); // Database cannot be reused, since these might be different clients

			DatabaseRemoteFile remoteDatabaseFile = new DatabaseRemoteFile(remoteDatabaseFileInCache.getName());
			dbDAO.load(remoteDatabase, remoteDatabaseFileInCache, true); // only load headers!
			List<DatabaseVersion> remoteDatabaseVersions = remoteDatabase.getDatabaseVersions();

			// Populate branches
			DatabaseBranch remoteClientBranch = unknownRemoteBranches.getBranch(remoteDatabaseFile.getClientName(), true);

			for (DatabaseVersion remoteDatabaseVersion : remoteDatabaseVersions) {
				DatabaseVersionHeader header = remoteDatabaseVersion.getHeader();
				remoteClientBranch.add(header);
			}
		}

		return unknownRemoteBranches;
	}

	private List<DatabaseRemoteFile> listUnknownRemoteDatabases(TransferManager transferManager) throws Exception {
		return (new LsRemoteOperation(config, transferManager).execute()).getUnknownRemoteDatabases();
	}

	private List<File> downloadUnknownRemoteDatabases(TransferManager transferManager, List<DatabaseRemoteFile> unknownRemoteDatabases)
			throws StorageException {
		logger.log(Level.INFO, "Downloading unknown databases.");
		List<File> unknownRemoteDatabasesInCache = new ArrayList<File>();

		for (DatabaseRemoteFile remoteFile : unknownRemoteDatabases) {
			File unknownRemoteDatabaseFileInCache = config.getCache().getDatabaseFile(remoteFile.getName());

			logger.log(Level.INFO, "- Downloading {0} to local cache at {1}", new Object[] { remoteFile.getName(), unknownRemoteDatabaseFileInCache });
			transferManager.download(new DatabaseRemoteFile(remoteFile.getName()), unknownRemoteDatabaseFileInCache);

			unknownRemoteDatabasesInCache.add(unknownRemoteDatabaseFileInCache);
			result.getDownloadedUnknownDatabases().add(remoteFile.getName());
		}

		return unknownRemoteDatabasesInCache;
	}

	private void disconnectTransferManager() {
		try {
			transferManager.disconnect();
		}
		catch (StorageException e) {
			// Don't care!
		}
	}

	public enum DownConflictStrategy {
		AUTO_RENAME, ASK_USER
	}
	
	public static class DownOperationOptions implements OperationOptions {
		private DownConflictStrategy conflictStrategy = DownConflictStrategy.AUTO_RENAME;

		public DownConflictStrategy getConflictStrategy() {
			return conflictStrategy;
		}

		public void setConflictStrategy(DownConflictStrategy conflictStrategy) {
			this.conflictStrategy = conflictStrategy;
		}				
	}

	public enum DownResultCode {
		OK_NO_REMOTE_CHANGES, OK_WITH_REMOTE_CHANGES, NOK
	};

	public static class DownOperationResult implements OperationResult {
		private DownResultCode resultCode;
		private ChangeSet changeSet = new ChangeSet();
		private Set<String> downloadedUnknownDatabases = new HashSet<String>();
		private Set<MultiChunkEntry> downloadedMultiChunks = new HashSet<MultiChunkEntry>();

		public DownResultCode getResultCode() {
			return resultCode;
		}

		public void setResultCode(DownResultCode resultCode) {
			this.resultCode = resultCode;
		}

		public void setChangeSet(ChangeSet ChangeSet) {
			this.changeSet = ChangeSet;
		}

		public ChangeSet getChangeSet() {
			return changeSet;
		}

		public Set<String> getDownloadedUnknownDatabases() {
			return downloadedUnknownDatabases;
		}

		public void setDownloadedUnknownDatabases(Set<String> downloadedUnknownDatabases) {
			this.downloadedUnknownDatabases = downloadedUnknownDatabases;
		}

		public Set<MultiChunkEntry> getDownloadedMultiChunks() {
			return downloadedMultiChunks;
		}

		public void setDownloadedMultiChunks(Set<MultiChunkEntry> downloadedMultiChunks) {
			this.downloadedMultiChunks = downloadedMultiChunks;
		}
	}
}
