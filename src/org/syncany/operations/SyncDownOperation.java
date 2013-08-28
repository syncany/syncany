package org.syncany.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.syncany.chunk.Chunk;
import org.syncany.chunk.MultiChunk;
import org.syncany.config.Cache;
import org.syncany.config.Config;
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
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.VectorClock;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class SyncDownOperation extends Operation {
	private static final Logger logger = Logger.getLogger(SyncDownOperation.class.getSimpleName());
	
	public SyncDownOperation(Config config) {
		super(config);
	}	
	
	public void execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync down' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");		
		
		File localDatabaseFile = new File(config.getAppDatabaseDir()+"/local.db");		
		Database localDatabase = loadLocalDatabase(localDatabaseFile);
		
		// 0. Create TM
		TransferManager transferManager = config.getConnection().createTransferManager();

		// 1. Check which remote databases to download based on the last local vector clock		
		List<RemoteFile> unknownRemoteDatabases = listUnknownRemoteDatabases(localDatabase, transferManager);
		
		if (unknownRemoteDatabases.isEmpty()) {
			logger.log(Level.INFO, "* Nothing new. Skipping down operation.");
			return;
		}
		
		// 2. Download the remote databases to the local cache folder
		List<File> unknownRemoteDatabasesInCache = downloadUnknownRemoteDatabases(transferManager, unknownRemoteDatabases);
		
		// 3. Read version headers (vector clocks)
		Branches unknownRemoteBranches = readUnknownDatabaseVersionHeaders(unknownRemoteDatabasesInCache);
		
		// 4. Determine winner branch
		logger.log(Level.INFO, "Detect updates and conflicts ...");
		logger.log(Level.INFO, "- DatabaseVersionUpdateDetector results:");
		DatabaseReconciliator databaseReconciliator = new DatabaseReconciliator();
		
		Branch localBranch = localDatabase.getBranch();	
		Branches stitchedRemoteBranches = databaseReconciliator.stitchRemoteBranches(unknownRemoteBranches, config.getMachineName(), localBranch);
		//  TODO FIXME IMPORTANT Does this stitching make all the other algorithms obsolete?
		//  TODO FIXME IMPORTANT --> Couldn't findWinnersWinnersLastDatabaseVersionHeader algorithm (walk forward, compare) be used instead?                       

		Branches allStitchedBranches = stitchedRemoteBranches.clone();
		allStitchedBranches.add(config.getMachineName(), localBranch);
		
		DatabaseVersionHeader lastCommonHeader = databaseReconciliator.findLastCommonDatabaseVersionHeader(localBranch, allStitchedBranches);		
		TreeMap<String, DatabaseVersionHeader> firstConflictingHeaders = databaseReconciliator.findFirstConflictingDatabaseVersionHeader(lastCommonHeader, allStitchedBranches);		
		TreeMap<String, DatabaseVersionHeader> winningFirstConflictingHeaders = databaseReconciliator.findWinningFirstConflictingDatabaseVersionHeaders(firstConflictingHeaders);		
		Entry<String, DatabaseVersionHeader> winnersWinnersLastDatabaseVersionHeader = databaseReconciliator.findWinnersWinnersLastDatabaseVersionHeader(winningFirstConflictingHeaders, allStitchedBranches);
		
		logger.log(Level.FINER, "   + localBranch: "+localBranch);
		logger.log(Level.FINER, "   + fullRemoteBranches: "+allStitchedBranches);
		logger.log(Level.FINER, "   + unknownRemoteBranches: "+unknownRemoteBranches);
		logger.log(Level.FINER, "   + allStitchedBranches: "+allStitchedBranches);
		logger.log(Level.FINER, "   + lastCommonHeader: "+lastCommonHeader);
		logger.log(Level.FINER, "   + firstConflictingHeaders: "+firstConflictingHeaders);
		logger.log(Level.FINER, "   + winningFirstConflictingHeaders: "+winningFirstConflictingHeaders);
		logger.log(Level.FINER, "   + winnersWinnersLastDatabaseVersionHeader: "+winnersWinnersLastDatabaseVersionHeader);

		String winnersName = winnersWinnersLastDatabaseVersionHeader.getKey();
		Branch winnersBranch = allStitchedBranches.getBranch(winnersName);
		
		logger.log(Level.INFO, "- Compared branches: "+allStitchedBranches);
		logger.log(Level.INFO, "- Winner is "+winnersName+" with branch "+winnersBranch);
				
		if (config.getMachineName().equals(winnersName)) {
			if (winnersBranch.size() > localBranch.size()) {
				throw new RuntimeException("TODO implement use case 'restore remote backup'");
			}
			
			logger.log(Level.INFO, "- I won, nothing to do locally");
		}
		else {
			logger.log(Level.INFO, "- Someone else won, now determine what to do ...");
			
			Branch localPruneBranch = databaseReconciliator.findLosersPruneBranch(localBranch, winnersBranch);
			logger.log(Level.INFO, "- Database versions to REMOVE locally: "+localPruneBranch);
			
			if (localPruneBranch.size() == 0) {
				logger.log(Level.INFO, "  + Nothing to prune locally. No conflicts. Only updates. Nice!");
			}
			else {
				logger.log(Level.INFO, "  + Pruning databases locally ...");
				
				for (DatabaseVersionHeader databaseVersionHeader : localPruneBranch.getAll()) {
					logger.log(Level.INFO, "    * Removing "+databaseVersionHeader+" ...");
					localDatabase.removeDatabaseVersion(localDatabase.getDatabaseVersion(databaseVersionHeader.getVectorClock()));
				}
					
				// TODO What to do with the prune branch?? Do something on filesystem!!
				logger.log(Level.WARNING, "  + TODO Prune on file system!");
			}
			
			Branch winnersApplyBranch = databaseReconciliator.findWinnersApplyBranch(localBranch, winnersBranch);
			logger.log(Level.INFO, "- Database versions to APPLY locally: "+winnersApplyBranch);
			
			if (winnersApplyBranch.size() == 0) {
				logger.log(Level.WARNING, "   ++++ NOTHING TO UPDATE FROM WINNER. This should not happen.");
			}
			else {
				logger.log(Level.INFO, "- Loading winners database ...");				
				Database winnersDatabase = readWinnersDatabase(winnersApplyBranch, unknownRemoteDatabasesInCache); //readClientDatabase(winnersName, unknownRemoteDatabasesInCache);
				

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
				

				logger.log(Level.INFO, "- Saving local database to "+localDatabaseFile+" ...");
				saveLocalDatabase(localDatabase, localDatabaseFile);
			}
			
		}		
		
		//throw new Exception("Not yet fully implemented.");
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
			boolean isDeletedFile = !isNewFile && isInSamePlace && winningLastVersion.getStatus() == FileStatus.DELETED;
			
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "      + isNewFile: "+isNewFile+", isInSamePlace:"+isInSamePlace+", isChecksumEqual: "+isChecksumEqual
						+", isChangedFile: "+isChangedFile+", isIdenticalFile: "+isIdenticalFile+", isRenamedFile: "+isRenamedFile+", isDeletedFile: "+isDeletedFile);
			}
			
			if (isNewFile) {
				fileSystemActions.add(new NewFileSystemAction(config, winningLastVersion, localDatabase, winnersDatabase));
				logger.log(Level.INFO, "      NEW FILE");
			}
			else if (isChangedFile) {			
				fileSystemActions.add(new ChangeFileSystemAction(config, localLastVersion, winningLastVersion, localDatabase, winnersDatabase));
				logger.log(Level.INFO, "      CHANGED FILE");
			}
			else if (isRenamedFile) {
				fileSystemActions.add(new RenameFileSystemAction(config, localLastVersion, winningLastVersion,localDatabase, winnersDatabase));
				logger.log(Level.INFO, "      RENAMED FILE");
			}
			else if (isDeletedFile) {
				fileSystemActions.add(new DeleteFileSystemAction(config, localLastVersion, winningLastVersion, localDatabase, winnersDatabase));
				logger.log(Level.INFO, "      DELETED FILE");
			}
			else if (isIdenticalFile) {
				logger.log(Level.INFO, "      IDENTICAL FILE, nothing to do!");
			}
			else {
				logger.log(Level.WARNING, "      THIS SHOULD NOT HAPPEN"); // TODO
				throw new Exception("Cannot determine file system action!");
			}
		}
				
		return fileSystemActions;
	}
	
	private void applyFileSystemActions(List<FileSystemAction> actions) throws Exception {
		for (FileSystemAction action : actions) {
			action.execute();
		}
	}
	
	private void downloadAndExtractMultiChunks(Set<MultiChunkEntry> unknownMultiChunks) throws StorageException, IOException {
		logger.log(Level.INFO, "- Downloading and extracting multichunks ...");
		TransferManager transferManager = config.getConnection().createTransferManager();
		
		for (MultiChunkEntry multiChunkEntry : unknownMultiChunks) {
			File localMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId());
			RemoteFile remoteMultiChunkFile = new RemoteFile(localMultiChunkFile.getName()); // TODO Make MultiChunkRemoteFile class, or something like that
			
			logger.log(Level.INFO, "  + Downloading multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" ...");
			transferManager.download(remoteMultiChunkFile, localMultiChunkFile);
			
			logger.log(Level.INFO, "  + Extracting multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" ...");
			MultiChunk multiChunkInCache = config.getMultiChunker().createMultiChunk(
					config.getTransformer().transform(new FileInputStream(localMultiChunkFile)));
			Chunk extractedChunk = null;
			
			while (null != (extractedChunk = multiChunkInCache.read())) {
				File localChunkFile = config.getCache().getChunkFile(extractedChunk.getChecksum());
				
				logger.log(Level.INFO, "    * Unpacking chunk "+StringUtil.toHex(extractedChunk.getChecksum())+" ...");
				FileUtil.writeToFile(extractedChunk.getContent(), localChunkFile);
			}
			
			logger.log(Level.INFO, "  + Removing multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" ...");
			localMultiChunkFile.delete();
		}
		
		transferManager.disconnect();
	}

	private Set<MultiChunkEntry> determineUnknownMultiChunks(Database database, Database winnersDatabase, Cache cache) {
		logger.log(Level.INFO, "- Determine new multichunks to download ...");
		
		Collection<PartialFileHistory> newOrChangedFileHistories = winnersDatabase.getFileHistories();
		Set<MultiChunkEntry> multiChunksToDownload = new HashSet<MultiChunkEntry>();		
		
		for (PartialFileHistory fileHistory : newOrChangedFileHistories) {
			if (!fileHistory.getLastVersion().isFolder()) {
				FileContent fileContent = database.getContent(fileHistory.getLastVersion().getChecksum());
				
				if (fileContent == null) {
					fileContent = winnersDatabase.getContent(fileHistory.getLastVersion().getChecksum());
				}
				
				Collection<ChunkEntryId> fileChunks = fileContent.getChunks(); // TODO [high] change this to getChunkRefs, solves cross referencing in database versions
				
				for (ChunkEntryId chunkChecksum : fileChunks) {
					File chunkFileInCache = cache.getChunkFile(chunkChecksum.getArray());
					
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
		
		return multiChunksToDownload;
	}

	private Database readWinnersDatabase(Branch winnersApplyBranch, List<File> remoteDatabases) throws IOException {
		// Make map 'short filename' -> 'full filename'
		Map<String, File> shortFilenameToFileMap = new HashMap<String, File>();
		
		for (File remoteDatabase : remoteDatabases) {
			shortFilenameToFileMap.put(remoteDatabase.getName(), remoteDatabase);
		}
		
		// Load individual databases for branch ranges
		DatabaseDAO databaseDAO = new DatabaseXmlDAO();
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
				databaseDAO.load(winnerBranchDatabase, new RemoteDatabaseFile(databaseFileForRange), clientVersionFrom, clientVersionTo);
						
				// Reset range
				clientName = null;
				clientVersionFrom = null;
				clientVersionTo = null;
			}
			
			// TODO FIXME WARNING: This currently does not work for this situation:
			logger.log(Level.WARNING, "TODO WARNING: This currently does not work for this situation: db-A-500  and branch A1-A5, because db-A-5 does NOT exist!!");
			//   - db-A-500  and branch A1-A5, because db-A-5 does NOT exist!!
			//   - To IMPLEMENT: if end of client range is reached (here: A5), load from next highest db-file of A (here: db-A-500)
		}
		
		return winnerBranchDatabase;		
	}

	private Branches readUnknownDatabaseVersionHeaders(List<File> remoteDatabases) throws IOException {
		logger.log(Level.INFO, "Loading database headers, creating branches ...");
		// Sort files (db-a-1 must be before db-a-2 !)
		Collections.sort(remoteDatabases);
		
		// Read database files
		Branches unknownRemoteBranches = new Branches();
		DatabaseDAO dbDAO = new DatabaseXmlDAO();
		
		for (File remoteDatabaseInCache : remoteDatabases) {
			Database remoteDatabase = new Database(); // Database cannot be reused, since these might be different clients
		
			RemoteDatabaseFile rdf = new RemoteDatabaseFile(remoteDatabaseInCache);
			dbDAO.load(remoteDatabase, rdf);		// FIXME [medium] This is very, very, very inefficient, DB is loaded and then discarded	
			List<DatabaseVersion> remoteDatabaseVersions = remoteDatabase.getDatabaseVersions();			
			
			// Pupulate branches
			Branch remoteClientBranch = unknownRemoteBranches.getBranch(rdf.getClientName(), true);
			
			for (DatabaseVersion remoteDatabaseVersion : remoteDatabaseVersions) {
				DatabaseVersionHeader header = remoteDatabaseVersion.getHeader();
				remoteClientBranch.add(header);
			}
		}
		
		return unknownRemoteBranches;
	}

	private List<RemoteFile> listUnknownRemoteDatabases(Database db, TransferManager transferManager) throws StorageException {
		logger.log(Level.INFO, "Retrieving remote database list.");
		
		List<RemoteFile> unknownRemoteDatabasesList = new ArrayList<RemoteFile>();

		Map<String, RemoteFile> remoteDatabaseFiles = transferManager.list("db-");
		
		// No local database yet
		if (db.getLastDatabaseVersion() == null) {
			return new ArrayList<RemoteFile>(remoteDatabaseFiles.values());
		}
		
		// At least one local database version exists
		else {
			VectorClock knownDatabaseVersions = db.getLastDatabaseVersion().getVectorClock();
			
			for (RemoteFile remoteFile : remoteDatabaseFiles.values()) {
				RemoteDatabaseFile remoteDatabaseFile = new RemoteDatabaseFile(remoteFile.getName());
				
				String clientName = remoteDatabaseFile.getClientName();
				Long knownClientVersion = knownDatabaseVersions.get(clientName);
						
				if (knownClientVersion != null) {
					if (remoteDatabaseFile.getClientVersion() > knownClientVersion) {
						logger.log(Level.INFO, "- Remote database {0} is new.", remoteFile.getName());
						unknownRemoteDatabasesList.add(remoteFile);
					}
					else {
						logger.log(Level.INFO, "- Remote database {0} is already known. Ignoring.", remoteFile.getName());
						// Do nothing. We know this database.
					}
				}
				
				else {
					logger.log(Level.INFO, "- Remote database {0} is new.", remoteFile.getName());
					unknownRemoteDatabasesList.add(remoteFile);
				}				
			}
			
			return unknownRemoteDatabasesList;			
		}
	}
	
	private List<File> downloadUnknownRemoteDatabases(TransferManager transferManager, List<RemoteFile> unknownRemoteDatabases) throws StorageException {
		logger.log(Level.INFO, "Downloading unknown databases.");
		List<File> unknownRemoteDatabasesInCache = new ArrayList<File>();
		
		for (RemoteFile remoteFile : unknownRemoteDatabases) {
			File unknownRemoteDatabaseFileInCache = config.getCache().getDatabaseFile(remoteFile.getName());

			logger.log(Level.INFO, "- Downloading {0} to local cache at {1}", new Object[] { remoteFile.getName(), unknownRemoteDatabaseFileInCache });
			transferManager.download(remoteFile, unknownRemoteDatabaseFileInCache);
			
			unknownRemoteDatabasesInCache.add(unknownRemoteDatabaseFileInCache);
		}
		
		return unknownRemoteDatabasesInCache;
	}		

}
