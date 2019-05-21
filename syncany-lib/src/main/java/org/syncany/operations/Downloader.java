/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2016 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import org.syncany.config.LocalEventBus;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.operations.daemon.messages.DownDownloadFileSyncExternalEvent;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;

/**
 * The downloader uses a {@link TransferManager} to download a given set of multichunks,
 * decrypt them and store them in the local cache folder. 
 * 
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class Downloader {
	private static final Logger logger = Logger.getLogger(Downloader.class.getSimpleName());

	private Config config;
	private TransferManager transferManager;
	private LocalEventBus eventBus;

	public Downloader(Config config, TransferManager transferManager) {
		this.config = config;
		this.transferManager = transferManager;
		this.eventBus = LocalEventBus.getInstance();
	}

	/** 
	 * Downloads the given multichunks from the remote storage and decrypts them
	 * to the local cache folder. 
	 */
	public void downloadAndDecryptMultiChunks(Set<MultiChunkId> unknownMultiChunkIds) throws StorageException, IOException {
		logger.log(Level.INFO, "Downloading and extracting multichunks ...");

		int multiChunkNumber = 0;

		for (MultiChunkId multiChunkId : unknownMultiChunkIds) {
			File localEncryptedMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkId);
			File localDecryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkId);
			MultichunkRemoteFile remoteMultiChunkFile = new MultichunkRemoteFile(multiChunkId);

			multiChunkNumber++;

			if (localDecryptedMultiChunkFile.exists()) {
				logger.log(Level.INFO, "  + Decrypted multichunk exists locally " + multiChunkId + ". No need to download it!");
			}
			else {
				eventBus.post(new DownDownloadFileSyncExternalEvent(config.getLocalDir().getAbsolutePath(), "multichunk", multiChunkNumber,
						unknownMultiChunkIds.size()));

				logger.log(Level.INFO, "  + Downloading multichunk " + multiChunkId + " ...");
				transferManager.download(remoteMultiChunkFile, localEncryptedMultiChunkFile);

				try {
					logger.log(Level.INFO, "  + Decrypting multichunk " + multiChunkId + " ...");
					InputStream multiChunkInputStream = config.getTransformer().createInputStream(new FileInputStream(localEncryptedMultiChunkFile));
					OutputStream decryptedMultiChunkOutputStream = new FileOutputStream(localDecryptedMultiChunkFile);

					IOUtils.copy(multiChunkInputStream, decryptedMultiChunkOutputStream);

					decryptedMultiChunkOutputStream.close();
					multiChunkInputStream.close();

				}
				catch (IOException e) {
					// Security: Deleting the multichunk if the decryption/extraction failed is important!
					//           If it is not deleted, the partially decrypted multichunk will reside in the
					//           local cache and the next 'down' will try to use it. If this is the only
					//           multichunk that has been tampered with, other changes might be applied to the 
					//           file system! See https://github.com/syncany/syncany/issues/59#issuecomment-55154793

					logger.log(Level.FINE, "    -> FAILED: Decryption/extraction of multichunk failed, deleting " + multiChunkId + " ...");
					localDecryptedMultiChunkFile.delete();

					throw new IOException("Decryption/extraction of multichunk " + multiChunkId
							+ " failed. The multichunk might have been tampered with!", e);
				}
				finally {
					logger.log(Level.FINE, "  + Locally deleting multichunk " + multiChunkId + " ...");
					localEncryptedMultiChunkFile.delete();
				}
			}
		}

		transferManager.disconnect();
	}
}
