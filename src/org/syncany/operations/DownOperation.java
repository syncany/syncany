package org.syncany.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Cache;
import org.syncany.config.Config;
import org.syncany.connection.plugins.DatabaseRemoteFile;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Branch;
import org.syncany.database.Branches;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.DatabaseDAO;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.DatabaseVersionHeader;
import org.syncany.database.DatabaseXmlDAO;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileStatus;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.RemoteDatabaseFile;
import org.syncany.database.VectorClock;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;
import org.syncany.operations.LsRemoteOperation.RemoteStatusOperationResult;
import org.syncany.operations.StatusOperation.ChangeSet;
import org.syncany.operations.actions.ChangeFileSystemAction;
import org.syncany.operations.actions.DeleteFileSystemAction;
import org.syncany.operations.actions.FileSystemAction;
import org.syncany.operations.actions.NewFileSystemAction;
import org.syncany.operations.actions.RenameFileSystemAction;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class DownOperation extends Operation {
	private static final Logger logger = Logger.getLogger(DownOperation.class.getSimpleName());
	
	private Database localDatabase;
	@SuppressWarnings("unused")	private DownOperationOptions options;
	private DownOperationResult result;
	
	private Branch localBranch;
	private TransferManager transferManager;
	private DatabaseReconciliator databaseReconciliator;
	
	public DownOperation(Config config) {
		this(config, null, new DownOperationOptions());
	}	
	
	public DownOperation(Config config, Database database) {
		this(config, database, new DownOperationOptions());
	}	
	
	public DownOperation(Config config, Database database, DownOperationOptions options) {
		super(config);
		
		this.localDatabase = database;
		this.options = options;
		this.result = new DownOperationResult();
	}	
	
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync down' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");		
				
		// 0. Load database and create TM
		initOperationVariables();

		// 1. Check which remote databases to download based on the last local vector clock		
		List<RemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(localDatabase, transferManager);
		
		if (unknownRemoteDatabases.isEmpty()) {
			logger.log(Level.INFO, "* Nothing new. Skipping down operation.");
			return result;
		}
		
		// 2. Download the remote databases to the local cache folder
		List<File> unknownRemoteDatabasesInCache = downloadUnknownRemoteDatabases(transferManager, unknownRemoteDatabases);
		
		// 3. Read version headers (vector clocks)
		Branches unknownRemoteBranches = readUnknownDatabaseVersionHeaders(unknownRemoteDatabasesInCache);
		
		// 4. Determine winner branch
		Branch winnersBranch = determineWinnerBranch(localDatabase, unknownRemoteBranches);		
		logger.log(Level.INFO, "We have a winner! Now determine what to do locally ...");

		// 5. Prune local stuff (if local conflicts exist)
		pruneConflictingLocalBranch(winnersBranch);
		
		// 6. Apply winner's branch 
		appyWinnersBranch(winnersBranch, unknownRemoteDatabasesInCache);
		
		logger.log(Level.INFO, "Sync down done.");		
		return result;		
	}		
	
	private void appyWinnersBranch(Branch winnersBranch, List<File> unknownRemoteDatabasesInCache) throws Exception {
		Branch winnersApplyBranch = databaseReconciliator.findWinnersApplyBranch(localBranch, winnersBranch);
		logger.log(Level.INFO, "- Database versions to APPLY locally: "+winnersApplyBranch);
		
		if (winnersApplyBranch.size() == 0) {
			logger.log(Level.WARNING, "  + Nothing to update. Nice!");
		}
		else {
			logger.log(Level.INFO, "- Loading winners database ...");				
			Database winnersDatabase = readWinnersDatabase(winnersApplyBranch, unknownRemoteDatabasesInCache);
			
			// Now download and extract multichunks
			Set<MultiChunkEntry> unknownMultiChunks = determineUnknownMultiChunks(localDatabase, winnersDatabase, config.getCache());
			downloadAndExtractMultiChunks(unknownMultiChunks);
			
			List<FileSystemAction> actions = determineFileSystemActions(localDatabase, winnersDatabase);
			applyFileSystemActions(actions);
							
			// Add winners database to local database
			// Note: This must happen AFTER the file system stuff, because we compare the winners database with the local database! 
			for (DatabaseVersionHeader applyDatabaseVersionHeader : winnersApplyBranch.getAll()) {
				logger.log(Level.INFO, "   + Applying database version "+applyDatabaseVersionHeader.getVectorClock());
				
				DatabaseVersion applyDatabaseVersion = winnersDatabase.getDatabaseVersion(applyDatabaseVersionHeader.getVectorClock());									
				localDatabase.addDatabaseVersion(applyDatabaseVersion);										
			}	
			

			logger.log(Level.INFO, "- Saving local database to "+config.getDatabaseFile()+" ...");
			saveLocalDatabase(localDatabase, config.getDatabaseFile());
		}
	}

	private void initOperationVariables() throws Exception {		
		localDatabase = (localDatabase != null) 
			? localDatabase
			: ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();
		
		localBranch = localDatabase.getBranch();	

		transferManager = config.getConnection().createTransferManager();		
		databaseReconciliator = new DatabaseReconciliator();
	}

	private void pruneConflictingLocalBranch(Branch winnersBranch) throws StorageException, IOException {		
		Branch localPruneBranch = databaseReconciliator.findLosersPruneBranch(localBranch, winnersBranch);
		logger.log(Level.INFO, "- Database versions to REMOVE locally: "+localPruneBranch);
		
		if (localPruneBranch.size() == 0) {
			logger.log(Level.INFO, "  + Nothing to prune locally. No conflicts. Only updates. Nice!");
		}
		else {
			logger.log(Level.INFO, "  + Pruning databases locally ...");
			Database dirtyDatabase = new Database();			
			
			for (DatabaseVersionHeader databaseVersionHeader : localPruneBranch.getAll()) {
				// Database version
				DatabaseVersion databaseVersion = localDatabase.getDatabaseVersion(databaseVersionHeader.getVectorClock());
				dirtyDatabase.addDatabaseVersion(databaseVersion);
				
				// Remove database version locally
				logger.log(Level.INFO, "    * Removing "+databaseVersionHeader+" ...");
				localDatabase.removeDatabaseVersion(databaseVersion);
				
				DatabaseRemoteFile remoteFileToPrune = new DatabaseRemoteFile("db-"+config.getMachineName()+"-"+databaseVersionHeader.getVectorClock().get(config.getMachineName()));
				logger.log(Level.INFO, "    * Deleting remote database file "+remoteFileToPrune+" ...");
				transferManager.delete(remoteFileToPrune);
			}
			
			logger.log(Level.INFO, "    * Saving dirty database to "+config.getDirtyDatabaseFile()+" ...");
			saveLocalDatabase(dirtyDatabase, config.getDirtyDatabaseFile());
		}
		
	}

	private Branch determineWinnerBranch(Database localDatabase, Branches unknownRemoteBranches) throws Exception {
		logger.log(Level.INFO, "Detect updates and conflicts ...");
		DatabaseReconciliator databaseReconciliator = new DatabaseReconciliator();
				
		logger.log(Level.INFO, "- Stitching branches ...");
		Branches allStitchedBranches = databaseReconciliator.stitchBranches(unknownRemoteBranches, config.getMachineName(), localBranch);
		
		DatabaseVersionHeader lastCommonHeader = databaseReconciliator.findLastCommonDatabaseVersionHeader(localBranch, allStitchedBranches);		
		TreeMap<String, DatabaseVersionHeader> firstConflictingHeaders = databaseReconciliator.findFirstConflictingDatabaseVersionHeader(lastCommonHeader, allStitchedBranches);		
		TreeMap<String, DatabaseVersionHeader> winningFirstConflictingHeaders = databaseReconciliator.findWinningFirstConflictingDatabaseVersionHeaders(firstConflictingHeaders);		
		Entry<String, DatabaseVersionHeader> winnersWinnersLastDatabaseVersionHeader = databaseReconciliator.findWinnersWinnersLastDatabaseVersionHeader(winningFirstConflictingHeaders, allStitchedBranches);
		
		logger.log(Level.FINER, "- Database reconciliation results:");
		logger.log(Level.FINER, "  + localBranch: "+localBranch);
		logger.log(Level.FINER, "  + unknownRemoteBranches: "+unknownRemoteBranches);
		logger.log(Level.FINER, "  + allStitchedBranches: "+allStitchedBranches);
		logger.log(Level.FINER, "  + lastCommonHeader: "+lastCommonHeader);
		logger.log(Level.FINER, "  + firstConflictingHeaders: "+firstConflictingHeaders);
		logger.log(Level.FINER, "  + winningFirstConflictingHeaders: "+winningFirstConflictingHeaders);
		logger.log(Level.FINER, "  + winnersWinnersLastDatabaseVersionHeader: "+winnersWinnersLastDatabaseVersionHeader);

		String winnersName = winnersWinnersLastDatabaseVersionHeader.getKey();
		Branch winnersBranch = allStitchedBranches.getBranch(winnersName);
		
		logger.log(Level.INFO, "- Compared branches: "+allStitchedBranches);
		logger.log(Level.INFO, "- Winner is "+winnersName+" with branch "+winnersBranch);
		
		return winnersBranch;
	}

	private List<FileSystemAction> determineFileSystemActions(Database localDatabase, Database winnersDatabase) throws Exception {
		List<FileSystemAction> fileSystemActions = new ArrayList<FileSystemAction>();
		
		logger.log(Level.INFO, "- Determine filesystem actions ...");
		
		for (PartialFileHistory winningFileHistory : winnersDatabase.getFileHistories()) {
			// Get remote file version and content
			FileVersion winningLastVersion = winningFileHistory.getLastVersion();			
			
			// Get local file version and content
			PartialFileHistory localFileHistory = localDatabase.getFileHistory(winningFileHistory.getFileId());			
			FileVersion localLastVersion = (localFileHistory != null) ? localFileHistory.getLastVersion() : null;
			
			logger.log(Level.INFO, "   + Comparing local version "+localLastVersion+" with winning version  "+winningLastVersion);			
			
			// Cases
			boolean isNewFile = localLastVersion == null;
			boolean isInSamePlace = !isNewFile && winningLastVersion != null && localLastVersion.getFullName().equals(winningLastVersion.getFullName());
			boolean isChecksumEqual = !isNewFile && Arrays.equals(localLastVersion.getChecksum(), winningLastVersion.getChecksum());
			
			boolean isChangedFile = !isNewFile && isInSamePlace && !isChecksumEqual;
			boolean isIdenticalFile = !isNewFile && isInSamePlace && isChecksumEqual;
			boolean isRenamedFile = !isNewFile && !isInSamePlace && isChecksumEqual;
			boolean isDeletedFile = winningLastVersion.getStatus() == FileStatus.DELETED;
			
			if (isNewFile && isDeletedFile) {
				isNewFile = false; // TODO [low] This is ugly. Do in original 'isNewFile = ...'-statement
			}
			
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "      + isNewFile: "+isNewFile+", isInSamePlace:"+isInSamePlace+", isChecksumEqual: "+isChecksumEqual
						+", isChangedFile: "+isChangedFile+", isIdenticalFile: "+isIdenticalFile+", isRenamedFile: "+isRenamedFile+", isDeletedFile: "+isDeletedFile);
			}
			
			if (isNewFile) {
				FileSystemAction action = new NewFileSystemAction(config, winningLastVersion, localDatabase, winnersDatabase);
				fileSystemActions.add(action);

				result.getChangeSet().getNewFiles().add(winningLastVersion.getFullName());
				
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, "      + Added: "+action);
				}
			}
			else if (isChangedFile) {	
				FileSystemAction action = new ChangeFileSystemAction(config, localLastVersion, winningLastVersion, localDatabase, winnersDatabase);
				fileSystemActions.add(action);
				
				result.getChangeSet().getChangedFiles().add(winningLastVersion.getFullName());				
				
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, "      + Changed: "+action);
				}
			}
			else if (isRenamedFile) {
				FileSystemAction action = new RenameFileSystemAction(config, localLastVersion, winningLastVersion,localDatabase, winnersDatabase);
				fileSystemActions.add(action);
				
				result.getChangeSet().getChangedFiles().add(winningLastVersion.getFullName());								
				
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, "      + Renamed: "+action);
				}
			}
			else if (isDeletedFile) {
				FileSystemAction action = new DeleteFileSystemAction(config, localLastVersion, winningLastVersion, localDatabase, winnersDatabase);
				fileSystemActions.add(action);
				
				result.getChangeSet().getDeletedFiles().add(winningLastVersion.getFullName());								
				
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, "      + Deleted: {0}", action);
				}
			}
			else if (isIdenticalFile) {
				result.getChangeSet().getDeletedFiles().add(winningLastVersion.getFullName());												
				
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, "      + Identical file. Nothing to do.");	
				}
			}
			else {
				logger.log(Level.WARNING, "      + THIS SHOULD NOT HAPPEN"); 
				throw new Exception("Cannot determine file system action!");
			}
		}
				
		return fileSystemActions;
	}
	
	private void applyFileSystemActions(List<FileSystemAction> actions) throws Exception {
		// Sort
		Collections.sort(actions, new FileSystemActionComparator());
		
		logger.log(Level.FINER, "- Applying file system actions (sorted!) ...");		
		
		// Apply
		for (FileSystemAction action : actions) {			
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "   +  {0}", action);
			}
			
			action.execute();
		}
	}
	
	private void downloadAndExtractMultiChunks(Set<MultiChunkEntry> unknownMultiChunks) throws StorageException, IOException {
		logger.log(Level.INFO, "- Downloading and extracting multichunks ...");
		TransferManager transferManager = config.getConnection().createTransferManager();
		
		// TODO [medium] Check existing files by checksum and do NOT download them if they exist locally, or copy them 
		
		for (MultiChunkEntry multiChunkEntry : unknownMultiChunks) {
			File localEncryptedMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId());
			File localDecryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkEntry.getId());
			MultiChunkRemoteFile remoteMultiChunkFile = new MultiChunkRemoteFile(localEncryptedMultiChunkFile.getName()); // TODO [low] Make MultiChunkRemoteFile class, or something like that
			
			logger.log(Level.INFO, "  + Downloading multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" ...");
			transferManager.download(remoteMultiChunkFile, localEncryptedMultiChunkFile);
			
			logger.log(Level.INFO, "  + Extracting multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" ...");
			InputStream multiChunkInputStream = config.getTransformer().createInputStream(new FileInputStream(localEncryptedMultiChunkFile));			
			OutputStream decryptedMultiChunkOutputStream = new FileOutputStream(localDecryptedMultiChunkFile); 			

			FileUtil.appendToOutputStream(multiChunkInputStream, decryptedMultiChunkOutputStream);
			
			decryptedMultiChunkOutputStream.close();
			multiChunkInputStream.close();
			
			logger.log(Level.FINE, "  + Locally deleting multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" ...");
			localEncryptedMultiChunkFile.delete();
		}
		
		transferManager.disconnect();
	}
	
	private Set<MultiChunkEntry> determineUnknownMultiChunks(Database database, Database winnersDatabase, Cache cache) {
		logger.log(Level.INFO, "- Determine new multichunks to download ...");
		
		Collection<PartialFileHistory> newOrChangedFileHistories = winnersDatabase.getFileHistories();
		Set<MultiChunkEntry> multiChunksToDownload = new HashSet<MultiChunkEntry>();		
		
		for (PartialFileHistory fileHistory : newOrChangedFileHistories) {
			FileVersion lastFileVersion = fileHistory.getLastVersion();

			// TODO [medium] If file has been renamed, all chunks are downloaded
			// Note: This cannot be fixed by adding a test for RENAMED here, because if a file is completely new to a
			//       client it is not "RENAMED" for him, it is new. 
			
			if (lastFileVersion.getType() == FileType.FILE && lastFileVersion.getStatus() != FileStatus.DELETED) {				
				FileContent fileContent = database.getContent(lastFileVersion.getChecksum());
				
				if (fileContent == null) {
					fileContent = winnersDatabase.getContent(lastFileVersion.getChecksum());
				}
				
				if (fileContent != null) { // File can be empty!					
					Collection<ChunkEntryId> fileChunks = fileContent.getChunks();
					
					for (ChunkEntryId chunkChecksum : fileChunks) {
						File chunkFileInCache = cache.getChunkFile(chunkChecksum.getArray()); // TODO [medium] Chunk files do not exist anymore. Remove this
						
						if (!chunkFileInCache.exists()) {
							MultiChunkEntry multiChunkForChunk = database.getMultiChunkForChunk(chunkChecksum);
							
							if (multiChunkForChunk == null) {
								multiChunkForChunk = winnersDatabase.getMultiChunkForChunk(chunkChecksum); 
							}
							
							if (!multiChunksToDownload.contains(multiChunkForChunk)) {
								logger.log(Level.INFO, "  + Adding multichunk "+StringUtil.toHex(multiChunkForChunk.getId())+" to download list ...");
								multiChunksToDownload.add(multiChunkForChunk);
							}
						}
					}
				}
			}
		}
		
		return multiChunksToDownload;
	}

	private Database readWinnersDatabase(Branch winnersApplyBranch, List<File> remoteDatabases) throws IOException {
		// Make map 'short filename' -> 'full filename'
		Map<String, File> shortFilenameToFileMap = new HashMap<String, File>();
		
		for (File remoteDatabase : remoteDatabases) {
			shortFilenameToFileMap.put(remoteDatabase.getName(), remoteDatabase);
		}
		
		// Load individual databases for branch ranges
		DatabaseDAO databaseDAO = new DatabaseXmlDAO(config.getTransformer());
		Database winnerBranchDatabase = new Database(); // Database cannot be reused, since these might be different clients
		
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
			
			String potentialDatabaseShortFileNameForRange = "db-"+clientName+"-"+clientVersionTo.get(clientName);
			File databaseFileForRange = shortFilenameToFileMap.get(potentialDatabaseShortFileNameForRange);
			
			if (databaseFileForRange != null) {
				// Load database
				logger.log(Level.INFO, "- Loading "+databaseFileForRange+" (from "+clientVersionFrom+", to "+clientVersionTo+") ...");
				databaseDAO.load(winnerBranchDatabase, databaseFileForRange, clientVersionFrom, clientVersionTo);
						
				// Reset range
				clientName = null;
				clientVersionFrom = null;
				clientVersionTo = null;
			}			
		}
		
		return winnerBranchDatabase;		
	}

	private Branches readUnknownDatabaseVersionHeaders(List<File> remoteDatabases) throws IOException {
		logger.log(Level.INFO, "Loading database headers, creating branches ...");
		// Sort files (db-a-1 must be before db-a-2 !)
		Collections.sort(remoteDatabases, new DatabaseFileComparator()); // TODO [medium] natural sort is a workaround, database file names should be centrally managed, db-name-0000000009 avoids natural sort  
		
		// Read database files
		Branches unknownRemoteBranches = new Branches();
		DatabaseDAO dbDAO = new DatabaseXmlDAO(config.getTransformer());
		
		for (File remoteDatabaseFileInCache : remoteDatabases) {
			Database remoteDatabase = new Database(); // Database cannot be reused, since these might be different clients
		
			RemoteDatabaseFile remoteDatabaseFile = new RemoteDatabaseFile(remoteDatabaseFileInCache);
			dbDAO.load(remoteDatabase, remoteDatabaseFile.getFile());		// TODO [medium] Performance: This is very, very, very inefficient, DB is loaded and then discarded	
			List<DatabaseVersion> remoteDatabaseVersions = remoteDatabase.getDatabaseVersions();			
			
			// Pupulate branches
			Branch remoteClientBranch = unknownRemoteBranches.getBranch(remoteDatabaseFile.getClientName(), true);
			
			for (DatabaseVersion remoteDatabaseVersion : remoteDatabaseVersions) {
				DatabaseVersionHeader header = remoteDatabaseVersion.getHeader();
				remoteClientBranch.add(header);
			}
		}
		
		return unknownRemoteBranches;
	}

	private List<RemoteFile> listUnknownRemoteDatabases(Database database, TransferManager transferManager) throws Exception {
		return ((RemoteStatusOperationResult) new LsRemoteOperation(config, database, transferManager).execute()).getUnknownRemoteDatabases();
	}
	
	private List<File> downloadUnknownRemoteDatabases(TransferManager transferManager, List<RemoteFile> unknownRemoteDatabases) throws StorageException {
		logger.log(Level.INFO, "Downloading unknown databases.");
		List<File> unknownRemoteDatabasesInCache = new ArrayList<File>();
		
		for (RemoteFile remoteFile : unknownRemoteDatabases) {
			File unknownRemoteDatabaseFileInCache = config.getCache().getDatabaseFile(remoteFile.getName());

			logger.log(Level.INFO, "- Downloading {0} to local cache at {1}", new Object[] { remoteFile.getName(), unknownRemoteDatabaseFileInCache });
			transferManager.download(new DatabaseRemoteFile(remoteFile.getName(), remoteFile.getSource()), unknownRemoteDatabaseFileInCache);
						
			unknownRemoteDatabasesInCache.add(unknownRemoteDatabaseFileInCache);
		}
		
		return unknownRemoteDatabasesInCache;
	}		
	
	/**
	 * Sorts file system actions according to their natural order to prevent scenarios 
	 * in which a non-empty directory is deleted, ...
	 * 
	 * TODO [low] write unit test for FileSystemActionComparator, maybe move it in own class
	 */
	public static class FileSystemActionComparator implements Comparator<FileSystemAction> {
		private static final Object[][] TARGET_ORDER =  new Object[][] {
			new Object[] { DeleteFileSystemAction.class, FileType.FILE}, 
			new Object[] { NewFileSystemAction.class, FileType.FOLDER },
			new Object[] { RenameFileSystemAction.class, FileType.FOLDER },
			new Object[] { NewFileSystemAction.class, FileType.FILE },
			new Object[] { RenameFileSystemAction.class, FileType.FILE },
			new Object[] { ChangeFileSystemAction.class, FileType.FOLDER },
			new Object[] { ChangeFileSystemAction.class, FileType.FILE },
			new Object[] { DeleteFileSystemAction.class, FileType.FOLDER },
		};
				
		@Override
		public int compare(FileSystemAction a1, FileSystemAction a2) {
			int a1Position = determinePosition(a1);
			int a2Position = determinePosition(a2);
			
			if (a1Position > a2Position) {
				return 1;
			}
			else if (a1Position < a2Position) {
				return -1;
			}
			
			return compareByFullName(a1, a2);
		}
				
		private int compareByFullName(FileSystemAction a1, FileSystemAction a2) {
			// For renamed/deleted, do the longest path first
			if (a1.getClass().equals(DeleteFileSystemAction.class) || a1.getClass().equals(RenameFileSystemAction.class)) {
				return -1 * a1.getFile2().getFullName().compareTo(a2.getFile2().getFullName());
			}
			
			// For the rest, do the shortest path first
			else if (a1.getClass().equals(NewFileSystemAction.class) || a1.getClass().equals(ChangeFileSystemAction.class)) {
				return a1.getFile2().getFullName().compareTo(a2.getFile2().getFullName());
			}
			
			return 0;
		}

		@SuppressWarnings("rawtypes")
		private int determinePosition(FileSystemAction a) {
			for (int i=0; i<TARGET_ORDER.length; i++) {
				Class targetClass = (Class) TARGET_ORDER[i][0];
				FileType targetFileType = (FileType) TARGET_ORDER[i][1];
				
				if (a.getClass().equals(targetClass) && a.getType() == targetFileType) {					
					return i;
				}
			}
			
			return -1;
		}
	}
	
	// TODO [medium] Duplicate code in SyncUpOperation
	public static class DatabaseFileComparator implements Comparator<File> { // TODO [low] Database file structure and natural sort are a workaround
		@Override
		public int compare(File f1, File f2) {
			RemoteDatabaseFile r1 = new RemoteDatabaseFile(f1);
			RemoteDatabaseFile r2 = new RemoteDatabaseFile(f2);
			
			int clientNameCompare = r1.getClientName().compareTo(r2.getClientName());
			
			if (clientNameCompare != 0) {
				return clientNameCompare;
			}
			else {
				return (int) (r1.getClientVersion() - r2.getClientVersion());
			}
		}
		
	}	

	public static class DownOperationOptions implements OperationOptions {
		// Nothing here yet.
	}
	
	public class DownOperationResult implements OperationResult {
		private ChangeSet changeSet = new ChangeSet();
		
		public void setChangeSet(ChangeSet ChangeSet) {
			this.changeSet = ChangeSet;
		}
		
		public ChangeSet getChangeSet() {
			return changeSet;
		}
	}
}
