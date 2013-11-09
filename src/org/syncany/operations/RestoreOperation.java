/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.connection.plugins.MultiChunkRemoteFile;
import org.syncany.connection.plugins.StorageException;
import org.syncany.connection.plugins.TransferManager;
import org.syncany.database.ChunkEntry.ChunkEntryId;
import org.syncany.database.Database;
import org.syncany.database.DatabaseVersion;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MultiChunkEntry;
import org.syncany.database.PartialFileHistory;
import org.syncany.operations.LoadDatabaseOperation.LoadDatabaseOperationResult;
import org.syncany.operations.actions.FileSystemAction;
import org.syncany.operations.actions.NewFileSystemAction;
import org.syncany.util.FileUtil;
import org.syncany.util.StringUtil;

// TODO [medium] Quick and dirty implementation of RestoreOperation, duplicate code with DownOperation

public class RestoreOperation extends Operation {
	private static final Logger logger = Logger.getLogger(RestoreOperation.class.getSimpleName());	
	private RestoreOperationOptions options;
	
	public RestoreOperation() {
		super(null);
		this.options = new RestoreOperationOptions();
	}	

	public RestoreOperation(Config config) {
		this(config, null);
	}	
	
	public RestoreOperation(Config config, RestoreOperationOptions options) {
		super(config);		
		this.options = options;
	}	
		
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Restore' at client "+config.getMachineName()+" ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		Database database = ((LoadDatabaseOperationResult) new LoadDatabaseOperation(config).execute()).getDatabase();		
		DatabaseVersion currentDatabaseVersion = database.getLastDatabaseVersion();
		
		if (currentDatabaseVersion == null) {
			throw new Exception("No database versions yet locally. Nothing to revert.");
		}

		List<String> restoreFilePaths = options.getRestoreFilePaths();
		List<FileVersion> restoreFileVersions = null;
		Set<MultiChunkEntry> multiChunksToDownload = null;
		
		if (options.getStrategy() == RestoreOperationStrategy.DATABASE_DATE) {
			Database databaseBeforeRestoreTime = getDatabaseBeforeRestoreTime(database, options.getDatabaseBeforeDate());
		
			restoreFileVersions = getLastFileVersionsByPath(restoreFilePaths, databaseBeforeRestoreTime);
			multiChunksToDownload = getMultiChunksToDownload(restoreFileVersions, databaseBeforeRestoreTime);
		}
		else if (options.getStrategy() == RestoreOperationStrategy.FILE_VERSION) {
			restoreFileVersions = getPreviousFileVersionsByPath(database, restoreFilePaths, options.getFileVersionNumber());
			multiChunksToDownload = getMultiChunksToDownload(restoreFileVersions, database);
		}
		
		downloadAndDecryptMultiChunks(multiChunksToDownload);
		
		for (FileVersion restoreFileVersion : restoreFileVersions) {
			logger.log(Level.INFO, "- Restore to: "+restoreFileVersion);
			
			FileSystemAction newFileSystemAction = new NewFileSystemAction(config, restoreFileVersion, database, new Database());
			logger.log(Level.INFO, "  --> "+newFileSystemAction);
			
			newFileSystemAction.execute();
		}		
		
		return new RestoreOperationResult();
	}			
	
	private List<FileVersion> getPreviousFileVersionsByPath(Database database, List<String> restoreFilePaths, Integer restoreVersionNumber) {
		List<FileVersion> restoreFileVersions = new ArrayList<FileVersion>();
	
		for (String filePath : restoreFilePaths) {
			PartialFileHistory fileHistory = database.getFileHistory(filePath);
			
			if (fileHistory != null) {
				// Go back <versionNumber> versions
				if (restoreVersionNumber < 0) {
					int fileVersionCount = fileHistory.getFileVersions().size();
					int indexOfRestoreFileVersion = fileVersionCount+restoreVersionNumber;
					
					if (indexOfRestoreFileVersion < 0) {
						logger.log(Level.INFO, "- Cannot find spec. version for file history "+fileHistory.getFileId()+", only has "+fileVersionCount+" versions (going back "+restoreVersionNumber+" not possible), file "+filePath);
					}
					else {						
						ArrayList<FileVersion> fileVersions = new ArrayList<FileVersion>(fileHistory.getFileVersions().values());
						FileVersion restoreFileVersion = fileVersions.get(indexOfRestoreFileVersion);
										
						restoreFileVersions.add(restoreFileVersion);
						logger.log(Level.INFO, "- "+restoreFileVersion);
					}
				}
				
				// Choose exact version <versionNumber> 
				else {
					FileVersion restoreFileVersion = fileHistory.getFileVersion(restoreVersionNumber);
					
					if (restoreFileVersion == null) {
						logger.log(Level.INFO, "- Cannot find spec. version for file history "+fileHistory.getFileId()+", version "+restoreVersionNumber+" does not exist, file "+filePath);
					}
					else {
						restoreFileVersions.add(restoreFileVersion);
						logger.log(Level.INFO, "- "+restoreFileVersion);	
					}					
				}
			}
			else {
				logger.log(Level.INFO, "Cannot find file history for file "+filePath);
			}
		}
		
		return restoreFileVersions;
	}

	private Set<MultiChunkEntry> getMultiChunksToDownload(List<FileVersion> restoreFileVersions, Database databaseBeforeRestoreTime) throws Exception {
		Set<MultiChunkEntry> multiChunksToDownload = new HashSet<MultiChunkEntry>();
		
		for (FileVersion restoreFileVersion : restoreFileVersions) {
			multiChunksToDownload.addAll(determineMultiChunksToDownload(restoreFileVersion, databaseBeforeRestoreTime));
		}
		
		return multiChunksToDownload;
	}

	private List<FileVersion> getLastFileVersionsByPath(List<String> restoreFilePaths, Database databaseBeforeRestoreTime) {
		List<FileVersion> restoreFileVersions = new ArrayList<FileVersion>();
		
		for (String restoreFilePath : restoreFilePaths) {
			PartialFileHistory restoreFileHistory = databaseBeforeRestoreTime.getFileHistory(restoreFilePath);
			
			if (restoreFileHistory != null) {
				restoreFileVersions.add(restoreFileHistory.getLastVersion());
			}
			else {
				logger.log(Level.INFO, "Cannot find file history for file "+restoreFilePath);
			}
		}
		
		return restoreFileVersions;
	}

	private Collection<MultiChunkEntry> determineMultiChunksToDownload(FileVersion fileVersion, Database database) throws Exception {
		// TODO [medium] Duplicate code in DownOperation
		Set<MultiChunkEntry> multiChunksToDownload = new HashSet<MultiChunkEntry>();		
		
		if (fileVersion.getSize() != null && fileVersion.getSize() > 0) {
			FileContent fileContent = database.getContent(fileVersion.getChecksum());
			
			boolean fileHasContent = fileContent != null;
			
			if (fileHasContent) { // File can be empty!					
				Collection<ChunkEntryId> fileChunks = fileContent.getChunks(); // TODO [medium] Instead of just looking for multichunks to download here, we should look for chunks in local files as well and return the chunk positions in the local files ChunkPosition (chunk123 at file12, offset 200, size 250)
				
				for (ChunkEntryId chunkChecksum : fileChunks) {
					MultiChunkEntry multiChunkForChunk = database.getMultiChunkForChunk(chunkChecksum);
					
					if (multiChunkForChunk == null) {
						throw new Exception("Cannot find multichunk "+StringUtil.toHex(chunkChecksum.getArray())); 
					}
					
					if (!multiChunksToDownload.contains(multiChunkForChunk)) {
						logger.log(Level.INFO, "  + Adding multichunk "+StringUtil.toHex(multiChunkForChunk.getId())+" to download list ...");
						multiChunksToDownload.add(multiChunkForChunk);
					}
				}
			}			
		}
		
		return multiChunksToDownload;
	}
	

	private void downloadAndDecryptMultiChunks(Set<MultiChunkEntry> unknownMultiChunks) throws StorageException, IOException {
		// TODO [medium] Duplicate code in DownOperation
		
		logger.log(Level.INFO, "- Downloading and extracting multichunks ...");
		TransferManager transferManager = config.getConnection().createTransferManager();
		
		for (MultiChunkEntry multiChunkEntry : unknownMultiChunks) {
			File localEncryptedMultiChunkFile = config.getCache().getEncryptedMultiChunkFile(multiChunkEntry.getId());
			File localDecryptedMultiChunkFile = config.getCache().getDecryptedMultiChunkFile(multiChunkEntry.getId());
			MultiChunkRemoteFile remoteMultiChunkFile = new MultiChunkRemoteFile(localEncryptedMultiChunkFile.getName()); // TODO [low] Make MultiChunkRemoteFile class, or something like that
			
			logger.log(Level.INFO, "  + Downloading multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" ...");
			transferManager.download(remoteMultiChunkFile, localEncryptedMultiChunkFile);
			
			logger.log(Level.INFO, "  + Decrypting multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" ...");
			InputStream multiChunkInputStream = config.getTransformer().createInputStream(new FileInputStream(localEncryptedMultiChunkFile));			
			OutputStream decryptedMultiChunkOutputStream = new FileOutputStream(localDecryptedMultiChunkFile); 			

			// TODO [medium] Calculate checksum while writing file, to verify correct content
			FileUtil.appendToOutputStream(multiChunkInputStream, decryptedMultiChunkOutputStream);
			
			decryptedMultiChunkOutputStream.close();
			multiChunkInputStream.close();
			
			logger.log(Level.FINE, "  + Locally deleting multichunk "+StringUtil.toHex(multiChunkEntry.getId())+" ...");
			localEncryptedMultiChunkFile.delete();
		}
		
		transferManager.disconnect();
	}

	private Database getDatabaseBeforeRestoreTime(Database database, Date restoreDate) {
		Database databaseBeforeRestoreTime = new Database();
		
		for (DatabaseVersion compareDatabaseVersion : database.getDatabaseVersions()) {
			if (compareDatabaseVersion.getTimestamp().equals(restoreDate) || compareDatabaseVersion.getTimestamp().before(restoreDate)) {
				databaseBeforeRestoreTime.addDatabaseVersion(compareDatabaseVersion);
			}
		}		
		
		return databaseBeforeRestoreTime;
	}

	public static enum RestoreOperationStrategy {
		DATABASE_DATE,
		FILE_VERSION
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
