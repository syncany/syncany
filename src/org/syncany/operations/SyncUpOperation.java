package org.syncany.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
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
import org.syncany.util.FileUtil;

public class SyncUpOperation extends Operation {
	private static final Logger logger = Logger.getLogger(SyncUpOperation.class.getSimpleName());
	
	private TransferManager tm; 
	
	public SyncUpOperation(Config config) {
		super(config);
		tm = config.getConnection().createTransferManager();
	}	
	
	public void execute() throws Exception {
		logger.log(Level.INFO, "Sync up ...");
		
		File localDatabaseFile = new File(profile.getAppDatabaseDir()+"/local.db");		
		Database db = loadLocalDatabase(localDatabaseFile);
		
		List<File> localFiles = FileUtil.getRecursiveFileList(profile.getLocalDir());
		long newestLocalDatabaseVersion = index(localFiles, db);
		
		long fromLocalDatabaseVersion = (newestLocalDatabaseVersion-1 >= 1) ? newestLocalDatabaseVersion : 1;		
		saveLocalDatabase(db, fromLocalDatabaseVersion, newestLocalDatabaseVersion, localDatabaseFile);
		
		boolean uploadMultiChunksSuccess = uploadMultiChunks(db.getLastDatabaseVersion().getMultiChunks());
		
		if (uploadMultiChunksSuccess) {
			boolean uploadLocalDatabaseSuccess = uploadLocalDatabase(localDatabaseFile, newestLocalDatabaseVersion);			
		}
		else {
			throw new Exception("aa");
		}		
	}	
	
	private boolean uploadMultiChunks(Collection<MultiChunkEntry> multiChunksEntries) throws InterruptedException, StorageException {

		for (MultiChunkEntry multiChunkEntry : multiChunksEntries) {
			File multiChunkFile = profile.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId());
			RemoteFile remoteFile = new RemoteFile(multiChunkFile.getName());
			tm.upload(multiChunkFile, remoteFile);
		}
		
		return true; // FIXME
	}

	private boolean uploadLocalDatabase(File localDatabaseFile, long newestLocalDatabaseVersion) throws InterruptedException, StorageException {
		RemoteFile remoteDatabaseFile = new RemoteFile("db-"+profile.getMachineName()+"-"+newestLocalDatabaseVersion);
		
		tm.upload(localDatabaseFile, remoteDatabaseFile);
		
		return true;
	}

	private long index(List<File> localFiles, Database db) throws FileNotFoundException, IOException {		
		Deduper deduper = new Deduper(profile.getChunker(), profile.getMultiChunker(), profile.getTransformer());
		Indexer indexer = new Indexer(profile, deduper, db);
		
		DatabaseVersion dbv = indexer.index(localFiles);		
		db.addDatabaseVersion(dbv);
						
		return db.getLastLocalDatabaseVersion();
	}
}
