package org.syncany.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Deduper;
import org.syncany.config.Config;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.VectorClock;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

public class SyncUpOperation extends Operation {
	private static final Logger logger = Logger.getLogger(SyncUpOperation.class.getSimpleName());
	
	private TransferManager transferManager; 
	
	public SyncUpOperation(Config config) {
		super(config);
		transferManager = config.getConnection().createTransferManager();
	}	
	
	public void execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Sync up' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		logger.log(Level.INFO, "Loading local database ...");		
		Database db = loadLocalDatabase(config.getAppDatabaseFile());
		
		logger.log(Level.INFO, "Starting index process ...");
		List<File> localFiles = FileUtil.getRecursiveFileList(config.getLocalDir(), true);
		DatabaseVersion lastDirtyDatabaseVersion = index(localFiles, db);
		
		if (lastDirtyDatabaseVersion.getFileHistories().size() == 0) {
			logger.log(Level.INFO, "Local database is up-to-date. NOTHING TO DO!");
		}
		else {
			logger.log(Level.INFO, "Adding newest database version "+lastDirtyDatabaseVersion.getHeader()+" to local database ...");
			db.addDatabaseVersion(lastDirtyDatabaseVersion);
	
			logger.log(Level.INFO, "Saving local database to file "+config.getAppDatabaseFile()+" ...");
			saveLocalDatabase(db, config.getAppDatabaseFile());
			
			logger.log(Level.INFO, "Uploading new multichunks ...");
			boolean uploadMultiChunksSuccess = uploadMultiChunks(db.getLastDatabaseVersion().getMultiChunks());
			
			if (uploadMultiChunksSuccess) {
				long newestLocalDatabaseVersion = lastDirtyDatabaseVersion.getVectorClock().get(config.getMachineName());
	
				RemoteFile remoteDeltaDatabaseFile = new RemoteFile("db-"+config.getMachineName()+"-"+newestLocalDatabaseVersion);
				File localDeltaDatabaseFile = config.getCache().getDatabaseFile(remoteDeltaDatabaseFile.getName());	
	
				logger.log(Level.INFO, "Saving local delta database file ...");
				logger.log(Level.INFO, "- Saving versions from: "+lastDirtyDatabaseVersion.getHeader()+", to: "+lastDirtyDatabaseVersion.getHeader()+") to file "+localDeltaDatabaseFile+" ...");
				saveLocalDatabase(db, lastDirtyDatabaseVersion, lastDirtyDatabaseVersion, localDeltaDatabaseFile);
				
				logger.log(Level.INFO, "- Uploading local delta database file ...");
				uploadLocalDatabase(localDeltaDatabaseFile, remoteDeltaDatabaseFile);			
			}
			else {
				throw new Exception("aa");
			}
		}
	}	
	
	private boolean uploadMultiChunks(Collection<MultiChunkEntry> multiChunksEntries) throws InterruptedException, StorageException {

		for (MultiChunkEntry multiChunkEntry : multiChunksEntries) {
			File localMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId());
			RemoteFile remoteMultiChunkFile = new RemoteFile(localMultiChunkFile.getName());
			
			logger.log(Level.INFO, "- Uploading multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" from "+localMultiChunkFile+" to "+remoteMultiChunkFile+" ...");
			transferManager.upload(localMultiChunkFile, remoteMultiChunkFile);
			
			logger.log(Level.INFO, "  + Removing "+StringUtil.toHex(multiChunkEntry.getId())+" locally ...");
			localMultiChunkFile.delete();
		}
		
		return true; // FIXME
	}

	private boolean uploadLocalDatabase(File localDatabaseFile, RemoteFile remoteDatabaseFile) throws InterruptedException, StorageException {		
		logger.log(Level.INFO, "- Uploading "+localDatabaseFile+" to "+remoteDatabaseFile+" ..."); 		
		transferManager.upload(localDatabaseFile, remoteDatabaseFile);
		
		return true;
	}

	private DatabaseVersion index(List<File> localFiles, Database db) throws FileNotFoundException, IOException {			
		// Get last vector clock
		String previousClient = null;
		VectorClock lastVectorClock = null;
		
		if (db.getLastDatabaseVersion() != null) {
			previousClient = db.getLastDatabaseVersion().getClient();
			lastVectorClock = db.getLastDatabaseVersion().getVectorClock();
		}
		else {
			previousClient = null;
			lastVectorClock = new VectorClock();
		}
		
		// New vector clock
		VectorClock newVectorClock = lastVectorClock.clone();

		Long lastLocalValue = lastVectorClock.getClock(config.getMachineName());
		Long newLocalValue = (lastLocalValue == null) ? 1 : lastLocalValue+1;
		
		newVectorClock.setClock(config.getMachineName(), newLocalValue);		

		// Index
		Deduper deduper = new Deduper(config.getChunker(), config.getMultiChunker(), config.getTransformer());
		Indexer indexer = new Indexer(config, deduper, db);
		
		DatabaseVersion newDatabaseVersion = indexer.index(localFiles);	
	
		newDatabaseVersion.setVectorClock(newVectorClock);
		newDatabaseVersion.setTimestamp(new Date());	
		newDatabaseVersion.setClient(config.getMachineName());
		newDatabaseVersion.setPreviousClient(previousClient);
						
		return newDatabaseVersion;
	}
}
