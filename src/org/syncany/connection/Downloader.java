package org.syncany.connection;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.CacheException;
import org.syncany.config.Profile;
import org.syncany.connection.plugins.RemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;

public class Downloader {
	private static final Logger logger = Logger.getLogger(Downloader.class.getSimpleName());
	
    private TransferManager transfer;
	
	public File downloadMetaChunk(Profile profile, String metaChunkFilename, String chunkIdStr) throws CacheException, IOException {
		if(profile!=null && transfer==null) {
			transfer = profile.getConnection().createTransferManager();
		}
		
		// Download metachunk
//		File encryptedMetaChunkFile = Profile.getInstance().getCache().createTempFile("metachunk-"+ CloneChunk.getMetaIdStr(chunkIdStr));
		File encryptedMetaChunkFile = profile.getCache().createTempFile("enc-multichunk-"+ metaChunkFilename);
		encryptedMetaChunkFile.deleteOnExit();
		
		try {
			transfer.download(new RemoteFile(metaChunkFilename), encryptedMetaChunkFile);
		} catch (StorageException e) {
			if (logger.isLoggable(Level.WARNING)) {
				logger.log(Level.WARNING, "  ERR: Metachunk "+ metaChunkFilename + " not found", e);
			}

			throw new IOException(e);
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
