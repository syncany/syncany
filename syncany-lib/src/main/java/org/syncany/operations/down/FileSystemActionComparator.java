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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.database.FileVersion.FileType;
import org.syncany.operations.down.actions.DeleteFileSystemAction;
import org.syncany.operations.down.actions.FileSystemAction;
import org.syncany.operations.down.actions.NewFileSystemAction;
import org.syncany.operations.down.actions.RenameFileSystemAction;

/**
 * Sorts file system actions according to their natural order to prevent scenarios 
 * in which a non-empty directory is deleted, ...
 * 
 * Sorting necessary:
 * 1. Delete file/symlink actions must happen first
 * 2. New folder actions must happen after delete file/symlink actions, and must be sorted by path, shortest first (within new actions)
 * 3. New file/symlink actions must happen after new folder actions 
 * 4. Rename file/symlink actions must happen after new folder actions
 * 5. Delete folder actions must happen last, sorted by path, longest first (within delete actions)
 *    a. except if the deleted folder has a name clash with another action (rename/new)
 *       in that case, it must happen before the other action
 * 
 * No sorting necessary:
 * - Change file/symlink actions can happen anytime
 * - Set attributes actions can happen anytime
 * 
 * Cannot happen:
 * - Rename folder
 * - Change folder
 * 
 * 
 * TODO [medium] NewSymlinkFileSystemAction --> NewFile (has no content)
 */
public class FileSystemActionComparator  {
	private static final Logger logger = Logger.getLogger(FileSystemActionComparator.class.getSimpleName());
	
	private static final Object[][] TARGET_ORDER =  new Object[][] {
		new Object[] { DeleteFileSystemAction.class, FileType.FILE }, 
		new Object[] { DeleteFileSystemAction.class, FileType.SYMLINK }, 
		new Object[] { NewFileSystemAction.class, FileType.FOLDER },
		new Object[] { NewFileSystemAction.class, FileType.FILE },
		new Object[] { NewFileSystemAction.class, FileType.SYMLINK },
		new Object[] { RenameFileSystemAction.class, FileType.FILE },
		new Object[] { RenameFileSystemAction.class, FileType.SYMLINK },
		new Object[] { DeleteFileSystemAction.class, FileType.FOLDER } 
	};
	
	private InternalFileSystemActionComparator internalComparator = new InternalFileSystemActionComparator();
			
	public void sort(List<FileSystemAction> actions) {
		Collections.sort(actions, internalComparator);
		postCompareSort(actions);
		
		if (logger.isLoggable(Level.INFO)) {
			logger.log(Level.INFO, "   Sorted actions:");
			
			for (FileSystemAction action : actions) {
				logger.log(Level.INFO, "   + "+action);
			}
		}
	}
	
	/**
	 * Fixes the case in which a folder has been swapped with a file (case 5a, see above)
	 * 
	 * New/Renamed file system actions must happen *after* the file was deleted. This method
	 * moves new/renamed actions below deleted folder actions. 
	 * 
	 * DEL  FILE     file1.jpg      
	 * NEW  FOLDER   folder1
	 * NEW  FILE     folder1/fileinfolder1.jpg                       < No Issue, but needs to be checked
	 * NEW  FILE     folder2                                         <<< Issue, because folder2 is deleted below!
	 * NEW  SYMLINK  folder1/symlinkinfolder1.jpg                    < No Issue, but needs to be checked
	 * REN  FILE     folder2/fileinfolder2.jpg -> folder1/x2.jpg
	 * REN  SYMLINK  folder2/symlinkinfolder2.jpg -> folder1/sym2
	 * DEL  FOLDER   folder2
	 *                                                               <<< New position of "NEW FILE folder2"!
	 */
	public void postCompareSort(List<FileSystemAction> actions) {
		List<FileSystemAction> fixedActions = new ArrayList<FileSystemAction>(actions);
		
		int i = 0;
		
		while (i < fixedActions.size()) {
			FileSystemAction currentAction = fixedActions.get(i);
			
			if (currentAction instanceof DeleteFileSystemAction && currentAction.getType() == FileType.FOLDER) {
				break;
			}
			
			//System.out.println("testing "+currentAction);
			if ((currentAction instanceof NewFileSystemAction || currentAction instanceof RenameFileSystemAction)
					&& (currentAction.getType() == FileType.FILE || currentAction.getType() == FileType.SYMLINK)) {
					
				int conflictingDeleteActionIndex = getPathConflictingWithDeleteActionIndex(currentAction, fixedActions);
				
				if (conflictingDeleteActionIndex >= 0) {
					logger.log(Level.INFO, "     --> match, conflict ["+i+"]: "+currentAction);
					logger.log(Level.INFO, "                    with ["+conflictingDeleteActionIndex+"]: "+fixedActions.get(conflictingDeleteActionIndex));
					
					fixedActions.remove(i);
					fixedActions.add(conflictingDeleteActionIndex, currentAction);
					
					i--; // fix counter!
				}
			}
			
			i++;
		}		
		
		// Replace
		actions.clear();
		actions.addAll(fixedActions);
	}
			
	private int getPathConflictingWithDeleteActionIndex(FileSystemAction currentAction, List<FileSystemAction> deleteFolderActions) {
		for (int i=0; i<deleteFolderActions.size(); i++) {
			FileSystemAction action = deleteFolderActions.get(i);

			if (action instanceof DeleteFileSystemAction && action.getType() == FileType.FOLDER) {			
				if (action.getFile2().getPath().equals(currentAction.getFile2().getPath())) {
					return i;
				}
			}
		}
		
		return -1;
	}

	private class InternalFileSystemActionComparator implements Comparator<FileSystemAction> {
		@Override
		public int compare(FileSystemAction a1, FileSystemAction a2) {
			int a1Position = determinePosition(a1);
			int a2Position = determinePosition(a2);
			
			if (a1Position > a2Position) {
				return 1;
			}
			else if (a1Position < a2Position) {
				return -1;
			}
			
			return compareByFullName(a1, a2);
		}
				
		private int compareByFullName(FileSystemAction a1, FileSystemAction a2) {
			// For deleted, do the longest path first
			if (a1.getClass().equals(DeleteFileSystemAction.class)) {
				return -1 * a1.getFile2().getPath().compareTo(a2.getFile2().getPath());
			}
			
			// For new folder actions, do the shortest path first
			else if (a1.getClass().equals(NewFileSystemAction.class)) {
				return a1.getFile2().getPath().compareTo(a2.getFile2().getPath());
			}
			
			return 0;
		}

		private int determinePosition(FileSystemAction a) {
			for (int i=0; i<TARGET_ORDER.length; i++) {
				Class<?> targetClass = (Class<?>) TARGET_ORDER[i][0];
				FileType targetFileType = (FileType) TARGET_ORDER[i][1];
				
				if (a.getClass().equals(targetClass) && a.getType() == targetFileType) {					
					return i;
				}
			}
			
			return -1;
		}
	}
	
}
