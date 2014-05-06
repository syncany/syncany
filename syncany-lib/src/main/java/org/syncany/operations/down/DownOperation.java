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
package org.syncany.operations.down;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.DatabaseVersionHeader.DatabaseVersionType;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.operations.AbstractTransferOperation;
import org.syncany.operations.down.DownOperationOptions.DownConflictStrategy;
import org.syncany.operations.down.DownOperationResult.DownResultCode;
import org.syncany.operations.down.actions.FileCreatingFileSystemAction;
import org.syncany.operations.down.actions.FileSystemAction;
import org.syncany.operations.down.actions.FileSystemAction.InconsistentFileSystemException;
import org.syncany.operations.ls_remote.LsRemoteOperation;
import org.syncany.operations.up.UpOperation;
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
public class DownOperation extends AbstractTransferOperation {
	private static final Logger logger = Logger.getLogger(DownOperation.class.getSimpleName());
	
	private DownOperationOptions options;
	private DownOperationResult result;

	private SqlDatabase localDatabase;
	private DatabaseBranch localBranch;
	private DatabaseReconciliator databaseReconciliator;
	private DownOperationListener listener;
	
	public DownOperation(Config config) {
		this(config, new DownOperationOptions(), null);
	}
	
	public DownOperation(Config config, DownOperationListener listener) {
		this(config, new DownOperationOptions(), listener);
	}

	public DownOperation(Config config, DownOperationOptions options, DownOperationListener listener) {
		super(config, "down");

		this.options = options;
		this.result = new DownOperationResult();
		this.listener = listener;

		this.localDatabase = new SqlDatabase(config);
		this.databaseReconciliator = new DatabaseReconciliator();
	}

	@Override
	public DownOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync down' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		// Check strategies
		if (options.getConflictStrategy() != DownConflictStrategy.RENAME) {
			logger.log(Level.INFO, "Conflict strategy "+options.getConflictStrategy()+" not yet implemented.");
			result.setResultCode(DownResultCode.NOK);
			
			return result;
		}
		
		// 0. Load database and create TM
		localBranch = localDatabase.getLocalDatabaseBranch();
		
		// 1. Upload action file 
		startOperation();

		// 2. Check which remote databases to download based on the last local vector clock
		List<DatabaseRemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(transferManager);

		if (unknownRemoteDatabases.isEmpty()) {
			logger.log(Level.INFO, "* Nothing new. Skipping down operation.");
			result.setResultCode(DownResultCode.OK_NO_REMOTE_CHANGES);

			finishOperation();
			return result;
		}

		// 3. Download the remote databases to the local cache folder
		TreeMap<File, DatabaseRemoteFile> unknownRemoteDatabasesInCache = downloadUnknownRemoteDatabases(transferManager, unknownRemoteDatabases);

		// 4. Read version headers (vector clocks)
		DatabaseBranches unknownRemoteBranches = readUnknownDatabaseVersionHeaders(unknownRemoteDatabasesInCache);

		// 5. Determine winner branch
		DatabaseBranch winnersBranch = determineWinnerBranch(unknownRemoteBranches);
		logger.log(Level.INFO, "We have a winner! Now determine what to do locally ...");

		// 6. Prune local stuff (if local conflicts exist)
		purgeConflictingLocalBranch(winnersBranch);

		// 7. Apply winner's branch
		applyWinnersBranch(winnersBranch, unknownRemoteDatabasesInCache);

		// 8. Write names of newly analyzed remote databases (so we don't download them again)
		localDatabase.writeKnownRemoteDatabases(unknownRemoteDatabases);

		finishOperation();

		logger.log(Level.INFO, "Sync down done.");
		return result;
	}

	private void applyWinnersBranch(DatabaseBranch winnersBranch, TreeMap<File, DatabaseRemoteFile> unknownRemoteDatabases) throws Exception {
		DatabaseBranch winnersApplyBranch = databaseReconciliator.findWinnersApplyBranch(localBranch, winnersBranch);
		logger.log(Level.INFO, "Database versions to APPLY locally: " + winnersApplyBranch);

		if (winnersApplyBranch.size() == 0) {
			logger.log(Level.WARNING, "  + Nothing to update. Nice!");
			result.setResultCode(DownResultCode.OK_NO_REMOTE_CHANGES);
		}
		else {
			logger.log(Level.INFO, "Loading winners database (DEFAULT) ...");			
			MemoryDatabase winnersDatabase = readWinnersDatabase(winnersApplyBranch, unknownRemoteDatabases, DatabaseVersionType.DEFAULT);

			logger.log(Level.INFO, "Loading winners database (PURGE) ...");			
			MemoryDatabase winnersPurgeDatabase = readWinnersDatabase(winnersApplyBranch, unknownRemoteDatabases, DatabaseVersionType.PURGE);

			logger.log(Level.INFO, "Determine file system actions ...");			
			FileSystemActionReconciliator actionReconciliator = new FileSystemActionReconciliator(config, result);
			List<FileSystemAction> actions = actionReconciliator.determineFileSystemActions(winnersDatabase);

			Set<MultiChunkId> unknownMultiChunks = determineRequiredMultiChunks(actions, winnersDatabase);
			downloadAndDecryptMultiChunks(unknownMultiChunks);

			applyFileSystemActions(actions);
			applyDatabaseVersions(winnersApplyBranch, winnersDatabase, winnersPurgeDatabase);
			//applyPurgeDatabaseVersions(winnersPurgeDatabase);			

			result.setResultCode(DownResultCode.OK_WITH_REMOTE_CHANGES);
		}
	}

	private void applyDatabaseVersions(DatabaseBranch winnersApplyBranch, MemoryDatabase winnersDatabase, MemoryDatabase winnersPurgeDatabase) throws SQLException {
		// Add winners database to local database
		// Note: This must happen AFTER the file system stuff, because we compare the winners database with the local database!			
		logger.log(Level.INFO, "- Adding database versions to SQL database ...");
		
		for (DatabaseVersionHeader currentDatabaseVersionHeader : winnersApplyBranch.getAll()) {
			if (currentDatabaseVersionHeader.getType() == DatabaseVersionType.DEFAULT) {
				persistDatabaseVersion(winnersDatabase, currentDatabaseVersionHeader);				
			}
			else if (currentDatabaseVersionHeader.getType() == DatabaseVersionType.PURGE) {
				persistPurgeDatabaseVesion(winnersPurgeDatabase, currentDatabaseVersionHeader);					
			}
			else {
				throw new RuntimeException("Unknow database version type: " + currentDatabaseVersionHeader.getType());
			}
		}
	}

	private void persistPurgeDatabaseVesion(MemoryDatabase winnersPurgeDatabase, DatabaseVersionHeader currentDatabaseVersionHeader) throws SQLException {
		logger.log(Level.INFO, "  + Applying PURGE database version " + currentDatabaseVersionHeader.getVectorClock());

		DatabaseVersion purgeDatabaseVersion = winnersPurgeDatabase.getDatabaseVersion(currentDatabaseVersionHeader.getVectorClock());
		Map<FileHistoryId, FileVersion> purgeFileVersions = new HashMap<FileHistoryId, FileVersion>();
		
		for (PartialFileHistory purgeFileHistory : purgeDatabaseVersion.getFileHistories()) {
			logger.log(Level.INFO, "     - Purging file history {0}, with versions <= {1}", new Object[] { 
					purgeFileHistory.getFileHistoryId().toString(), purgeFileHistory.getLastVersion() });
			
			purgeFileVersions.put(purgeFileHistory.getFileHistoryId(), purgeFileHistory.getLastVersion());				
		}
		
		localDatabase.removeSmallerOrEqualFileVersions(purgeFileVersions);
		localDatabase.removeDeletedFileVersions();  
		localDatabase.removeUnreferencedDatabaseEntities();
		localDatabase.writeDatabaseVersionHeader(purgeDatabaseVersion.getHeader());		
		
		localDatabase.commit(); // TODO [medium] Harmonize commit behavior		
	}

	private void persistDatabaseVersion(MemoryDatabase winnersDatabase, DatabaseVersionHeader currentDatabaseVersionHeader) {
		logger.log(Level.INFO, "  + Applying database version " + currentDatabaseVersionHeader.getVectorClock());

		DatabaseVersion applyDatabaseVersion = winnersDatabase.getDatabaseVersion(currentDatabaseVersionHeader.getVectorClock());				
		localDatabase.persistDatabaseVersion(applyDatabaseVersion);
	}

	private void purgeConflictingLocalBranch(DatabaseBranch winnersBranch) throws Exception {
		DatabaseBranch localPurgeBranch = databaseReconciliator.findLosersPruneBranch(localBranch, winnersBranch);
		logger.log(Level.INFO, "- Database versions to REMOVE locally: " + localPurgeBranch);

		if (localPurgeBranch.size() == 0) {
			logger.log(Level.INFO, "  + Nothing to prune locally. No conflicts. Only updates. Nice!");
		}
		else {
			// Load dirty database (if existent)
			logger.log(Level.INFO, "  + Marking databases as DIRTY locally ...");

			for (DatabaseVersionHeader databaseVersionHeader : localPurgeBranch.getAll()) {
				logger.log(Level.INFO, "    * MASTER->DIRTY: "+databaseVersionHeader);
				localDatabase.markDatabaseVersionDirty(databaseVersionHeader.getVectorClock());
			
				String remoteFileToPruneClientName = config.getMachineName();
				long remoteFileToPruneVersion = databaseVersionHeader.getVectorClock().getClock(config.getMachineName());
				DatabaseRemoteFile remoteFileToPrune = new DatabaseRemoteFile(remoteFileToPruneClientName, remoteFileToPruneVersion);

				logger.log(Level.INFO, "    * Deleting remote database file " + remoteFileToPrune + " ...");
				transferManager.delete(remoteFileToPrune);		
				
				result.getDirtyDatabasesCreated().add(databaseVersionHeader);
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
		DatabaseBranch winnersBranch = databaseReconciliator.findWinnerBranch(config.getMachineName(), localBranch, unknownRemoteBranches);

		return winnersBranch;
	}

	private Set<MultiChunkId> determineRequiredMultiChunks(List<FileSystemAction> actions, MemoryDatabase winnersDatabase) {
		Set<MultiChunkId> multiChunksToDownload = new HashSet<MultiChunkId>();

		for (FileSystemAction action : actions) {
			if (action instanceof FileCreatingFileSystemAction) { // TODO [low] This adds ALL multichunks even though some might be available locally
				multiChunksToDownload.addAll(determineMultiChunksToDownload(action.getFile2(), winnersDatabase));
			}
		}

		return multiChunksToDownload;
	}

	private Collection<MultiChunkId> determineMultiChunksToDownload(FileVersion fileVersion, MemoryDatabase winnersDatabase) {
		Set<MultiChunkId> multiChunksToDownload = new HashSet<MultiChunkId>();

		// First: Check if we know this file locally!
		List<MultiChunkId> multiChunkIds = localDatabase.getMultiChunkIds(fileVersion.getChecksum());
		
		if (multiChunkIds.size() > 0) {
			multiChunksToDownload.addAll(multiChunkIds);
		}
		else {
			// Second: We don't know it locally; must be from the winners database
			FileContent winningFileContent = winnersDatabase.getContent(fileVersion.getChecksum());			
			boolean winningFileHasContent = winningFileContent != null;

			if (winningFileHasContent) { // File can be empty!
				List<ChunkChecksum> fileChunks = winningFileContent.getChunks(); 
				
				// TODO [medium] Instead of just looking for multichunks to download here, we should look for chunks in local files as well
				// and return the chunk positions in the local files ChunkPosition (chunk123 at file12, offset 200, size 250)
				
				Map<ChunkChecksum, MultiChunkId> checksumsWithMultiChunkIds = localDatabase.getMultiChunkIdsByChecksums(fileChunks);
				
				for (ChunkChecksum chunkChecksum : fileChunks) {
					MultiChunkId multiChunkIdForChunk = checksumsWithMultiChunkIds.get(chunkChecksum);
					if (multiChunkIdForChunk == null) {
						multiChunkIdForChunk = winnersDatabase.getMultiChunkIdForChunk(chunkChecksum);
						
						if (multiChunkIdForChunk == null) {
							throw new RuntimeException("Cannot find multichunk for chunk "+chunkChecksum);	
						}
					}
					
					if (!multiChunksToDownload.contains(multiChunkIdForChunk)) {
						logger.log(Level.INFO, "  + Adding multichunk " + multiChunkIdForChunk + " to download list ...");
						multiChunksToDownload.add(multiChunkIdForChunk);
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

	private void downloadAndDecryptMultiChunks(Set<MultiChunkId> unknownMultiChunkIds) throws StorageException, IOException {
		logger.log(Level.INFO, "Downloading and extracting multichunks ...");

		// TODO [medium] Check existing files by checksum and do NOT download them if they exist locally, or copy them

		for (MultiChunkId multiChunkId : unknownMultiChunkIds) {
			File localEncryptedMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkId);
			File localDecryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkId);
			MultiChunkRemoteFile remoteMultiChunkFile = new MultiChunkRemoteFile(multiChunkId);

			logger.log(Level.INFO, "  + Downloading multichunk " + multiChunkId + " ...");
			transferManager.download(remoteMultiChunkFile, localEncryptedMultiChunkFile);
			result.getDownloadedMultiChunks().add(multiChunkId);

			logger.log(Level.INFO, "  + Decrypting multichunk " + multiChunkId + " ...");
			InputStream multiChunkInputStream = config.getTransformer().createInputStream(new FileInputStream(localEncryptedMultiChunkFile));
			OutputStream decryptedMultiChunkOutputStream = new FileOutputStream(localDecryptedMultiChunkFile);

			// TODO [medium] Calculate checksum while writing file, to verify correct content
			FileUtil.appendToOutputStream(multiChunkInputStream, decryptedMultiChunkOutputStream);

			decryptedMultiChunkOutputStream.close();
			multiChunkInputStream.close();

			logger.log(Level.FINE, "  + Locally deleting multichunk " + multiChunkId + " ...");
			localEncryptedMultiChunkFile.delete();
		}

		transferManager.disconnect();
	}

	/**
	 * Loads the winner's database branch into the memory in a {@link MemoryDatabase} object, by using
	 * the already downloaded list of remote database files.
	 * 
	 * <p>Because database files can contain multiple {@link DatabaseVersion}s per client, a range for which
	 * to load the database versions must be determined.
	 * 
	 * <p><b>Example 1:</b><br />
	 * <pre>
	 *  db-A-0001   (A1)     Already known             Not loaded 
	 *  db-A-0005   (A2)     Already known             Not loaded 
	 *              (A3)     Already known             Not loaded 
	 *              (A4)     Part of winner's branch   Loaded
	 *              (A5)     Purge database version    Ignored (only DEFAULT)
	 *  db-B-0001   (A5,B1)  Part of winner's branch   Loaded
	 *  db-A-0006   (A6,B1)  Part of winner's branch   Loaded
	 * </pre>
	 * 
	 * <p>In example 1, only (A4)-(A5) must be loaded from db-A-0005, and not all four database versions.
	 * 
	 * <p><b>Other example:</b><br />
	 * <pre>
	 *  db-A-0005   (A1)     Part of winner's branch   Loaded 
	 *  db-A-0005   (A2)     Part of winner's branch   Loaded 
	 *  db-B-0001   (A2,B1)  Part of winner's branch   Loaded
	 *  db-A-0005   (A3,B1)  Part of winner's branch   Loaded 
	 *  db-A-0005   (A4,B1)  Part of winner's branch   Loaded
	 *  db-A-0005   (A5,B1)  Purge database version    Ignored (only DEFAULT)
	 * </pre>
	 * 
	 * <p>In example 2, (A1)-(A5,B1) [except (A2,B1)] are contained in db-A-0005 (after merging!), so 
	 * db-A-0005 must be processed twice; each time loading separate parts of the file. In this case:
	 * First load (A1)-(A2) from db-A-0005, then load (A2,B1) from db-B-0001, then load (A3,B1)-(A4,B1)
	 * from db-A-0005, and ignore (A5,B1).
	 * 
	 * @return Returns a loaded memory database containing all metadata from the winner's branch 
	 */
	private MemoryDatabase readWinnersDatabase(DatabaseBranch winnersApplyBranch, TreeMap<File, DatabaseRemoteFile> unknownRemoteDatabases, DatabaseVersionType filterType) throws IOException, StorageException {
		// Make map 'short filename' -> 'full filename'
		Map<String, File> shortFilenameToFileMap = new HashMap<String, File>();

		for (File remoteDatabase : unknownRemoteDatabases.keySet()) {
			shortFilenameToFileMap.put(remoteDatabase.getName(), remoteDatabase);
		}

		// Load individual databases for branch ranges
		DatabaseXmlSerializer xmlDatabaseSerializer = new DatabaseXmlSerializer(config.getTransformer());
		MemoryDatabase winnerBranchDatabase = new MemoryDatabase(); // Database cannot be reused, since these might be different clients

		List<DatabaseVersionHeader> winnersApplyBranchList = winnersApplyBranch.getAll();
		
		String rangeClientName = null;
		VectorClock rangeVersionFrom = null;
		VectorClock rangeVersionTo = null;

		for (int i=0; i<winnersApplyBranchList.size(); i++) {
			DatabaseVersionHeader currentDatabaseVersionHeader = winnersApplyBranchList.get(i);
			DatabaseVersionHeader nextDatabaseVersionHeader = (i+1 < winnersApplyBranchList.size()) ? winnersApplyBranchList.get(i+1) : null;
			
			// First of range for this client
			if (rangeClientName == null) {
				rangeClientName = currentDatabaseVersionHeader.getClient();
				rangeVersionFrom = currentDatabaseVersionHeader.getVectorClock();
				rangeVersionTo = currentDatabaseVersionHeader.getVectorClock();
			}
			
			// Still in range for this client
			else {
				rangeVersionTo = currentDatabaseVersionHeader.getVectorClock();
			}
			
			// Now load this stuff from the database file (or not)
			//   - If the database file exists, load the range and reset it
			//   - If not, only force a load if this is the range end
			
			File databaseVersionFile = getExactDatabaseVersionFile(currentDatabaseVersionHeader, shortFilenameToFileMap);
						
			if (databaseVersionFile != null) {
				xmlDatabaseSerializer.load(winnerBranchDatabase, databaseVersionFile, rangeVersionFrom, rangeVersionTo, filterType);				
				rangeClientName = null;
			}
			else {
				boolean lastDatabaseVersionHeader = nextDatabaseVersionHeader == null;
				boolean nextClientIsDifferent = !lastDatabaseVersionHeader && !currentDatabaseVersionHeader.getClient().equals(nextDatabaseVersionHeader.getClient());
				boolean rangeEnds = lastDatabaseVersionHeader || nextClientIsDifferent;

				if (rangeEnds) {
					databaseVersionFile = getNextDatabaseVersionFile(currentDatabaseVersionHeader, shortFilenameToFileMap);
					
					xmlDatabaseSerializer.load(winnerBranchDatabase, databaseVersionFile, rangeVersionFrom, rangeVersionTo, filterType);					
					rangeClientName = null;
				}
			}
		}
	
		return winnerBranchDatabase;
	}
	
	/**
	 * Returns the database file for a given database version header, or <tt>null</tt> 
	 * if for this database version header no file has been downloaded.
	 * 
	 * <p>Unlike {@link #getNextDatabaseVersionFile(DatabaseVersionHeader, Map) getNextDatabaseVersionFile()},
	 * this method does <b>not</b> try to find a database file by counting up the local version. It returns
	 * null if the exact version has not been found!
	 * 
	 * <p><b>Example:</b> given database version header is A/(A3,B2)/T=..
	 * <pre>
	 *   - Does db-A-0003 exist? No, return null.
	 * </pre>
	 */
	private File getExactDatabaseVersionFile(DatabaseVersionHeader currentDatabaseVersionHeader, Map<String, File> shortFilenameToFileMap) throws StorageException {
		String clientName = currentDatabaseVersionHeader.getClient();
		long clientFileClock = currentDatabaseVersionHeader.getVectorClock().getClock(clientName);
		
		DatabaseRemoteFile potentialDatabaseRemoteFileForRange = new DatabaseRemoteFile(clientName, clientFileClock);				
		return shortFilenameToFileMap.get(potentialDatabaseRemoteFileForRange.getName());
	}

	/**
	 * Returns a database file for a given database version header, or throws an error if
	 * no file has been found.
	 * 
	 * <p><b>Note:</b> Unlike {@link #getExactDatabaseVersionFile(DatabaseVersionHeader, Map) getExactDatabaseVersionFile()},
	 * this method tries to find a database file by counting up the local version, i.e. if the exact version cannot be found,
	 * it increases the local client version by one until a matching version is found.
	 * 
	 * <p><b>Example:</b> given database version header is A/(A3,B2)/T=..
	 * <pre>
	 *   - Does db-A-0003 exist? No, continue.
	 *   - Does db-A-0004 exist? No, continue.
	 *   - Does db-A-0005 exist. Yes, return db-A-0005.
	 * </pre>
	 */
	private File getNextDatabaseVersionFile(DatabaseVersionHeader currentDatabaseVersionHeader, Map<String, File> shortFilenameToFileMap) throws StorageException {
		String clientName = currentDatabaseVersionHeader.getClient();
		long clientFileClock = currentDatabaseVersionHeader.getVectorClock().getClock(clientName);
		
		DatabaseRemoteFile potentialDatabaseRemoteFileForRange = null;
		File databaseFileForRange = null;
		
		int maxRounds = 100000; // TODO [medium] This is ugly and potentially dangerous. Can this lead to incorrect results?
		boolean isLoadableDatabaseFile = false;
		
		while (!isLoadableDatabaseFile && maxRounds > 0) {
			potentialDatabaseRemoteFileForRange = new DatabaseRemoteFile(clientName, clientFileClock);
			
			databaseFileForRange = shortFilenameToFileMap.get(potentialDatabaseRemoteFileForRange.getName());
			isLoadableDatabaseFile = databaseFileForRange != null;	
			
			maxRounds--;
			clientFileClock++;
		}
		
		if (!isLoadableDatabaseFile) {
			throw new StorageException("Cannot find suitable database remote file to load range.");
		}
		
		return databaseFileForRange;
	}

	private DatabaseBranches readUnknownDatabaseVersionHeaders(TreeMap<File, DatabaseRemoteFile> remoteDatabases) throws IOException, StorageException {
		logger.log(Level.INFO, "Loading database headers, creating branches ...");

		// Read database files
		DatabaseBranches unknownRemoteBranches = new DatabaseBranches();
		DatabaseXmlSerializer dbDAO = new DatabaseXmlSerializer(config.getTransformer());

		for (Map.Entry<File, DatabaseRemoteFile> remoteDatabaseFileEntry : remoteDatabases.entrySet()) {
			MemoryDatabase remoteDatabase = new MemoryDatabase(); // Database cannot be reused, since these might be different clients

			File remoteDatabaseFileInCache = remoteDatabaseFileEntry.getKey();
			DatabaseRemoteFile remoteDatabaseFile = remoteDatabaseFileEntry.getValue();
			
			dbDAO.load(remoteDatabase, remoteDatabaseFileInCache, true, null); // only load headers!
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

	private TreeMap<File, DatabaseRemoteFile> downloadUnknownRemoteDatabases(TransferManager transferManager, List<DatabaseRemoteFile> unknownRemoteDatabases)
			throws StorageException {
		
		logger.log(Level.INFO, "Downloading unknown databases.");

		TreeMap<File, DatabaseRemoteFile> unknownRemoteDatabasesInCache = new TreeMap<File, DatabaseRemoteFile>();
		int downloadFileIndex = 0;

		if (listener != null) {
			listener.onDownloadStart(unknownRemoteDatabases.size());
		}
		
		for (DatabaseRemoteFile remoteFile : unknownRemoteDatabases) {
			File unknownRemoteDatabaseFileInCache = config.getCache().getDatabaseFile(remoteFile.getName());
			DatabaseRemoteFile unknownDatabaseRemoteFile = new DatabaseRemoteFile(remoteFile.getName());
			
			logger.log(Level.INFO, "- Downloading {0} to local cache at {1}", new Object[] { remoteFile.getName(), unknownRemoteDatabaseFileInCache });

			downloadFileIndex++;
			if (listener != null) {
				listener.onDownloadFile(remoteFile.getName(), downloadFileIndex);
			}
			
			transferManager.download(unknownDatabaseRemoteFile, unknownRemoteDatabaseFileInCache);

			unknownRemoteDatabasesInCache.put(unknownRemoteDatabaseFileInCache, unknownDatabaseRemoteFile);
			result.getDownloadedUnknownDatabases().add(remoteFile.getName());
		}

		return unknownRemoteDatabasesInCache;
	}
}
