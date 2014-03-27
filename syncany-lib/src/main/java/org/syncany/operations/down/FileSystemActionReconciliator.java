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
package org.syncany.operations.down;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.config.Config;
import org.syncany.database.FileVersion;
import org.syncany.database.FileVersionComparator;
import org.syncany.database.FileVersionComparator.FileChange;
import org.syncany.database.FileVersionComparator.FileVersionComparison;
import org.syncany.database.MemoryDatabase;
import org.syncany.database.PartialFileHistory;
import org.syncany.database.PartialFileHistory.FileHistoryId;
import org.syncany.database.SqlDatabase;
import org.syncany.operations.ChangeSet;
import org.syncany.operations.down.actions.ChangeFileSystemAction;
import org.syncany.operations.down.actions.DeleteFileSystemAction;
import org.syncany.operations.down.actions.FileSystemAction;
import org.syncany.operations.down.actions.NewFileSystemAction;
import org.syncany.operations.down.actions.NewSymlinkFileSystemAction;
import org.syncany.operations.down.actions.RenameFileSystemAction;
import org.syncany.operations.down.actions.SetAttributesFileSystemAction;


/**
 * Implements the file synchronization algorithm in the down operation.
 * 
 * The algorithm compares the local file on the disk with the last local
 * database file version and the last winning file version and determines
 * what file system action (fsa) to apply.
 *
 * Input variables:
 * - winning version
 * - winning file (= local file of winning version)
 * - local version
 * - local file (= local file of local version)
 * 
 * Algorithm:
 * if (has no local version) { 
 *   compwinfwinv = compare winning file to winning version (incl. checksum!)
 *   
 *   if (compwinfwinv: winning file matches winning version) {
 *     // do nothing
 *   }
 *   else if (compwinfwinv: new) {
 *     add new fsa for winning version
 *     add multichunks to download list for winning version
 *   }
 *   else if (compwinfwinv: deleted) {
 *     add delete fsa for winning version
 *   }
 *   else if (compwinfwinv: changed link) {
 *     add changed link fsa for winning version
 *   } 
 *   else if (compwinfwinv: changes attrs / modified date) { // does not(!) include "path"
 *     add changed attrs fsa for winning version
 *   }
 *   else if (compwinfwinv: changed path) {
 *     // Cannot be!
 *   }
 *   else { // size/checksum (path cannot be!)
 *     add conflict fsa for winning file
 *     add new fsa for winning version
 *     add multichunks to download list for winning version
 *   }
 * }
 * 
 * else { // local version exists
 *   complocflocv = compare local file to local version (incl. checksum!)
 *   
 *   if (complocflocv: local file matches local version) { // file as expected on disk
 *     complocvwinv = compare local version to winning version
 *       
 *     if (complocvwinv: local version matches winning version) { // means: local file = local version = winning version
 *       // Nothing to do
 *     }
 *     else if (complocvwinv: new) {
 *       // Cannot be!
 *     }
 *     else if (complocvwinv: deleted) {
 *       add delete fsa for winning version
 *     }
 *     else if (complocvwinv: changed link) {
 *       add changed link fsa for winning version
 *     } 
 *     else if (complocvwinv: changes attrs / modified date / path) { // includes "path!"
 *       add changed attrs / renamed fsa for winning version
 *     }
 *     else { // size/checksum 
 *       add changed fsa for winning version (and delete local version)
 *       add multichunks to download list for winning version
 *     }
 *   }
 *   else { // local file does NOT match local version
 *     if (local file exists) {
 *       add conflict fsa for local version
 *     }
 *     
 *     add new fsa for winning version
 *     add multichunks to download list for winning version
 * }
 * 
 */
public class FileSystemActionReconciliator {
	private static final Logger logger = Logger.getLogger(FileSystemActionReconciliator.class.getSimpleName());

	private Config config; 
	private ChangeSet changeSet;
	private SqlDatabase localDatabase;
	private FileVersionComparator fileVersionComparator;
	
	public FileSystemActionReconciliator(Config config, DownOperationResult result) {
		this.config = config; 
		this.changeSet = result.getChangeSet();
		this.localDatabase = new SqlDatabase(config);
		this.fileVersionComparator = new FileVersionComparator(config.getLocalDir(), config.getChunker().getChecksumAlgorithm());
	}
	
	public List<FileSystemAction> determineFileSystemActions(MemoryDatabase winnersDatabase) throws Exception {
		List<FileSystemAction> fileSystemActions = new ArrayList<FileSystemAction>();
		
		// Load file history cache
		logger.log(Level.INFO, "- Loading current file tree...");
		
		List<PartialFileHistory> fileHistoriesWithLastVersion = localDatabase.getFileHistoriesWithLastVersion();		
		Map<FileHistoryId, FileVersion> fileHistoryIdCache = fillFileHistoryIdCache(fileHistoriesWithLastVersion);
				
		logger.log(Level.INFO, "- Determine filesystem actions ...");
		
		for (PartialFileHistory winningFileHistory : winnersDatabase.getFileHistories()) {
			// Get remote file version and content
			FileVersion winningLastVersion = winningFileHistory.getLastVersion();			
			File winningLastFile = new File(config.getLocalDir(), winningLastVersion.getPath());
			
			// Get local file version and content
			FileVersion localLastVersion = fileHistoryIdCache.get(winningFileHistory.getFileHistoryId());
			File localLastFile = (localLastVersion != null) ? new File(config.getLocalDir(), localLastVersion.getPath()) : null;
						
			logger.log(Level.INFO, "  + Comparing local version: "+localLastVersion);	
			logger.log(Level.INFO, "    with winning version   : "+winningLastVersion);
			
			// Sync algorithm ////			
			
			// No local file version in local database
			if (localLastVersion == null) { 	
				determineActionNoLocalLastVersion(winningLastVersion, winningLastFile, winnersDatabase, fileSystemActions);
			}
			
			// Local version found in local database
			else {
				FileVersionComparison localFileToVersionComparison = fileVersionComparator.compare(localLastVersion, localLastFile, true);
				
				// Local file on disk as expected
				if (localFileToVersionComparison.equals()) { 
					determineActionWithLocalVersionAndLocalFileAsExpected(winningLastVersion, winningLastFile, localLastVersion, localLastFile,
							winnersDatabase, fileSystemActions);
				}
				
				// Local file NOT what was expected
				else { 
					determineActionWithLocalVersionAndLocalFileDiffers(winningLastVersion, winningLastFile, localLastVersion, localLastFile,
							winnersDatabase, fileSystemActions, localFileToVersionComparison);			
				}
			}		
		}
			
		return fileSystemActions;
	}

	private void determineActionNoLocalLastVersion(FileVersion winningLastVersion, File winningLastFile, MemoryDatabase winnersDatabase,
			List<FileSystemAction> outFileSystemActions) throws Exception {
		
		FileVersionComparison winningFileToVersionComparison = fileVersionComparator.compare(winningLastVersion, winningLastFile, true);
		
		boolean contentChanged = winningFileToVersionComparison.getFileChanges().contains(FileChange.CHANGED_CHECKSUM)
				|| winningFileToVersionComparison.getFileChanges().contains(FileChange.CHANGED_SIZE);
		
		if (winningFileToVersionComparison.equals()) {
			logger.log(Level.INFO, "     -> (1) Equals: Nothing to do, winning version equals winning file: "+winningLastVersion+" AND "+winningLastFile);	
}
		else if (winningFileToVersionComparison.getFileChanges().contains(FileChange.DELETED)) {					
			FileSystemAction action = new NewFileSystemAction(config, winningLastVersion, winnersDatabase);
			outFileSystemActions.add(action);
			
			logger.log(Level.INFO, "     -> (2) Deleted: Local file does NOT exist, but it should, winning version not known: "+winningLastVersion+" AND "+winningLastFile);
			logger.log(Level.INFO, "     -> "+action);
			
			changeSet.getNewFiles().add(winningLastVersion.getPath());
		}
		else if (winningFileToVersionComparison.getFileChanges().contains(FileChange.NEW)) {
			logger.log(Level.INFO, "     -> (3) New: winning version was deleted, but local exists: "+winningLastVersion+" AND "+winningLastFile);					
			throw new Exception("What happend here?");
		}
		else if (winningFileToVersionComparison.getFileChanges().contains(FileChange.CHANGED_LINK_TARGET)) {					
			FileSystemAction action = new NewSymlinkFileSystemAction(config, winningLastVersion, winnersDatabase);
			outFileSystemActions.add(action);

			logger.log(Level.INFO, "     -> (4) Changed link target: winning file has a different link target: "+winningLastVersion+" AND "+winningLastFile);
			logger.log(Level.INFO, "     -> "+action);
			
			changeSet.getNewFiles().add(winningLastVersion.getPath());
		}
		else if (!contentChanged && (winningFileToVersionComparison.getFileChanges().contains(FileChange.CHANGED_LAST_MOD_DATE)
				|| winningFileToVersionComparison.getFileChanges().contains(FileChange.CHANGED_ATTRIBUTES))) {	
			
			FileSystemAction action = new SetAttributesFileSystemAction(config, winningLastVersion, winnersDatabase);
			outFileSystemActions.add(action);

			logger.log(Level.INFO, "     -> (5) Changed file attributes: winning file has different file attributes: "+winningLastVersion+" AND "+winningLastFile);
			logger.log(Level.INFO, "     -> "+action);
			
			changeSet.getNewFiles().add(winningLastVersion.getPath());
		}
		else if (winningFileToVersionComparison.getFileChanges().contains(FileChange.CHANGED_PATH)) {
			logger.log(Level.INFO, "     -> (6) Changed path: winning file has a different path: "+winningLastVersion+" AND "+winningLastFile);					
			throw new Exception("What happend here?");
		}
		else { // Content changed
			FileSystemAction action = new NewFileSystemAction(config, winningLastVersion, winnersDatabase);
			outFileSystemActions.add(action);

			logger.log(Level.INFO, "     -> (7) Content changed: Winning file differs from winning version: "+winningLastVersion+" AND "+winningLastFile);
			logger.log(Level.INFO, "     -> "+action);
			
			changeSet.getNewFiles().add(winningLastVersion.getPath());
		}							
	}
	
	private void determineActionWithLocalVersionAndLocalFileAsExpected(FileVersion winningLastVersion, File winningLastFile,
			FileVersion localLastVersion, File localLastFile, MemoryDatabase winnersDatabase, List<FileSystemAction> fileSystemActions) {
		
		FileVersionComparison winningVersionToLocalVersionComparison = fileVersionComparator.compare(winningLastVersion, localLastVersion);
		
		boolean contentChanged = winningVersionToLocalVersionComparison.getFileChanges().contains(FileChange.CHANGED_CHECKSUM)
				|| winningVersionToLocalVersionComparison.getFileChanges().contains(FileChange.CHANGED_SIZE);					
		
		if (winningVersionToLocalVersionComparison.equals()) { // Local file = local version = winning version!
			logger.log(Level.INFO, "     -> (8) Equals: Nothing to do, local file equals local version equals winning version: local file = "+localLastFile+", local version = "+localLastVersion+", winning version = "+winningLastVersion);
		}
		else if (winningVersionToLocalVersionComparison.getFileChanges().contains(FileChange.DELETED)) {
			FileSystemAction action = new ChangeFileSystemAction(config, localLastVersion, winningLastVersion, winnersDatabase);
			fileSystemActions.add(action);

			logger.log(Level.INFO, "     -> (9) Content changed: Local file does not exist, but it should: local file = "+localLastFile+", local version = "+localLastVersion+", winning version = "+winningLastVersion);
			logger.log(Level.INFO, "     -> "+action);
			
			changeSet.getChangedFiles().add(winningLastVersion.getPath());
		}
		else if (winningVersionToLocalVersionComparison.getFileChanges().contains(FileChange.NEW)) {
			FileSystemAction action = new DeleteFileSystemAction(config, localLastVersion, winningLastVersion, winnersDatabase);
			fileSystemActions.add(action);
			
			logger.log(Level.INFO, "     -> (10) Local file is exists, but should not: local file = "+localLastFile+", local version = "+localLastVersion+", winning version = "+winningLastVersion);					
			logger.log(Level.INFO, "     -> "+action);	
			
			changeSet.getDeletedFiles().add(winningLastVersion.getPath());
		}
		else if (winningVersionToLocalVersionComparison.getFileChanges().contains(FileChange.CHANGED_LINK_TARGET)) {					
			FileSystemAction action = new NewSymlinkFileSystemAction(config, winningLastVersion, winnersDatabase);
			fileSystemActions.add(action);

			logger.log(Level.INFO, "     -> (11) Changed link target: local file has a different link target: local file = "+localLastFile+", local version = "+localLastVersion+", winning version = "+winningLastVersion);
			logger.log(Level.INFO, "     -> "+action);
			
			changeSet.getNewFiles().add(winningLastVersion.getPath());
		}
		else if (!contentChanged && (winningVersionToLocalVersionComparison.getFileChanges().contains(FileChange.CHANGED_LAST_MOD_DATE)
				|| winningVersionToLocalVersionComparison.getFileChanges().contains(FileChange.CHANGED_ATTRIBUTES)
				|| winningVersionToLocalVersionComparison.getFileChanges().contains(FileChange.CHANGED_PATH))) {	
			
			FileSystemAction action = new RenameFileSystemAction(config, localLastVersion, winningLastVersion, winnersDatabase);
			fileSystemActions.add(action);

			logger.log(Level.INFO, "     -> (12) Rename / Changed file attributes: Local file has different file attributes: local file = "+localLastFile+", local version = "+localLastVersion+", winning version = "+winningLastVersion);
			logger.log(Level.INFO, "     -> "+action);
			
			changeSet.getChangedFiles().add(winningLastVersion.getPath());
		}
		else { // Content changed
			FileSystemAction action = new ChangeFileSystemAction(config, localLastVersion, winningLastVersion, winnersDatabase);
			fileSystemActions.add(action);

			logger.log(Level.INFO, "     -> (13) Content changed: Local file differs from winning version: local file = "+localLastFile+", local version = "+localLastVersion+", winning version = "+winningLastVersion);
			logger.log(Level.INFO, "     -> "+action);	
			
			changeSet.getChangedFiles().add(winningLastVersion.getPath());
		}
	}

	private void determineActionWithLocalVersionAndLocalFileDiffers(FileVersion winningLastVersion, File winningLastFile,
			FileVersion localLastVersion, File localLastFile, MemoryDatabase winnersDatabase, List<FileSystemAction> fileSystemActions,
			FileVersionComparison localFileToVersionComparison) {

		if (localFileToVersionComparison.getFileChanges().contains(FileChange.DELETED)) {	
			logger.log(Level.INFO, "     -> (14) File deleted: Local file does not exist and SHOULD NOT: Nothing to do!");
		}
		else {
			FileSystemAction action = new ChangeFileSystemAction(config, localLastVersion, winningLastVersion, winnersDatabase);
			fileSystemActions.add(action);
	
			logger.log(Level.INFO, "     -> (15) Content changed: Local file differs from last version: local file = "+localLastFile+", local version = "+localLastVersion+", winning version = "+winningLastVersion);
			logger.log(Level.INFO, "     -> "+action);	
			
			changeSet.getChangedFiles().add(winningLastVersion.getPath());
		}
	}

	private Map<FileHistoryId, FileVersion> fillFileHistoryIdCache(List<PartialFileHistory> fileHistoriesWithLastVersion) {
		Map<FileHistoryId, FileVersion> fileHistoryIdCache = new HashMap<FileHistoryId, FileVersion>();
		
		for (PartialFileHistory fileHistory : fileHistoriesWithLastVersion) {
			fileHistoryIdCache.put(fileHistory.getFileHistoryId(), fileHistory.getLastVersion());
		}
		
		return fileHistoryIdCache;
	}	
}
