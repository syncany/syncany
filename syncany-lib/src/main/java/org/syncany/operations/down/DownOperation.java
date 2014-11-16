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
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simpleframework.xml.core.Persister;
import org.syncany.config.Config;
import org.syncany.config.LocalEventBus;
import org.syncany.config.to.CleanupTO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.DatabaseVersionHeader.DatabaseVersionType;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.database.VectorClock;
import org.syncany.database.dao.DatabaseXmlSerializer;
import org.syncany.database.dao.DatabaseXmlSerializer.DatabaseReadType;
import org.syncany.operations.AbstractTransferOperation;
import org.syncany.operations.cleanup.CleanupOperation;
import org.syncany.operations.daemon.messages.DownDownloadFileSyncExternalEvent;
import org.syncany.operations.daemon.messages.DownEndSyncExternalEvent;
import org.syncany.operations.daemon.messages.DownStartSyncExternalEvent;
import org.syncany.operations.down.DownOperationOptions.DownConflictStrategy;
import org.syncany.operations.down.DownOperationResult.DownResultCode;
import org.syncany.operations.ls_remote.LsRemoteOperation;
import org.syncany.operations.ls_remote.LsRemoteOperationResult;
import org.syncany.operations.up.UpOperation;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.files.CleanupRemoteFile;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

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
 *  <li>If the apply-changes-flag is switched on, changes are applied to the local file system using the
 *      {@link ApplyChangesOperation}.</li>
 *  <li>Save local database and update known database list (database files that do not need to be 
 *      downloaded anymore</li>  
 * </ol>
 *     
 * @see DatabaseReconciliator
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DownOperation extends AbstractTransferOperation {
	private static final Logger logger = Logger.getLogger(DownOperation.class.getSimpleName());

	public static final String ACTION_ID = "down";

	private LocalEventBus eventBus;

	private DownOperationOptions options;
	private DownOperationResult result;

	private SqlDatabase localDatabase;
	private DatabaseReconciliator databaseReconciliator;
	private DatabaseXmlSerializer databaseSerializer;

	public DownOperation(Config config) {
		this(config, new DownOperationOptions());
	}

	public DownOperation(Config config, DownOperationOptions options) {
		super(config, ACTION_ID);

		this.eventBus = LocalEventBus.getInstance();

		this.options = options;
		this.result = new DownOperationResult();

		this.localDatabase = new SqlDatabase(config);
		this.databaseReconciliator = new DatabaseReconciliator();
		this.databaseSerializer = new DatabaseXmlSerializer(config.getTransformer());
	}

	/**
	 * Executes the down operation, roughly following these steps:
	 * 
	 * <ul>
	 *  <li>Download the remote databases to the local cache folder
	 *  <li>Read version headers (vector clocks)
	 *  <li>Determine winner branch
	 *  <li>Prune local stuff (if local conflicts exist)
	 *  <li>Apply winner's branch
	 *  <li>Write names of newly analyzed remote databases (so we don't download them again)
	 * </ul>
	 */
	@Override
	public DownOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync down' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		if (!checkPreconditions()) {
			return result;
		}

		fireStartEvent();
		startOperation();

		DatabaseBranch localBranch = localDatabase.getLocalDatabaseBranch();
		List<DatabaseRemoteFile> newRemoteDatabases = result.getLsRemoteResult().getUnknownRemoteDatabases();

		TreeMap<File, DatabaseRemoteFile> unknownRemoteDatabasesInCache = downloadUnknownRemoteDatabases(newRemoteDatabases);
		TreeMap<DatabaseRemoteFile, List<DatabaseVersion>> remoteDatabaseHeaders = readUnknownDatabaseVersionHeaders(unknownRemoteDatabasesInCache);
		DatabaseBranches unknownRemoteBranches = populateDatabaseBranches(remoteDatabaseHeaders);
		Map<DatabaseVersionHeader, File> databaseVersionLocations = findDatabaseVersionLocations(remoteDatabaseHeaders, unknownRemoteDatabasesInCache);

		if (cleanupOccurred()) {
			localBranch = new DatabaseBranch();
			localDatabase.deleteAll();
			localDatabase.commit();
		}
		try {
			DatabaseBranches allStitchedBranches = determineStitchedBranches(localBranch, unknownRemoteBranches);

			Map.Entry<String, DatabaseBranch> winnersBranch = determineWinnerBranch(localBranch, allStitchedBranches);

			purgeConflictingLocalBranch(localBranch, winnersBranch);

			applyWinnersBranch(localBranch, winnersBranch, allStitchedBranches, databaseVersionLocations);

			persistMuddyMultiChunks(winnersBranch, allStitchedBranches, databaseVersionLocations);
			removeNonMuddyMultiChunks();

			localDatabase.writeKnownRemoteDatabases(newRemoteDatabases);
			localDatabase.commit();
		}
		catch (Exception e) {
			localDatabase.rollback();
			throw e;
		}

		finishOperation();
		fireEndEvent();

		logger.log(Level.INFO, "Sync down done.");
		return result;
	}

	private void fireStartEvent() {
		eventBus.post(new DownStartSyncExternalEvent(config.getLocalDir().getAbsolutePath()));
	}

	private void fireEndEvent() {
		eventBus.post(new DownEndSyncExternalEvent(config.getLocalDir().getAbsolutePath(), result.getResultCode(), result.getChangeSet()));
	}

	/**
	 * Checks whether any new databases are only and whether any other conflicting
	 * actions are running.  
	 * 
	 * <p>This method sets the result code in <tt>result</tt> according to the 
	 * checking result and returns <tt>true</tt> if the rest of the operation can 
	 * continue, <tt>false</tt> otherwise. 
	 */
	private boolean checkPreconditions() throws Exception {
		// Check strategies
		if (options.getConflictStrategy() != DownConflictStrategy.RENAME) {
			logger.log(Level.INFO, "Conflict strategy " + options.getConflictStrategy() + " not yet implemented.");
			result.setResultCode(DownResultCode.NOK);

			return false;
		}

		// Check which remote databases to download based on the last local vector clock
		LsRemoteOperationResult lsRemoteResult = listUnknownRemoteDatabases();
		result.setLsRemoteResult(lsRemoteResult);

		if (lsRemoteResult.getUnknownRemoteDatabases().isEmpty()) {
			logger.log(Level.INFO, "* Nothing new. Skipping down operation.");
			result.setResultCode(DownResultCode.OK_NO_REMOTE_CHANGES);

			return false;
		}

		// Check if other operations are running
		if (otherRemoteOperationsRunning(CleanupOperation.ACTION_ID)) {
			logger.log(Level.INFO, "* Cleanup running. Skipping down operation.");
			result.setResultCode(DownResultCode.NOK);

			return false;
		}

		return true;
	}

	/**
	 * Lists unknown/new remote databases using the {@link LsRemoteOperation}. 
	 */
	private LsRemoteOperationResult listUnknownRemoteDatabases() throws Exception {
		return new LsRemoteOperation(config, transferManager).execute();
	}

	/**
	 * Downloads the previously identified new/unknown remote databases to the local cache
	 * and returns a map with the local cache files mapped to the given remote database 
	 * files. The method additionally fires events for every database it downloads.
	 */
	private TreeMap<File, DatabaseRemoteFile> downloadUnknownRemoteDatabases(List<DatabaseRemoteFile> unknownRemoteDatabases)
			throws StorageException {

		logger.log(Level.INFO, "Downloading unknown databases.");

		TreeMap<File, DatabaseRemoteFile> unknownRemoteDatabasesInCache = new TreeMap<File, DatabaseRemoteFile>();
		int downloadFileIndex = 0;

		for (DatabaseRemoteFile remoteFile : unknownRemoteDatabases) {
			File unknownRemoteDatabaseFileInCache = config.getCache().getDatabaseFile(remoteFile.getName());
			DatabaseRemoteFile unknownDatabaseRemoteFile = new DatabaseRemoteFile(remoteFile.getName());

			logger.log(Level.INFO, "- Downloading {0} to local cache at {1}", new Object[] { remoteFile.getName(), unknownRemoteDatabaseFileInCache });
			eventBus.post(new DownDownloadFileSyncExternalEvent(config.getLocalDir().getAbsolutePath(), "database", ++downloadFileIndex,
					unknownRemoteDatabases.size()));

			transferManager.download(unknownDatabaseRemoteFile, unknownRemoteDatabaseFileInCache);

			unknownRemoteDatabasesInCache.put(unknownRemoteDatabaseFileInCache, unknownDatabaseRemoteFile);
			result.getDownloadedUnknownDatabases().add(remoteFile.getName());
		}

		return unknownRemoteDatabasesInCache;
	}

	/**
	 * Read the given database files into individual per-user {@link DatabaseBranch}es. This method only
	 * reads the headers from the local database files, and not the entire databases into memory.
	 * 
	 * <p>The returned database branches contain only the per-client {@link DatabaseVersionHeader}s, and not
	 * the entire stitched branches, i.e. A's database branch will only contain database version headers from A.
	 */
	private TreeMap<DatabaseRemoteFile, List<DatabaseVersion>> readUnknownDatabaseVersionHeaders(TreeMap<File, DatabaseRemoteFile> remoteDatabases)
			throws IOException,
			StorageException {
		logger.log(Level.INFO, "Loading database headers, creating branches ...");

		// Read database files
		TreeMap<DatabaseRemoteFile, List<DatabaseVersion>> remoteDatabaseHeaders = new TreeMap<DatabaseRemoteFile, List<DatabaseVersion>>();

		for (Map.Entry<File, DatabaseRemoteFile> remoteDatabaseFileEntry : remoteDatabases.entrySet()) {
			MemoryDatabase remoteDatabase = new MemoryDatabase(); // Database cannot be reused, since these might be different clients

			File remoteDatabaseFileInCache = remoteDatabaseFileEntry.getKey();
			DatabaseRemoteFile remoteDatabaseFile = remoteDatabaseFileEntry.getValue();

			databaseSerializer.load(remoteDatabase, remoteDatabaseFileInCache, null, null, DatabaseReadType.HEADER_ONLY, null); // only load headers!

			remoteDatabaseHeaders.put(remoteDatabaseFile, remoteDatabase.getDatabaseVersions());

		}

		return remoteDatabaseHeaders;
	}

	private DatabaseBranches populateDatabaseBranches(TreeMap<DatabaseRemoteFile, List<DatabaseVersion>> remoteDatabaseHeaders) {
		DatabaseBranches unknownRemoteBranches = new DatabaseBranches();
		for (DatabaseRemoteFile remoteDatabaseFile : remoteDatabaseHeaders.keySet()) {

			// Populate branches
			DatabaseBranch remoteClientBranch = unknownRemoteBranches.getBranch(remoteDatabaseFile.getClientName(), true);

			for (DatabaseVersion remoteDatabaseVersion : remoteDatabaseHeaders.get(remoteDatabaseFile)) {
				DatabaseVersionHeader header = remoteDatabaseVersion.getHeader();
				remoteClientBranch.add(header);
			}
		}

		logger.log(Level.INFO, "Populated unknown branches: " + unknownRemoteBranches);
		return unknownRemoteBranches;
	}

	/**
	 * Uses the {@link DatabaseReconciliator} to stitch together the partial database branches of 
	 * the other clients to full database branches that can be used in further algorithms.
	 * 
	 * <p>Input to this algorithm are the locally complete branch (extracted from the local database)
	 * and the partial remote databases read from the new database files. 
	 */
	private DatabaseBranches determineStitchedBranches(DatabaseBranch localBranch, DatabaseBranches unknownRemoteBranches) {
		logger.log(Level.INFO, "Determine stitched branches using database reconciliator ...");
		logger.log(Level.FINE, "Local branch: " + localBranch);
		return databaseReconciliator.stitchBranches(unknownRemoteBranches, config.getMachineName(), localBranch);
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
	 * @param localBranch Local database branch (extracted from the local database)
	 * @param allStitchedBranches The newly downloaded remote database version headers (= branches)
	 * @return Returns the branch of the winner 
	 * @throws Exception If any kind of error occurs (...)
	 */
	private Map.Entry<String, DatabaseBranch> determineWinnerBranch(DatabaseBranch localBranch, DatabaseBranches allStitchedBranches)
			throws Exception {
		logger.log(Level.INFO, "Determine winner using database reconciliator ...");
		return databaseReconciliator.findWinnerBranch(config.getMachineName(), localBranch, allStitchedBranches);
	}

	/**
	 * Marks locally conflicting database versions as <tt>DIRTY</tt> and removes remote databases that
	 * correspond to those database versions. This method uses the {@link DatabaseReconciliator}
	 * to determine whether there is a local purge branch. 
	 */
	private void purgeConflictingLocalBranch(DatabaseBranch localBranch, Entry<String, DatabaseBranch> winnersBranch) throws Exception {
		DatabaseBranch localPurgeBranch = databaseReconciliator.findLosersPruneBranch(localBranch, winnersBranch.getValue());
		logger.log(Level.INFO, "- Database versions to REMOVE locally: " + localPurgeBranch);

		if (localPurgeBranch.size() == 0) {
			logger.log(Level.INFO, "  + Nothing to purge locally. No conflicts. Only updates. Nice!");
		}
		else {
			// Load dirty database (if existent)
			logger.log(Level.INFO, "  + Marking databases as DIRTY locally ...");

			for (DatabaseVersionHeader databaseVersionHeader : localPurgeBranch.getAll()) {
				logger.log(Level.INFO, "    * MASTER->DIRTY: " + databaseVersionHeader);
				localDatabase.markDatabaseVersionDirty(databaseVersionHeader.getVectorClock());

				boolean isOwnDatabaseVersionHeader = config.getMachineName().equals(databaseVersionHeader.getClient());

				if (isOwnDatabaseVersionHeader) {
					String remoteFileToPruneClientName = config.getMachineName();
					long remoteFileToPruneVersion = databaseVersionHeader.getVectorClock().getClock(config.getMachineName());
					DatabaseRemoteFile remoteFileToPrune = new DatabaseRemoteFile(remoteFileToPruneClientName, remoteFileToPruneVersion);

					logger.log(Level.INFO, "    * Deleting own remote database file " + remoteFileToPrune + " ...");
					transferManager.delete(remoteFileToPrune);
				}
				else {
					logger.log(Level.INFO, "    * NOT deleting any database file remotely (not our database!)");
				}

				result.getDirtyDatabasesCreated().add(databaseVersionHeader);
			}
		}
	}

	/**
	 * Applies the winner's branch locally in the local database as well as on the local file system. To
	 * do so, it reads the winner's database, downloads newly required multichunks, determines file system actions
	 * and applies these actions locally.
	 */
	private void applyWinnersBranch(DatabaseBranch localBranch, Entry<String, DatabaseBranch> winnersBranch, DatabaseBranches allStitchedBranches,
			Map<DatabaseVersionHeader, File> databaseVersionLocations) throws Exception {

		DatabaseBranch winnersApplyBranch = databaseReconciliator.findWinnersApplyBranch(localBranch, winnersBranch.getValue());
		logger.log(Level.INFO, "- Database versions to APPLY locally: " + winnersApplyBranch);

		if (winnersApplyBranch.size() == 0) {
			logger.log(Level.WARNING, "  + Nothing to update. Nice!");
			result.setResultCode(DownResultCode.OK_NO_REMOTE_CHANGES);
		}
		else {
			logger.log(Level.INFO, "Loading winners database (DEFAULT) ...");
			MemoryDatabase winnersDatabase = readWinnersDatabase(winnersApplyBranch, databaseVersionLocations, DatabaseVersionType.DEFAULT);

			if (options.isApplyChanges()) {
				new ApplyChangesOperation(config, localDatabase, transferManager, winnersDatabase, result).execute();
			}
			else {
				logger.log(Level.INFO, "Doing nothing on the file system, because --no-apply switched on");
			}

			persistDatabaseVersions(winnersApplyBranch, winnersDatabase);

			result.setResultCode(DownResultCode.OK_WITH_REMOTE_CHANGES);
		}
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
	 * @param databaseFileList 
	 * @param ignoredMostRecentPurgeVersions 
	 * 
	 * @return Returns a loaded memory database containing all metadata from the winner's branch 
	 */
	private MemoryDatabase readWinnersDatabase(DatabaseBranch winnersApplyBranch, Map<DatabaseVersionHeader, File> databaseVersionLocations,
			DatabaseVersionType filterType) throws IOException, StorageException {

		MemoryDatabase winnerBranchDatabase = new MemoryDatabase();

		List<DatabaseVersionHeader> winnersApplyBranchList = winnersApplyBranch.getAll();

		String rangeClientName = null;
		VectorClock rangeVersionFrom = null;
		VectorClock rangeVersionTo = null;
		logger.log(Level.FINE, "DatabaseVersionLocations: " + databaseVersionLocations);
		for (int i = 0; i < winnersApplyBranchList.size(); i++) {

			DatabaseVersionHeader currentDatabaseVersionHeader = winnersApplyBranchList.get(i);
			DatabaseVersionHeader nextDatabaseVersionHeader = (i + 1 < winnersApplyBranchList.size()) ? winnersApplyBranchList.get(i + 1) : null;

			logger.log(Level.FINE, "current header: " + currentDatabaseVersionHeader + " --- next header:    " + nextDatabaseVersionHeader);

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

			File databaseVersionFile = databaseVersionLocations.get(currentDatabaseVersionHeader);

			boolean lastDatabaseVersionHeader = nextDatabaseVersionHeader == null;
			boolean nextDatabaseVersionInSameFile = lastDatabaseVersionHeader
					|| databaseVersionFile.equals(databaseVersionLocations.get(nextDatabaseVersionHeader));
			boolean rangeEnds = lastDatabaseVersionHeader || !nextDatabaseVersionInSameFile;

			if (rangeEnds) {
				logger.log(Level.FINE, "load " + rangeVersionFrom + " ... " + rangeVersionTo + " from " + databaseVersionFile);

				databaseSerializer.load(winnerBranchDatabase, databaseVersionFile, rangeVersionFrom, rangeVersionTo, DatabaseReadType.FULL,
						filterType);
				rangeClientName = null;
			}
			else {
				logger.log(Level.FINE, "noload " + rangeVersionFrom + " ... " + rangeVersionTo + " from " + databaseVersionFile);
			}
		}

		for (DatabaseVersion dbv : winnerBranchDatabase.getDatabaseVersions()) {
			logger.log(Level.FINE, "WINNER " + dbv.getHeader());
		}

		return winnerBranchDatabase;
	}

	/**
	 * Takes a collection of {@link PartialFileHistory}s and returns a map of their history 
	 * identifiers to the last version in the history. This method is used to determine the 
	 * details purge versions (from a purge database file).
	 * 
	 * <p>The result of this method is used to ignore the already purged file histories when
	 * reading the winner's database in {@link #readWinnersDatabase(DatabaseBranch, DatabaseFileList, DatabaseVersionType, Map)}.
	 */
	private Map<FileHistoryId, FileVersion> extractMostRecentPurgeVersions(Collection<PartialFileHistory> fileHistories) {
		Map<FileHistoryId, FileVersion> mostRecentPurgeVersions = new HashMap<FileHistoryId, FileVersion>();

		for (PartialFileHistory fileHistory : fileHistories) {
			FileVersion mostRecentPurgeVersion = fileHistory.getLastVersion();
			mostRecentPurgeVersions.put(fileHistory.getFileHistoryId(), mostRecentPurgeVersion);
		}

		return mostRecentPurgeVersions;
	}

	/**
	 * Persists the given winners branch to the local database, i.e. for every database version
	 * in the winners branch, all contained multichunks, chunks, etc. are added to the local SQL 
	 * database.
	 * 
	 * <p>This method applies both regular database versions as well as purge database versions. 
	 */
	private void persistDatabaseVersions(DatabaseBranch winnersApplyBranch, MemoryDatabase winnersDatabase)
			throws SQLException {
		// Add winners database to local database
		// Note: This must happen AFTER the file system stuff, because we compare the winners database with the local database!			
		logger.log(Level.INFO, "- Adding database versions to SQL database ...");

		for (DatabaseVersionHeader currentDatabaseVersionHeader : winnersApplyBranch.getAll()) {
			if (currentDatabaseVersionHeader.getType() == DatabaseVersionType.DEFAULT) {
				persistDatabaseVersion(winnersDatabase, currentDatabaseVersionHeader);
			}
			else {
				throw new RuntimeException("Unknow database version type: " + currentDatabaseVersionHeader.getType());
			}
		}
	}

	/**
	 * Persists a regular database version to the local database by using 
	 * {@link SqlDatabase#writeDatabaseVersion(DatabaseVersion)}.
	 */
	private void persistDatabaseVersion(MemoryDatabase winnersDatabase, DatabaseVersionHeader currentDatabaseVersionHeader) {
		logger.log(Level.INFO, "  + Applying database version " + currentDatabaseVersionHeader.getVectorClock());

		DatabaseVersion applyDatabaseVersion = winnersDatabase.getDatabaseVersion(currentDatabaseVersionHeader.getVectorClock());
		logger.log(Level.FINE, "  + Contents: " + applyDatabaseVersion);
		localDatabase.writeDatabaseVersion(applyDatabaseVersion);
	}

	/**
	 * Identifies and persists 'muddy' multichunks to the local database. Muddy multichunks are multichunks
	 * that have been referenced by DIRTY database versions and might be reused in future database versions when
	 * the other client cleans up its mess (performs another 'up'). 
	 */
	private void persistMuddyMultiChunks(Entry<String, DatabaseBranch> winnersBranch, DatabaseBranches allStitchedBranches,
			Map<DatabaseVersionHeader, File> databaseVersionLocations) throws StorageException, IOException, SQLException {
		// Find dirty database versions (from other clients!) and load them from files
		Map<DatabaseVersionHeader, Collection<MultiChunkEntry>> muddyMultiChunksPerDatabaseVersion = new HashMap<>();
		Set<DatabaseVersionHeader> winnersDatabaseVersionHeaders = Sets.newHashSet(winnersBranch.getValue().getAll());

		for (String otherClientName : allStitchedBranches.getClients()) {
			boolean isLocalMachine = config.getMachineName().equals(otherClientName);

			if (!isLocalMachine) {
				DatabaseBranch otherClientBranch = allStitchedBranches.getBranch(otherClientName);
				Set<DatabaseVersionHeader> otherClientDatabaseVersionHeaders = Sets.newHashSet(otherClientBranch.getAll());

				SetView<DatabaseVersionHeader> otherMuddyDatabaseVersionHeaders = Sets.difference(otherClientDatabaseVersionHeaders,
						winnersDatabaseVersionHeaders);
				boolean hasMuddyDatabaseVersionHeaders = otherMuddyDatabaseVersionHeaders.size() > 0;

				if (hasMuddyDatabaseVersionHeaders) {
					logger.log(Level.INFO, "DIRTY database version headers of " + otherClientName + ":  " + otherMuddyDatabaseVersionHeaders);

					for (DatabaseVersionHeader muddyDatabaseVersionHeader : otherMuddyDatabaseVersionHeaders) {
						MemoryDatabase muddyMultiChunksDatabase = new MemoryDatabase();

						File localFileForMuddyDatabaseVersion = databaseVersionLocations.get(muddyDatabaseVersionHeader);
						VectorClock fromVersion = muddyDatabaseVersionHeader.getVectorClock();
						VectorClock toVersion = muddyDatabaseVersionHeader.getVectorClock();

						logger.log(Level.INFO, "  - Loading " + muddyDatabaseVersionHeader + " from file " + localFileForMuddyDatabaseVersion);
						databaseSerializer.load(muddyMultiChunksDatabase, localFileForMuddyDatabaseVersion, fromVersion, toVersion,
								DatabaseReadType.FULL, DatabaseVersionType.DEFAULT);

						boolean hasMuddyMultiChunks = muddyMultiChunksDatabase.getMultiChunks().size() > 0;

						if (hasMuddyMultiChunks) {
							muddyMultiChunksPerDatabaseVersion.put(muddyDatabaseVersionHeader, muddyMultiChunksDatabase.getMultiChunks());
						}
					}

				}
			}
		}

		// Add muddy multichunks to 'multichunks_muddy' database table 
		boolean hasMuddyMultiChunks = muddyMultiChunksPerDatabaseVersion.size() > 0;

		if (hasMuddyMultiChunks) {
			localDatabase.writeMuddyMultiChunks(muddyMultiChunksPerDatabaseVersion);
		}
	}

	/**
	 * Removes multichunks from the 'muddy' table as soon as they because present in the 
	 * actual multichunk database table.
	 */
	private void removeNonMuddyMultiChunks() throws SQLException {
		// TODO [medium] This might not get the right multichunks. Rather use the database version information in the multichunk_muddy table.
		localDatabase.removeNonMuddyMultiChunks();
	}

	private boolean containsDatabaseFromSelf(List<DatabaseRemoteFile> databaseRemoteFiles) {
		String clientName = config.getMachineName();
		for (DatabaseRemoteFile dbRemoteFile : databaseRemoteFiles) {
			if (dbRemoteFile.getClientName().equals(clientName)) {
				return true;
			}
		}
		return false;
	}

	private boolean cleanupOccurred() throws Exception {
		// Find all existing cleanup files
		Map<String, CleanupRemoteFile> cleanupFiles = transferManager.list(CleanupRemoteFile.class);

		Pattern cleanupFilePattern = Pattern.compile("cleanup-([0-9]+)");

		int cleanupNumber = 0;
		// Find the number of the last cleanup
		for (String filename : cleanupFiles.keySet()) {
			Matcher matcher = cleanupFilePattern.matcher(filename);
			matcher.matches();
			cleanupNumber = Math.max(cleanupNumber, Integer.parseInt(matcher.group(1)));
		}

		if (config.getCleanupFile().exists()) {
			CleanupTO cleanupTO = (new Persister()).read(CleanupTO.class, config.getCleanupFile());
			return cleanupNumber > cleanupTO.getCleanupNumber();
		}
		else {
			return cleanupNumber > 0;
		}
	}

	private Map<DatabaseVersionHeader, File> findDatabaseVersionLocations(Map<DatabaseRemoteFile, List<DatabaseVersion>> remoteDatabaseHeaders,
			Map<File, DatabaseRemoteFile> databaseRemoteFilesInCache) {
		Map<DatabaseVersionHeader, File> databaseVersionLocations = new HashMap<DatabaseVersionHeader, File>();

		for (File databaseFile : databaseRemoteFilesInCache.keySet()) {
			DatabaseRemoteFile databaseRemoteFile = databaseRemoteFilesInCache.get(databaseFile);
			for (DatabaseVersion databaseVersion : remoteDatabaseHeaders.get(databaseRemoteFile)) {
				databaseVersionLocations.put(databaseVersion.getHeader(), databaseFile);
			}
		}
		return databaseVersionLocations;
	}
}
