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
package org.syncany.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.syncany.config.Config;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.files.MultiChunkRemoteFile;

/**
 * @author pheckel
 *
 */
public class Downloader {
	private static final Logger logger = Logger.getLogger(Downloader.class.getSimpleName());

	private Config config;
	private TransferManager transferManager;
	
	public Downloader(Config config, TransferManager transferManager) {
		this.config = config;
		this.transferManager = transferManager;
	}
	
	/** 
	 * Downloads the given multichunks from the remote storage and decrypts them
	 * to the local cache folder. 
	 */
	public void downloadAndDecryptMultiChunks(Set<MultiChunkId> unknownMultiChunkIds) throws StorageException, IOException {
		logger.log(Level.INFO, "Downloading and extracting multichunks ...");
		
		// TODO [medium] Check existing files by checksum and do NOT download them if they exist locally, or copy them

		for (MultiChunkId multiChunkId : unknownMultiChunkIds) {
			File localEncryptedMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkId);
			File localDecryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkId);
			MultiChunkRemoteFile remoteMultiChunkFile = new MultiChunkRemoteFile(multiChunkId);

			logger.log(Level.INFO, "  + Downloading multichunk " + multiChunkId + " ...");
			transferManager.download(remoteMultiChunkFile, localEncryptedMultiChunkFile);

			logger.log(Level.INFO, "  + Decrypting multichunk " + multiChunkId + " ...");
			InputStream multiChunkInputStream = config.getTransformer().createInputStream(new FileInputStream(localEncryptedMultiChunkFile));
			OutputStream decryptedMultiChunkOutputStream = new FileOutputStream(localDecryptedMultiChunkFile);

			IOUtils.copy(multiChunkInputStream, decryptedMultiChunkOutputStream);
			
			decryptedMultiChunkOutputStream.close();
			multiChunkInputStream.close();

			logger.log(Level.FINE, "  + Locally deleting multichunk " + multiChunkId + " ...");
			localEncryptedMultiChunkFile.delete();
		}

		transferManager.disconnect();
	}
}
