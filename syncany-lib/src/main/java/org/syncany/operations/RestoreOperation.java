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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.down.actions.FileSystemAction;
import org.syncany.operations.down.actions.NewFileSystemAction;
import org.syncany.util.FileUtil;

/**
 * TODO [medium] Quick and dirty implementation of RestoreOperation, duplicate code with DownOperation
 * 
 */
public class RestoreOperation extends Operation {
	private static final Logger logger = Logger.getLogger(RestoreOperation.class.getSimpleName());
	private RestoreOperationOptions options;
	
	private SqlDatabase localDatabase;

	public RestoreOperation(Config config) {
		this(config, new RestoreOperationOptions());
	}

	public RestoreOperation(Config config, RestoreOperationOptions options) {
		super(config);
		
		this.options = options;
		this.localDatabase = new SqlDatabase(config);
	}

	@Override
	public RestoreOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Restore' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");

		List<String> restoreFilePaths = options.getRestoreFilePaths();
		List<FileVersion> restoreFileVersions = new ArrayList<FileVersion>();
		Set<MultiChunkId> multiChunksToDownload = new HashSet<MultiChunkId>();

		if (options.getStrategy() == RestoreOperationStrategy.DATABASE_DATE) {
			restoreFileVersions = getFileTreeAtDate(options.getDatabaseBeforeDate(), restoreFilePaths);
			
			for (FileVersion restoreFileVersion : restoreFileVersions) {
				FileChecksum restoreFileChecksum = restoreFileVersion.getChecksum();
				
				if (restoreFileChecksum != null) {
					multiChunksToDownload.addAll(localDatabase.getMultiChunkIds(restoreFileChecksum));
				}
			}
		}
		else {
			throw new Exception("Strategy "+options.getStrategy()+" not supported yet.");
		}

		downloadAndDecryptMultiChunks(multiChunksToDownload);

		for (FileVersion restoreFileVersion : restoreFileVersions) {
			logger.log(Level.INFO, "- Restore to: " + restoreFileVersion);

			FileSystemAction newFileSystemAction = new NewFileSystemAction(config, restoreFileVersion, new MemoryDatabase());
			logger.log(Level.INFO, "  --> " + newFileSystemAction);

			newFileSystemAction.execute();
		}

		return new RestoreOperationResult();
	}

	private List<FileVersion> getFileTreeAtDate(Date databaseBeforeDate, List<String> restoreFilePaths) {
		Map<String, FileVersion> entireFileTreeAtDate = localDatabase.getFileTreeAtDate(databaseBeforeDate);
		List<FileVersion> restoreFileVersions = new ArrayList<FileVersion>();
		
		for (FileVersion restoreFileVersion : entireFileTreeAtDate.values()) {			
			if (restoreFilePaths.size() > 0) {
				if (restoreFilePaths.contains(restoreFileVersion.getPath())) {
					restoreFileVersions.add(restoreFileVersion);	
				}				
			}
			else {
				restoreFileVersions.add(restoreFileVersion);
			}			
		}
		
		return restoreFileVersions;
	}

	private void downloadAndDecryptMultiChunks(Set<MultiChunkId> unknownMultiChunkIds) throws StorageException, IOException {
		// TODO [medium] Duplicate code in DownOperation

		logger.log(Level.INFO, "- Downloading and extracting multichunks ...");
		TransferManager transferManager = config.getConnection().createTransferManager();

		for (MultiChunkId multiChunkId : unknownMultiChunkIds) {
			File localEncryptedMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkId);
			File localDecryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkId);
			MultiChunkRemoteFile remoteMultiChunkFile = new MultiChunkRemoteFile(multiChunkId);

			logger.log(Level.INFO, "  + Downloading multichunk " + multiChunkId + " ...");
			transferManager.download(remoteMultiChunkFile, localEncryptedMultiChunkFile);

			logger.log(Level.INFO, "  + Decrypting multichunk " + multiChunkId + " ...");
			InputStream multiChunkInputStream = config.getTransformer().createInputStream(new FileInputStream(localEncryptedMultiChunkFile));
			OutputStream decryptedMultiChunkOutputStream = new FileOutputStream(localDecryptedMultiChunkFile);

			// TODO [medium] Calculate checksum while writing file, to verify correct content
			FileUtil.appendToOutputStream(multiChunkInputStream, decryptedMultiChunkOutputStream);

			decryptedMultiChunkOutputStream.close();
			multiChunkInputStream.close();

			logger.log(Level.FINE, "  + Locally deleting multichunk " + multiChunkId + " ...");
			localEncryptedMultiChunkFile.delete();
		}

		transferManager.disconnect();
	}

	public static enum RestoreOperationStrategy {
		DATABASE_DATE, FILE_VERSION
	}

	public static class RestoreOperationOptions implements OperationOptions {
		private RestoreOperationStrategy strategy;
		private Date databaseBeforeDate;
		private Integer fileVersionNumber;
		private List<String> restoreFilePaths;

		public Date getDatabaseBeforeDate() {
			return databaseBeforeDate;
		}

		public void setDatabaseBeforeDate(Date databaseBeforeDate) {
			this.databaseBeforeDate = databaseBeforeDate;
		}

		public List<String> getRestoreFilePaths() {
			return restoreFilePaths;
		}

		public void setRestoreFilePaths(List<String> restoreFiles) {
			this.restoreFilePaths = restoreFiles;
		}

		public RestoreOperationStrategy getStrategy() {
			return strategy;
		}

		public void setStrategy(RestoreOperationStrategy strategy) {
			this.strategy = strategy;
		}

		public Integer getFileVersionNumber() {
			return fileVersionNumber;
		}

		public void setFileVersionNumber(Integer fileVersionNumber) {
			this.fileVersionNumber = fileVersionNumber;
		}
	}

	public class RestoreOperationResult implements OperationResult {
		// Fressen
	}
}
