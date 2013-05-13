package org.syncany.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.chunk.Deduper;
import org.syncany.config.Profile;
import org.syncany.connection.Uploader;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.db.Database;
import org.syncany.db.DatabaseVersion;
import org.syncany.db.MultiChunkEntry;
import org.syncany.util.FileUtil;

public class SyncUpOperation extends Operation {
	private static final Logger logger = Logger.getLogger(SyncUpOperation.class.getSimpleName());
	
	private Uploader uploader;
	
	public SyncUpOperation(Profile profile) {
		super(profile);
		this.uploader = new Uploader(profile.getConnection());
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
	
	private boolean uploadMultiChunks(Collection<MultiChunkEntry> multiChunksEntries) throws InterruptedException {
		uploader.start();

		for (MultiChunkEntry multiChunkEntry : multiChunksEntries) {
			File multiChunkFile = profile.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId()); 
			uploader.queue(multiChunkFile);
		}
		
		return true; // FIXME
	}

	private boolean uploadLocalDatabase(File localDatabaseFile, long newestLocalDatabaseVersion) throws InterruptedException {
		RemoteFile remoteDatabaseFile = new RemoteFile("db-"+profile.getMachineName()+"-"+newestLocalDatabaseVersion);
		
		uploader.queue(localDatabaseFile, remoteDatabaseFile);
		uploader.stopWhenDone();
		
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
