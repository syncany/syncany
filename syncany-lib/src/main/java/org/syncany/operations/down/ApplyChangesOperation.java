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
package org.syncany.operations.down;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.ChunkEntry.ChunkChecksum;
import org.syncany.database.FileContent;
import org.syncany.database.FileVersion;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.MultiChunkEntry.MultiChunkId;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.Downloader;
import org.syncany.operations.Operation;
import org.syncany.operations.OperationResult;
import org.syncany.operations.down.actions.FileCreatingFileSystemAction;
import org.syncany.operations.down.actions.FileSystemAction;
import org.syncany.plugins.transfer.TransferManager;

/**
 * Applies a given winners database to the local directory.
 * 
 * <p>Steps:
 * <ul>
 *  <li>Determine whether the local branch needs to be updated (new database versions); if so, determine
 *      local {@link FileSystemAction}s</li>
 *  <li>Determine, download and decrypt required multi chunks from remote storage from file actions
 *      (implemented in {@link #determineMultiChunksToDownload(FileVersion, MemoryDatabase) determineMultiChunksToDownload()},
 *      and {@link Downloader#downloadAndDecryptMultiChunks(Set) downloadAndDecryptMultiChunks()})</li>
 *  <li>Apply file system actions locally, creating conflict files where necessary if local file does
 *      not match the expected file (implemented in {@link #applyFileSystemActions(List) applyFileSystemActions()} </li>
 * </ul>
 * 
 * @author Philipp C. Heckel (philipp.heckel@gmail.com)
 */
public class ApplyChangesOperation extends Operation {
	private static final Logger logger = Logger.getLogger(DownOperation.class.getSimpleName());

	private SqlDatabase localDatabase;
	private Downloader downloader;

	private MemoryDatabase winnersDatabase;
	private DownOperationResult result;
	
	private boolean cleanupOccurred;
	private List<PartialFileHistory> preDeleteFileHistoriesWithLastVersion;

	public ApplyChangesOperation(Config config, SqlDatabase localDatabase, TransferManager transferManager, MemoryDatabase winnersDatabase,
			DownOperationResult result, boolean cleanupOccurred, List<PartialFileHistory> preDeleteFileHistoriesWithLastVersion) {
		
		super(config);
		
		this.localDatabase = localDatabase;
		this.downloader = new Downloader(config, transferManager);
		this.winnersDatabase = winnersDatabase;
		this.result = result;
		this.cleanupOccurred = cleanupOccurred;
		this.preDeleteFileHistoriesWithLastVersion = preDeleteFileHistoriesWithLastVersion;
	}

	@Override
	public OperationResult execute() throws Exception {
		logger.log(Level.INFO, "Determine file system actions ...");		
		
		FileSystemActionReconciliator actionReconciliator = new FileSystemActionReconciliator(config, result.getChangeSet());
		List<FileSystemAction> actions;
		
		if (cleanupOccurred) {
			actions = actionReconciliator.determineFileSystemActions(winnersDatabase, true, preDeleteFileHistoriesWithLastVersion);
		}
		else {
			actions = actionReconciliator.determineFileSystemActions(winnersDatabase);
		}

		Set<MultiChunkId> unknownMultiChunks = determineRequiredMultiChunks(actions, winnersDatabase);
		
		downloader.downloadAndDecryptMultiChunks(unknownMultiChunks);
		result.getDownloadedMultiChunks().addAll(unknownMultiChunks);

		applyFileSystemActions(actions);
		
		return null;
	}
	
	/**
	 * Finds the multichunks that need to be downloaded to apply the given file system actions.
	 * The method looks at all {@link FileCreatingFileSystemAction}s and returns their multichunks. 
	 */
	private Set<MultiChunkId> determineRequiredMultiChunks(List<FileSystemAction> actions, MemoryDatabase winnersDatabase) {
		Set<MultiChunkId> multiChunksToDownload = new HashSet<MultiChunkId>();

		for (FileSystemAction action : actions) {
			if (action instanceof FileCreatingFileSystemAction) { // TODO [low] This adds ALL multichunks even though some might be available locally
				multiChunksToDownload.addAll(determineMultiChunksToDownload(action.getFile2(), winnersDatabase));
			}
		}

		return multiChunksToDownload;
	}
	
	/**
	 * Finds the multichunks that need to be downloaded for the given file version -- using the local 
	 * database and given winners database. Returns a set of multichunk identifiers.
	 */
	private Collection<MultiChunkId> determineMultiChunksToDownload(FileVersion fileVersion, MemoryDatabase winnersDatabase) {
		Set<MultiChunkId> multiChunksToDownload = new HashSet<MultiChunkId>();

		// First: Check if we know this file locally!
		List<MultiChunkId> multiChunkIds = localDatabase.getMultiChunkIds(fileVersion.getChecksum());
		
		if (multiChunkIds.size() > 0) {
			multiChunksToDownload.addAll(multiChunkIds);
		}
		else {
			// Second: We don't know it locally; must be from the winners database
			FileContent winningFileContent = winnersDatabase.getContent(fileVersion.getChecksum());			
			boolean winningFileHasContent = winningFileContent != null;

			if (winningFileHasContent) { // File can be empty!
				List<ChunkChecksum> fileChunks = winningFileContent.getChunks(); 
				
				// TODO [medium] Instead of just looking for multichunks to download here, we should look for chunks in local files as well
				// and return the chunk positions in the local files ChunkPosition (chunk123 at file12, offset 200, size 250)
				
				Map<ChunkChecksum, MultiChunkId> checksumsWithMultiChunkIds = localDatabase.getMultiChunkIdsByChecksums(fileChunks);
				
				for (ChunkChecksum chunkChecksum : fileChunks) {
					MultiChunkId multiChunkIdForChunk = checksumsWithMultiChunkIds.get(chunkChecksum);
					if (multiChunkIdForChunk == null) {
						multiChunkIdForChunk = winnersDatabase.getMultiChunkIdForChunk(chunkChecksum);
						
						if (multiChunkIdForChunk == null) {
							throw new RuntimeException("Cannot find multichunk for chunk "+chunkChecksum);	
						}
					}
					
					if (!multiChunksToDownload.contains(multiChunkIdForChunk)) {
						logger.log(Level.INFO, "  + Adding multichunk " + multiChunkIdForChunk + " to download list ...");
						multiChunksToDownload.add(multiChunkIdForChunk);
					}
				}
			}
		}
		
		return multiChunksToDownload;
	}
	
	/**
	 * Applies the given file system actions in a sensible order. To do that, 
	 * the given actions are first sorted using the {@link FileSystemActionComparator} and
	 * then executed individually using {@link FileSystemAction#execute()}.
	 */
	private void applyFileSystemActions(List<FileSystemAction> actions) throws Exception {
		// Sort
		FileSystemActionComparator actionComparator = new FileSystemActionComparator();
		actionComparator.sort(actions);

		logger.log(Level.FINER, "- Applying file system actions (sorted!) ...");

		// Apply
		for (FileSystemAction action : actions) {
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "   +  {0}", action);
			}

			// Execute the file system action
			
			// Note that exceptions are not caught here, to prevent 
			// apply-failed-delete-on-up situations.
			
			action.execute(); 
		}
	}
}
