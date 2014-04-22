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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
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
import org.syncany.operations.ChangeSet;
import org.syncany.operations.CleanupOperation;
import org.syncany.operations.CleanupOperation.CleanupOperationResult;
import org.syncany.operations.LsRemoteOperation;
import org.syncany.operations.LsRemoteOperation.LsRemoteOperationResult;
import org.syncany.operations.Operation;
import org.syncany.operations.StatusOperation;
import org.syncany.operations.StatusOperation.StatusOperationResult;
import org.syncany.operations.down.DownOperation;
import org.syncany.operations.up.UpOperationResult.UpResultCode;

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
public class UpOperation extends Operation {
	private static final Logger logger = Logger.getLogger(UpOperation.class.getSimpleName());

	public static final int MIN_KEEP_DATABASE_VERSIONS = 5;
	public static final int MAX_KEEP_DATABASE_VERSIONS = 15;

	private UpOperationOptions options;
	private TransferManager transferManager;
	private SqlDatabase localDatabase;
	private UpOperationListener listener;
	
	public UpOperation(Config config) {
		this(config, new UpOperationOptions(), null);
	}
	
	public UpOperation(Config config, UpOperationListener listener) {
		this(config, new UpOperationOptions(), listener);
	}

	public UpOperation(Config config, UpOperationOptions options, UpOperationListener listener) {
		super(config);

		this.listener = listener;
		this.options = options;
		this.transferManager = config.getPlugin().createTransferManager(config.getConnection());
		this.localDatabase = new SqlDatabase(config);
	}

	@Override
	public UpOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync up' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		UpOperationResult result = new UpOperationResult();
		
		// Find local changes
		StatusOperation statusOperation = new StatusOperation(config, options.getStatusOptions());
		StatusOperationResult statusOperationResult = statusOperation.execute();
		ChangeSet localChanges = statusOperationResult.getChangeSet();
		
		result.getStatusResult().setChangeSet(localChanges);

		if (!localChanges.hasChanges()) {
			logger.log(Level.INFO, "Local database is up-to-date (change set). NOTHING TO DO!");
			result.setResultCode(UpResultCode.OK_NO_CHANGES);

			disconnectTransferManager();
			clearCache();

			return result;
		}

		// Find remote changes (unless --force is enabled)
		if (!options.forceUploadEnabled()) {
			LsRemoteOperationResult lsRemoteOperationResult = new LsRemoteOperation(config, transferManager).execute();
			List<DatabaseRemoteFile> unknownRemoteDatabases = lsRemoteOperationResult.getUnknownRemoteDatabases();

			if (unknownRemoteDatabases.size() > 0) {
				logger.log(Level.INFO, "There are remote changes. Call 'down' first or use --force you must, Luke!");
				result.setResultCode(UpResultCode.NOK_UNKNOWN_DATABASES);

				disconnectTransferManager();
				clearCache();

				return result;
			}
			else {
				logger.log(Level.INFO, "No remote changes, ready to upload.");
			}
		}
		else {
			logger.log(Level.INFO, "Force (--force) is enabled, ignoring potential remote changes.");
		}

		List<File> locallyUpdatedFiles = extractLocallyUpdatedFiles(localChanges);
		localChanges = null; // allow GC to clean up

		// Index
		DatabaseVersion newDatabaseVersion = index(locallyUpdatedFiles);

		if (newDatabaseVersion.getFileHistories().size() == 0) {
			logger.log(Level.INFO, "Local database is up-to-date. NOTHING TO DO!");
			result.setResultCode(UpResultCode.OK_NO_CHANGES);

			disconnectTransferManager();
			clearCache();

			return result;
		}		

		// Upload multichunks
		logger.log(Level.INFO, "Uploading new multichunks ...");
		uploadMultiChunks(newDatabaseVersion.getMultiChunks());

		// Create delta database
		writeAndUploadDeltaDatabase(newDatabaseVersion);

		// Save local database		
		logger.log(Level.INFO, "Persisting local SQL database (new database version {0}) ...", newDatabaseVersion.getHeader().toString());
		long newDatabaseVersionId = localDatabase.persistDatabaseVersion(newDatabaseVersion);

		logger.log(Level.INFO, "Removing DIRTY database versions from database ...");	
		localDatabase.removeDirtyDatabaseVersions(newDatabaseVersionId);		
		
		if (options.cleanupEnabled()) {
			CleanupOperationResult cleanupOperationResult = new CleanupOperation(config, options.getCleanupOptions()).execute();
			result.setCleanupResult(cleanupOperationResult); 
		}
					
		disconnectTransferManager();
		clearCache();

		logger.log(Level.INFO, "Sync up done.");

		// Result
		addNewDatabaseChangesToResultChanges(newDatabaseVersion, result.getChangeSet());
		result.setResultCode(UpResultCode.OK_APPLIED_CHANGES);
		
		return result;
	}

	private void writeAndUploadDeltaDatabase(DatabaseVersion newDatabaseVersion) throws InterruptedException, StorageException, IOException {
		// Clone database version (necessary, because the original must not be touched)
		DatabaseVersion deltaDatabaseVersion = newDatabaseVersion.clone();		
		
		// Add dirty data (if existent)
		addDirtyData(deltaDatabaseVersion);		
		
		// New delta database
		MemoryDatabase deltaDatabase = new MemoryDatabase();
		deltaDatabase.addDatabaseVersion(deltaDatabaseVersion);		
				
		// Save delta database locally
		long newestLocalDatabaseVersion = deltaDatabaseVersion.getVectorClock().getClock(config.getMachineName());
		DatabaseRemoteFile remoteDeltaDatabaseFile = new DatabaseRemoteFile(config.getMachineName(), newestLocalDatabaseVersion);
		File localDeltaDatabaseFile = config.getCache().getDatabaseFile(remoteDeltaDatabaseFile.getName());

		logger.log(Level.INFO, "Saving local delta database, version {0} to file {1} ... ", new Object[] {
				deltaDatabaseVersion.getHeader(), localDeltaDatabaseFile });
		
		saveDeltaDatabase(deltaDatabase, localDeltaDatabaseFile);				

		// Upload delta database
		logger.log(Level.INFO, "- Uploading local delta database file ...");
		uploadLocalDatabase(localDeltaDatabaseFile, remoteDeltaDatabaseFile);
	}

	protected void saveDeltaDatabase(MemoryDatabase db, File localDatabaseFile) throws IOException {	
		logger.log(Level.INFO, "- Saving database to "+localDatabaseFile+" ...");
		
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
				
				logger.log(Level.INFO, "- Adding chunks/multichunks/filecontents from database version "+dirtyDatabaseVersion.getHeader());
				
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

	private void uploadMultiChunks(Collection<MultiChunkEntry> multiChunksEntries) throws InterruptedException, StorageException {
		List<MultiChunkId> dirtyMultiChunkIds = localDatabase.getDirtyMultiChunkIds();
		int multiChunkIndex = 0;
		
		if (listener != null) {
			listener.onUploadStart(multiChunksEntries.size());
		}
		
		for (MultiChunkEntry multiChunkEntry : multiChunksEntries) {
			multiChunkIndex++;

			if (dirtyMultiChunkIds.contains(multiChunkEntry.getId())) {
				logger.log(Level.INFO, "- Ignoring multichunk (from dirty database, already uploaded), " + multiChunkEntry.getId() + " ...");
			}
			else {
				File localMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId());
				MultiChunkRemoteFile remoteMultiChunkFile = new MultiChunkRemoteFile(multiChunkEntry.getId());

				logger.log(Level.INFO, "- Uploading multichunk {0} from {1} to {2} ...", new Object[] { multiChunkEntry.getId(), localMultiChunkFile,
						remoteMultiChunkFile });
				
				transferManager.upload(localMultiChunkFile, remoteMultiChunkFile);
				
				if (listener != null) {
					listener.onUploadFile(remoteMultiChunkFile.getName(), multiChunkIndex);
				}

				logger.log(Level.INFO, "  + Removing " + multiChunkEntry.getId() + " locally ...");
				localMultiChunkFile.delete();
			}
		}
	}

	private void uploadLocalDatabase(File localDatabaseFile, DatabaseRemoteFile remoteDatabaseFile) throws InterruptedException, StorageException {
		logger.log(Level.INFO, "- Uploading " + localDatabaseFile + " to " + remoteDatabaseFile + " ...");
		transferManager.upload(localDatabaseFile, remoteDatabaseFile);
	}

	private DatabaseVersion index(List<File> localFiles) throws FileNotFoundException, IOException {
		// Get last vector clock
		DatabaseVersionHeader lastDatabaseVersionHeader = localDatabase.getLastDatabaseVersionHeader();
		VectorClock lastVectorClock = (lastDatabaseVersionHeader != null) ? lastDatabaseVersionHeader.getVectorClock() : new VectorClock();

		// New vector clock
		VectorClock newVectorClock = findNewVectorClock(lastVectorClock);

		// Index
		Deduper deduper = new Deduper(config.getChunker(), config.getMultiChunker(), config.getTransformer());
		Indexer indexer = new Indexer(config, deduper, listener);

		DatabaseVersion newDatabaseVersion = indexer.index(localFiles);

		newDatabaseVersion.setVectorClock(newVectorClock);
		newDatabaseVersion.setTimestamp(new Date());
		newDatabaseVersion.setClient(config.getMachineName());

		return newDatabaseVersion;
	}
	
	private VectorClock findNewVectorClock(VectorClock lastVectorClock) {
		VectorClock newVectorClock = lastVectorClock.clone();

		Long lastLocalValue = lastVectorClock.getClock(config.getMachineName());
		Long lastDirtyLocalValue = localDatabase.getMaxDirtyVectorClock(config.getMachineName());

		Long newLocalValue = null;

		if (lastDirtyLocalValue != null) {
			// TODO [medium] Does this lead to problems? C-1 does not exist! Possible problems with DatabaseReconciliator?
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

	private void disconnectTransferManager() {
		try {
			transferManager.disconnect();
		}
		catch (StorageException e) {
			// Don't care!
		}
	}
	
	private void clearCache() {
		config.getCache().clear();
	}
}
