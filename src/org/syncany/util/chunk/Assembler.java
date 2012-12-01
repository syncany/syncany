package org.syncany.util.chunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import org.syncany.config.Profile;
import org.syncany.connection.Downloader;
import org.syncany.db.CloneChunk;
import org.syncany.util.chunk.Chunk;
import org.syncany.util.exceptions.EncryptionException;
import org.syncany.util.exceptions.InconsistentFileSystemException;
import org.syncany.util.FileUtil;

public class Assembler {

	private static final Logger logger = Logger.getLogger(Assembler.class.getSimpleName());
	
	private Downloader downloader;
	private Profile profile;
	
	public Assembler(Profile profile) {
		this.profile = profile;
	}
	
	
	public void assembleFile(List<String> chunkIdStrs, File tempFile) throws InconsistentFileSystemException {
		if(profile!=null && downloader==null) this.downloader = profile.getDownloader();
		
		try {
			FileOutputStream fos = new FileOutputStream(tempFile, false);
			if (logger.isLoggable(Level.INFO)) {
				logger.info("- Decrypting chunks to temp file  "+ tempFile.getAbsolutePath() + " ...");
			}
			
			for (String chunkIdStr : chunkIdStrs) {
				byte[] checksum = CloneChunk.decodeChecksum(chunkIdStr);
				byte[] metaId = CloneChunk.decodeMetaId(chunkIdStr);

				// Look for plain chunk file a la chunk-xyz
				File decryptedChunkFile = Profile.getInstance().getCache().getPlainChunkFile(checksum);

				// Extract metachunk to decrypted chunk files
				if (!decryptedChunkFile.exists()) {
					String metaChunkFilename = CloneChunk.getFileName(metaId, null);

					if (logger.isLoggable(Level.INFO)) {
						logger.log(Level.INFO, "- Chunk does not exist in local cache. Downloading metachunk {0} ...", metaChunkFilename);
					}
					
					// Download file
					File encryptedMetaChunkFile = downloader.downloadMetaChunk(metaChunkFilename, chunkIdStr);
					
					// Decrypt file
					decryptMetaChunk(encryptedMetaChunkFile, metaId, chunkIdStr);
			
				} else {
					if (logger.isLoggable(Level.INFO)) {
						logger.log(Level.INFO, "- Chunk EXISTS in cache {0}.", decryptedChunkFile);
					}
				}

				// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx
				// a chunk can exist in more than one metachunk
				// ---> if multiple clients pack it in a different metachunk
				// (unabhaengig voneinander)

				byte[] chunkContents = FileUtil.readFile(decryptedChunkFile);
				int chunkSize = (int) decryptedChunkFile.length();

				// Write decrypted chunk to file
				fos.write(chunkContents, 0, chunkSize);
			}

			fos.close();
		} catch (Exception e) {
			throw new InconsistentFileSystemException(e);
		} finally {
			downloader.disconnect();
		}
	}
	
	
	private void decryptMetaChunk(File encryptedMetaChunkFile, byte[] metaId, String chunkIdStr) throws EncryptionException, IOException {
		
		// Decipher and extract to individual chunks
		Cipher decCipher = Profile.getInstance().getRepository().getEncryption().createDecCipher(metaId);
		CustomMultiChunk metaChunk = new CustomMultiChunk(metaId, new CipherInputStream(new FileInputStream(encryptedMetaChunkFile), decCipher));

		Chunk chunkx;
		while (null != (chunkx = metaChunk.read())) {
			File decryptedChunkxFile = Profile.getInstance().getCache().getPlainChunkFile(chunkx.getChecksum());

			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO,"    - Extracting chunk {0} to local cache ...", CloneChunk.encodeIdStr(chunkx.getChecksum()));
			}

			FileUtil.writeFile(chunkx.getContents(), decryptedChunkxFile);
		}

		encryptedMetaChunkFile.delete();
	}
	
}
