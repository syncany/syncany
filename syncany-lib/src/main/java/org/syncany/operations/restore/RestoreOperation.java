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
package org.syncany.operations.restore;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.FileContent.FileChecksum;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersion.FileType;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.AbstractTransferOperation;
import org.syncany.operations.Downloader;
import org.syncany.operations.restore.RestoreOperationResult.RestoreResultCode;
import org.syncany.plugins.transfer.StorageException;

public class RestoreOperation extends AbstractTransferOperation {
	private static final Logger logger = Logger.getLogger(RestoreOperation.class.getSimpleName());
	public static final String ACTION_ID = "restore";
	
	private RestoreOperationOptions options;
	
	private SqlDatabase localDatabase;
	private Downloader downloader;

	public RestoreOperation(Config config) {
		this(config, new RestoreOperationOptions());
	}

	public RestoreOperation(Config config, RestoreOperationOptions options) {
		super(config, ACTION_ID);
		
		this.options = options;
		this.localDatabase = new SqlDatabase(config);
		this.downloader = new Downloader(config, transferManager);
	}

	@Override
	public RestoreOperationResult execute() throws Exception {
		logger.log(Level.INFO, "");
		logger.log(Level.INFO, "Running 'Restore' at client " + config.getMachineName() + " ...");
		logger.log(Level.INFO, "--------------------------------------------");
		
		// Find file version
		FileHistoryId restoreFileHistoryId = findFileHistoryId();
		FileVersion restoreFileVersion = findRestoreFileVersion(restoreFileHistoryId);

		if (restoreFileHistoryId == null || restoreFileVersion == null) {
			return new RestoreOperationResult(RestoreResultCode.NACK_NO_FILE);
		}
		else if (restoreFileVersion.getType() == FileType.FOLDER) {
			return new RestoreOperationResult(RestoreResultCode.NACK_INVALID_FILE);
		}

		logger.log(Level.INFO, "Restore file identified: " + restoreFileVersion);
		
		// Download multichunks
		downloadMultiChunks(restoreFileVersion);
		
		// Restore file
		logger.log(Level.INFO, "- Restoring: " + restoreFileVersion);

		RestoreFileSystemAction restoreAction = new RestoreFileSystemAction(config, restoreFileVersion, options.getRelativeTargetPath());
		RestoreFileSystemActionResult restoreResult = restoreAction.execute();

		return new RestoreOperationResult(RestoreResultCode.ACK, restoreResult.getTargetFile());
	}

	private FileHistoryId findFileHistoryId() {
		return localDatabase.expandFileHistoryId(options.getFileHistoryId()); 
	}

	private FileVersion findRestoreFileVersion(FileHistoryId restoreFileHistoryId) {
		if (options.getFileVersion() != null) {
			return localDatabase.getFileVersion(restoreFileHistoryId, options.getFileVersion());
		}
		else {
			List<FileVersion> fileHistory = localDatabase.getFileHistory(restoreFileHistoryId);
			
			if (fileHistory.size() >= 2) { 
				return fileHistory.get(fileHistory.size()-2);
			}
			else {
				return null;
			}
		}
	}

	private void downloadMultiChunks(FileVersion restoreFileVersion) throws StorageException, IOException {
		Set<MultiChunkId> multiChunksToDownload = new HashSet<MultiChunkId>();
		FileChecksum restoreFileChecksum = restoreFileVersion.getChecksum();
			
		if (restoreFileChecksum != null) {
			multiChunksToDownload.addAll(localDatabase.getMultiChunkIds(restoreFileChecksum));

			logger.log(Level.INFO, "Downloading " + multiChunksToDownload.size() + " multichunk(s) to restore file ...");
			downloader.downloadAndDecryptMultiChunks(multiChunksToDownload);
		}
	}
}
