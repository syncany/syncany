package org.syncany.connection;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Profile;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.db.CloneChunk;
import org.syncany.exceptions.CacheException;
import org.syncany.exceptions.InconsistentFileSystemException;
import org.syncany.exceptions.StorageException;
import org.syncany.watch.remote.files.RemoteFile;

public class Downloader {

	private static final Logger logger = Logger.getLogger(Downloader.class.getSimpleName());
	
    private TransferManager transfer;
    private Profile profile;
	
	public Downloader(Profile profile) {
		this.profile = profile;
	}
	
	public File downloadMetaChunk(String metaChunkFilename, String chunkIdStr) throws CacheException, InconsistentFileSystemException {
		if(profile!=null && transfer==null) {
			transfer = profile.getRepository().getConnection().createTransferManager();
		}
		
		// Download metachunk
//		File encryptedMetaChunkFile = Profile.getInstance().getCache().createTempFile("metachunk-"+ CloneChunk.getMetaIdStr(chunkIdStr));
		File encryptedMetaChunkFile = profile.getCache().createTempFile("enc-chunk-"+ CloneChunk.getMetaIdStr(chunkIdStr));
		encryptedMetaChunkFile.deleteOnExit();
		
		try {
			transfer.download(new RemoteFile(metaChunkFilename), encryptedMetaChunkFile);
		} catch (StorageException e) {
			if (logger.isLoggable(Level.WARNING)) {
				logger.log(Level.WARNING, "  ERR: Metachunk "+ metaChunkFilename + " not found", e);
			}

			throw new InconsistentFileSystemException(e);
		}
		
		return encryptedMetaChunkFile;
		
	}
	
	public void disconnect() {
		if (transfer != null) {
			try { transfer.disconnect(); } 
			catch (StorageException ex) { }
		}
	}
	
}
